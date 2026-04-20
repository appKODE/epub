package ru.kode.epub.lib

import android.content.Context
import android.net.Uri
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.jsoup.parser.Parser
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import ru.kode.epub.lib.entity.Book
import ru.kode.epub.lib.entity.Chapter
import ru.kode.epub.lib.entity.CoverImage
import ru.kode.epub.lib.entity.FontFace
import ru.kode.epub.lib.entity.Length
import ru.kode.epub.lib.entity.ListItem
import ru.kode.epub.lib.entity.Metadata
import ru.kode.epub.lib.entity.Span
import ru.kode.epub.lib.entity.Style
import ru.kode.epub.lib.entity.TableCell
import ru.kode.epub.lib.entity.TableRow
import ru.kode.epub.lib.entity.TocItem
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.io.StringReader
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.collections.iterator
import kotlin.text.equals

/**
 * Parses an EPUB file (provided as a [android.net.Uri]) into a [Book].
 *
 * Images and font files are returned as raw [ByteArray] — no disk caching is performed.
 * The caller is responsible for loading/caching them as needed.
 *
 * TOC detection order: EPUB2 NCX preferred, EPUB3 nav as fallback.
 */
object EpubParser {

  fun parseMetadata(context: Context, uri: Uri): Metadata {
    val stream = context.contentResolver.openInputStream(uri)
      ?: error("Cannot open EPUB stream for $uri")
    val fileMap = stream.use { readZipEntries(it) }

    val containerXml = fileMap["META-INF/container.xml"]
      ?.toString(Charsets.UTF_8)
      ?: error("META-INF/container.xml not found")
    val opfPath = parseContainerXml(containerXml)
    val opfDir = opfPath.substringBeforeLast("/", "")

    val opfContent = fileMap[opfPath]
      ?.toString(Charsets.UTF_8)
      ?: error("OPF not found at: $opfPath")
    val opfData = parseOpf(opfContent)

    val coverImage = opfData.coverManifestId
      ?.let { opfData.manifest[it] }
      ?.let { item ->
        val bytes = fileMap[joinPaths(opfDir, item.href)] ?: return@let null
        CoverImage(bytes, item.mediaType.ifBlank { guessMimeType(item.href) })
      }

    return Metadata(
      title = opfData.title.ifBlank { "Unknown" },
      author = opfData.author.ifBlank { "Unknown" },
      language = opfData.language.lowercase(),
      coverImage = coverImage,
      dateYear = opfData.dateYear,
      description = opfData.description,
      categories = opfData.categories
    )
  }

  fun deleteFontCache(context: Context, uri: Uri) {
    File(context.cacheDir, "epub-fonts/${uri.toString().hashCode()}").deleteRecursively()
  }

  fun parse(context: Context, uri: Uri): Book {
    val stream = context.contentResolver.openInputStream(uri)
      ?: error("Cannot open EPUB stream for $uri")
    val fileMap = stream.use { readZipEntries(it) }

    val containerXml = fileMap["META-INF/container.xml"]
      ?.toString(Charsets.UTF_8)
      ?: error("META-INF/container.xml not found")
    val opfPath = parseContainerXml(containerXml)
    val opfDir = opfPath.substringBeforeLast("/", "")

    val opfContent = fileMap[opfPath]
      ?.toString(Charsets.UTF_8)
      ?: error("OPF not found at: $opfPath")
    val opfData = parseOpf(opfContent)

    // ── CSS ────────────────────────────────────────────────────────────────
    val cssRules = mutableListOf<CssRule>()
    val fontFaceRules = mutableListOf<FontFaceRule>()
    opfData.manifest.values.forEach { item ->
      val isCss = item.mediaType.contains("css", ignoreCase = true) ||
        item.href.endsWith(".css", ignoreCase = true)
      if (!isCss) return@forEach
      val cssPath = joinPaths(opfDir, item.href)
      val cssText = fileMap[cssPath]?.toString(Charsets.UTF_8) ?: return@forEach
      cssRules.addAll(
        parseCssRules(cssText).map { rule ->
          // Resolve background-image URLs relative to the CSS file
          val bgImage = rule.declarations["background-image"] ?: return@map rule
          val relUrl = extractUrlPath(bgImage) ?: return@map rule
          rule.copy(declarations = rule.declarations + ("background-image" to "url('${resolvePath(cssPath, relUrl)}')"))
        }
      )
      fontFaceRules.addAll(parseFontFaces(cssText, cssPath))
    }

    // ── Font faces (write to cache, return file references) ───────────────
    val fontCacheDir = File(context.cacheDir, "epub-fonts/${uri.toString().hashCode()}")
      .also { it.mkdirs() }
    val fontFaces = fontFaceRules.mapNotNull { rule ->
      val bytes = fileMap[rule.srcPath]
        ?: fileMap.entries.firstOrNull { it.key.endsWith(rule.srcPath) }?.value
        ?: return@mapNotNull null
      val ext = rule.mimeType.substringAfterLast('/').let { if (it == "octet-stream") "ttf" else it }
      val file = File(fontCacheDir, "font_${rule.family.replace(' ', '_')}_${rule.weight}_${rule.italic}.$ext")
      if (!file.exists()) {
        runCatching { file.writeBytes(bytes) }.getOrNull() ?: return@mapNotNull null
      }
      FontFace(family = rule.family, weight = rule.weight, italic = rule.italic, file = file)
    }

    // ── Image helper (raw bytes) ───────────────────────────────────────────
    val loadImage: (String) -> ByteArray? = { zipPath ->
      fileMap[zipPath] ?: fileMap.entries.firstOrNull { it.key.endsWith(zipPath) }?.value
    }

    // ── Cover image ────────────────────────────────────────────────────────
    val coverImage = opfData.coverManifestId
      ?.let { opfData.manifest[it] }
      ?.let { item ->
        val bytes = fileMap[joinPaths(opfDir, item.href)] ?: return@let null
        CoverImage(bytes, item.mediaType.ifBlank { guessMimeType(item.href) })
      }

    // ── TOC ───────────────────────────────────────────────────────────────
    data class TocParseResult(val flat: List<NavPoint>, val hierarchy: List<NavPointNode>)

    val tocResult: TocParseResult = opfData.tocManifestId
      ?.let { opfData.manifest[it] }
      ?.let { item ->
        val tocPath = joinPaths(opfDir, item.href)
        val tocContent = fileMap[tocPath]?.toString(Charsets.UTF_8) ?: return@let null
        if (item.mediaType == "application/x-dtbncx+xml" || item.href.endsWith(".ncx", ignoreCase = true)) {
          TocParseResult(parseNcx(tocContent, tocPath), parseNcxHierarchy(tocContent, tocPath))
        } else {
          TocParseResult(parseNav(tocContent, tocPath), parseNavHierarchy(tocContent, tocPath))
        }
      } ?: TocParseResult(emptyList(), emptyList())

    val navPoints = tocResult.flat
    val navByHref = navPoints.groupBy { it.hrefPath }

    // ── Chapters ──────────────────────────────────────────────────────────
    // Build a map (hrefPath, fragment?) -> actual index in book.chapters so that
    // TocItem.chapterIndex correctly references the spine position even when some
    // spine items have no TOC entries.
    val navToChapterIdx = mutableMapOf<Pair<String, String?>, Int>()
    var chapterBaseIdx = 0
    val chapters = opfData.spine.flatMap { spineItem ->
      val item = opfData.manifest[spineItem.idref] ?: return@flatMap emptyList()
      val chapterPath = joinPaths(opfDir, item.href)
      val html = fileMap[chapterPath]?.toString(Charsets.UTF_8) ?: return@flatMap emptyList()
      val points = navByHref[chapterPath]
      val result: List<Chapter>
      if (!points.isNullOrEmpty()) {
        val mapped = parseChaptersFromHtmlWithNav(html, chapterPath, cssRules, loadImage, points)
        mapped.forEachIndexed { idx, (_, fragment) ->
          navToChapterIdx[chapterPath to fragment] = chapterBaseIdx + idx
        }
        result = mapped.map { it.first }
      } else {
        result = listOfNotNull(parseChapter(html, chapterPath, cssRules, loadImage))
      }
      chapterBaseIdx += result.size
      result
    }

    // ── TOC hierarchy ─────────────────────────────────────────────────────
    val toc = buildTocItems(tocResult.hierarchy, navToChapterIdx, maxIndex = chapters.size)

    return Book(
      metadata = Metadata(
        title = opfData.title.ifBlank { "Unknown" },
        author = opfData.author.ifBlank { "Unknown" },
        language = opfData.language.lowercase(),
        coverImage = coverImage,
        dateYear = opfData.dateYear,
        description = opfData.description,
        categories = opfData.categories
      ),
      chapters = chapters,
      toc = toc,
      fontFaces = fontFaces
    )
  }

  // ─────────────────────────── ZIP ──────────────────────────────────────────

  private fun readZipEntries(stream: InputStream): Map<String, ByteArray> {
    val map = mutableMapOf<String, ByteArray>()
    ZipInputStream(BufferedInputStream(stream)).use { zip ->
      var entry = zip.nextEntry
      while (entry != null) {
        if (!entry.isDirectory) map[entry.name] = zip.readBytes()
        zip.closeEntry()
        entry = zip.nextEntry
      }
    }
    return map
  }

  // ─────────────────────── container.xml ────────────────────────────────────

  private fun parseContainerXml(xml: String): String {
    val doc = parseXml(xml)
    val rootfiles = doc.getElementsByTagName("rootfile")
    for (i in 0 until rootfiles.length) {
      val path = rootfiles.item(i).attributes?.getNamedItem("full-path")?.nodeValue
      if (!path.isNullOrBlank()) return path
    }
    error("rootfile/@full-path not found in container.xml")
  }

  // ───────────────────────────── OPF ────────────────────────────────────────

  private data class ManifestItem(val href: String, val mediaType: String, val properties: String = "")
  private data class SpineItem(val idref: String, val linear: Boolean)
  private data class NavPoint(val title: String, val hrefPath: String, val fragment: String?)

  /** NCX/nav entry preserving parent-child hierarchy for TOC display. */
  private data class NavPointNode(
    val title: String,
    val hrefPath: String,
    val fragment: String?,
    val children: List<NavPointNode>
  )

  private data class OpfData(
    val title: String,
    val author: String,
    val language: String,
    val description: String,
    val dateYear: Int?,
    val categories: List<String>,
    val manifest: Map<String, ManifestItem>,
    val spine: List<SpineItem>,
    val coverManifestId: String?,
    val tocManifestId: String?
  )

  @Suppress("CyclomaticComplexMethod")
  private fun parseOpf(xml: String): OpfData {
    val doc = parseXml(xml)

    val title = doc.getElementsByTagName("dc:title").item(0)?.textContent?.trim() ?: ""
    val author = doc.getElementsByTagName("dc:creator").item(0)?.textContent?.trim() ?: ""
    val language = doc.getElementsByTagName("dc:language").item(0)?.textContent?.trim() ?: ""
    val description = doc.getElementsByTagName("dc:description").item(0)?.textContent?.trim() ?: ""
    val dateYear = doc.getElementsByTagName("dc:date").item(0)?.textContent?.trim()?.take(4)?.toIntOrNull()

    val categories = mutableListOf<String>()
    val subjectNodes = doc.getElementsByTagName("dc:subject")
    for (i in 0 until subjectNodes.length) {
      subjectNodes.item(i).textContent?.trim()?.takeIf { it.isNotEmpty() }?.let { categories.add(it) }
    }

    var coverManifestId: String? = null
    val metaNodes = doc.getElementsByTagName("meta")
    for (i in 0 until metaNodes.length) {
      val attrs = metaNodes.item(i).attributes ?: continue
      if (attrs.getNamedItem("name")?.nodeValue == "cover") {
        coverManifestId = attrs.getNamedItem("content")?.nodeValue
        break
      }
    }

    val manifest = mutableMapOf<String, ManifestItem>()
    val manifestNodes = doc.getElementsByTagName("item")
    for (i in 0 until manifestNodes.length) {
      val attrs = manifestNodes.item(i).attributes ?: continue
      val id = attrs.getNamedItem("id")?.nodeValue ?: continue
      val href = attrs.getNamedItem("href")?.nodeValue ?: continue
      val mediaType = attrs.getNamedItem("media-type")?.nodeValue ?: ""
      val properties = attrs.getNamedItem("properties")?.nodeValue ?: ""
      manifest[id] = ManifestItem(href, mediaType, properties)
      if ("cover-image" in properties.split(Regex("\\s+"))) coverManifestId = id
    }
    if (coverManifestId == null) {
      val coverItem = manifest["cover"]
      if (coverItem != null && coverItem.mediaType.startsWith("image/")) coverManifestId = "cover"
    }

    var spineTocId: String? = null
    val spineNodes = doc.getElementsByTagName("spine")
    if (spineNodes.length > 0) {
      spineTocId = spineNodes.item(0).attributes?.getNamedItem("toc")?.nodeValue
    }
    val spine = mutableListOf<SpineItem>()
    val itemrefNodes = doc.getElementsByTagName("itemref")
    for (i in 0 until itemrefNodes.length) {
      val attrs = itemrefNodes.item(i).attributes ?: continue
      val idref = attrs.getNamedItem("idref")?.nodeValue ?: continue
      val linearAttr = attrs.getNamedItem("linear")?.nodeValue
      val linear = linearAttr == null || linearAttr.equals("yes", ignoreCase = true)
      spine.add(SpineItem(idref, linear))
    }

    // Prefer EPUB2 NCX, fall back to EPUB3 nav
    val ncxManifestId = spineTocId
      ?: manifest.entries.firstOrNull { it.value.mediaType == "application/x-dtbncx+xml" }?.key
    val navManifestId = manifest.entries
      .firstOrNull { "nav" in it.value.properties.split(Regex("\\s+")) }?.key
    val tocManifestId = ncxManifestId ?: navManifestId

    return OpfData(
      title = title,
      author = author,
      language = language,
      description = description,
      dateYear = dateYear,
      categories = categories,
      manifest = manifest,
      spine = spine,
      coverManifestId = coverManifestId,
      tocManifestId = tocManifestId
    )
  }

  // ─────────────────────── TOC: NCX & nav ───────────────────────────────────

  private fun parseNcx(xml: String, ncxPath: String): List<NavPoint> {
    val doc = parseXml(xml)
    val result = mutableListOf<NavPoint>()
    val navPointNodes = doc.getElementsByTagName("navPoint")
    for (i in 0 until navPointNodes.length) {
      val np = navPointNodes.item(i)
      val children = np.childNodes
      var title = ""
      var src = ""
      for (j in 0 until children.length) {
        val child = children.item(j)
        when (child.nodeName.substringAfterLast(':')) {
          "navLabel" -> {
            val labelChildren = child.childNodes
            for (k in 0 until labelChildren.length) {
              if (labelChildren.item(k).nodeName.substringAfterLast(':') == "text") {
                title = labelChildren.item(k).textContent?.trim() ?: ""
              }
            }
          }

          "content" -> src = child.attributes?.getNamedItem("src")?.nodeValue ?: ""
        }
      }
      if (src.isEmpty()) continue
      val fragment = src.substringAfter('#', "").takeIf { it.isNotEmpty() }
      val hrefPath = resolvePath(ncxPath, src.substringBefore('#'))
      result.add(NavPoint(title, hrefPath, fragment))
    }
    return result
  }

  private fun parseNav(html: String, navPath: String): List<NavPoint> {
    val doc = Jsoup.parse(html, "", Parser.xmlParser())
    val nav = doc.selectFirst("nav[epub|type=toc]")
      ?: doc.selectFirst("nav[epub:type=toc]")
      ?: doc.selectFirst("nav")
      ?: return emptyList()
    return nav.select("a").mapNotNull { a ->
      val href = a.attr("href").trim().ifEmpty { return@mapNotNull null }
      val fragment = href.substringAfter('#', "").takeIf { it.isNotEmpty() }
      val rawHref = href.substringBefore('#')
      val hrefPath = if (rawHref.isEmpty()) navPath else resolvePath(navPath, rawHref)
      NavPoint(a.text().trim(), hrefPath, fragment)
    }
  }

  /** Parses NCX into a hierarchy of [NavPointNode]s preserving parent-child nesting. */
  private fun parseNcxHierarchy(xml: String, ncxPath: String): List<NavPointNode> {
    val doc = parseXml(xml)
    val navMap = doc.getElementsByTagName("navMap").item(0) ?: return emptyList()
    return parseNavPointNodeChildren(navMap.childNodes, ncxPath)
  }

  private fun parseNavPointNodeChildren(
    nodes: NodeList,
    ncxPath: String
  ): List<NavPointNode> {
    val result = mutableListOf<NavPointNode>()
    for (i in 0 until nodes.length) {
      val node = nodes.item(i)
      if (node.nodeName.substringAfterLast(':') == "navPoint") {
        result.add(parseNavPointNodeElement(node, ncxPath))
      }
    }
    return result
  }

  private fun parseNavPointNodeElement(node: Node, ncxPath: String): NavPointNode {
    var title = ""
    var src = ""
    val childNodes = mutableListOf<NavPointNode>()
    val children = node.childNodes
    for (i in 0 until children.length) {
      val child = children.item(i)
      when (child.nodeName.substringAfterLast(':')) {
        "navLabel" -> {
          val labelChildren = child.childNodes
          for (k in 0 until labelChildren.length) {
            if (labelChildren.item(k).nodeName.substringAfterLast(':') == "text") {
              title = labelChildren.item(k).textContent?.trim() ?: ""
            }
          }
        }

        "content" -> src = child.attributes?.getNamedItem("src")?.nodeValue ?: ""
        "navPoint" -> childNodes.add(parseNavPointNodeElement(child, ncxPath))
      }
    }
    val fragment = src.substringAfter('#', "").takeIf { it.isNotEmpty() }
    val hrefPath = resolvePath(ncxPath, src.substringBefore('#'))
    return NavPointNode(title, hrefPath, fragment, childNodes)
  }

  /** Parses EPUB3 nav TOC into a hierarchy of [NavPointNode]s using nested &lt;ol&gt; elements. */
  private fun parseNavHierarchy(html: String, navPath: String): List<NavPointNode> {
    val doc = Jsoup.parse(html, "", Parser.xmlParser())
    val nav = doc.selectFirst("nav[epub|type=toc]")
      ?: doc.selectFirst("nav[epub:type=toc]")
      ?: doc.selectFirst("nav")
      ?: return emptyList()
    val rootOl = nav.selectFirst("ol") ?: return emptyList()
    return parseNavOlChildren(rootOl, navPath)
  }

  private fun parseNavOlChildren(ol: Element, navPath: String): List<NavPointNode> =
    ol.children().filter { it.tagName() == "li" }.mapNotNull { li ->
      val a = li.selectFirst("a") ?: return@mapNotNull null
      val href = a.attr("href").trim().ifEmpty { return@mapNotNull null }
      val fragment = href.substringAfter('#', "").takeIf { it.isNotEmpty() }
      val rawHref = href.substringBefore('#')
      val hrefPath = if (rawHref.isEmpty()) navPath else resolvePath(navPath, rawHref)
      val children = li.selectFirst("ol")?.let { parseNavOlChildren(it, navPath) } ?: emptyList()
      NavPointNode(a.text().trim(), hrefPath, fragment, children)
    }

  /** Assigns chapter indices from [navToChapterIdx] map so that [TocItem.chapterIndex] matches the
   *  actual position of the chapter in [Book.chapters]. Nodes whose section produced no content
   *  AND whose children also have no content are omitted from the result. */
  private fun buildTocItems(
    nodes: List<NavPointNode>,
    navToChapterIdx: Map<Pair<String, String?>, Int>,
    maxIndex: Int
  ): List<TocItem> = nodes.mapNotNull { node ->
    val children = buildTocItems(node.children, navToChapterIdx, maxIndex)
    val directIndex = navToChapterIdx[node.hrefPath to node.fragment]
    // Drop the entry if it has no direct content and no children with content
    if (directIndex == null && children.isEmpty()) return@mapNotNull null
    val chapterIndex = directIndex
      ?: children.firstOrNull()?.chapterIndex
      ?: (maxIndex - 1).coerceAtLeast(0)
    TocItem(title = node.title, chapterIndex = chapterIndex, children = children)
  }

  // ─────────────────────── HTML → Chapter(s) ────────────────────────────────

  private fun parseChapter(
    html: String,
    chapterPath: String,
    cssRules: List<CssRule>,
    loadImage: (String) -> ByteArray?
  ): Chapter? {
    val doc = Jsoup.parse(html, "", Parser.xmlParser())
    val title = doc.selectFirst("h1, h2, h3")?.text() ?: ""
    val body = doc.body() ?: return null
    val nodes = body.childNodes().mapNotNull { parseNode(it, chapterPath, cssRules, emptyList(), loadImage) }
    return if (nodes.isEmpty()) null else Chapter(title, nodes)
  }

  /** Returns a list of (Chapter, fragment?) pairs where fragment is the TOC anchor that starts each
   *  chapter section (null for the section before the first anchor / whole-file entries). */
  private fun parseChaptersFromHtmlWithNav(
    html: String,
    chapterPath: String,
    cssRules: List<CssRule>,
    loadImage: (String) -> ByteArray?,
    navPoints: List<NavPoint>
  ): List<Pair<Chapter, String?>> {
    val doc = Jsoup.parse(html, "", Parser.xmlParser())
    val body = doc.body() ?: return emptyList()

    val fragmentedPoints = navPoints.filter { it.fragment != null }

    if (fragmentedPoints.isEmpty()) {
      val title = navPoints.firstOrNull()?.title ?: doc.selectFirst("h1, h2, h3")?.text() ?: ""
      val nodes = body.childNodes().mapNotNull { parseNode(it, chapterPath, cssRules, emptyList(), loadImage) }
      return if (nodes.isEmpty()) emptyList() else listOf(Chapter(title, nodes) to null)
    }

    val fragmentIds = fragmentedPoints.mapNotNull { it.fragment }.toSet()
    val contentRoot = findContentRoot(body, fragmentIds)
    val contentChildren = contentRoot.childNodes().toList()

    data class SplitPoint(val childIndex: Int, val navPoint: NavPoint)

    val splitPoints = fragmentedPoints.mapNotNull { np ->
      val fragment = np.fragment ?: return@mapNotNull null
      val idx = contentChildren.indexOfFirst { node ->
        node is Element && (node.id() == fragment || node.getElementById(fragment) != null)
      }
      if (idx >= 0) SplitPoint(idx, np) else null
    }.sortedBy { it.childIndex }.distinctBy { it.childIndex }

    if (splitPoints.isEmpty()) {
      val title = navPoints.firstOrNull()?.title ?: doc.selectFirst("h1, h2, h3")?.text() ?: ""
      val nodes = contentChildren.mapNotNull { parseNode(it, chapterPath, cssRules, emptyList(), loadImage) }
      return if (nodes.isEmpty()) emptyList() else listOf(Chapter(title, nodes) to null)
    }

    data class Section(val title: String, val jsoupNodes: List<org.jsoup.nodes.Node>, val fragment: String?)

    val sections = mutableListOf<Section>()
    val firstTitle = navPoints.firstOrNull { it.fragment == null }?.title ?: navPoints.first().title
    var prevIdx = 0
    var prevTitle = firstTitle
    var prevFragment: String? = null
    for (sp in splitPoints) {
      sections.add(Section(prevTitle, contentChildren.subList(prevIdx, sp.childIndex), prevFragment))
      prevTitle = sp.navPoint.title
      prevFragment = sp.navPoint.fragment
      prevIdx = sp.childIndex
    }
    sections.add(Section(prevTitle, contentChildren.subList(prevIdx, contentChildren.size), prevFragment))

    return sections.mapNotNull { (title, jsoupNodes, fragment) ->
      val nodes = jsoupNodes.mapNotNull { parseNode(it, chapterPath, cssRules, emptyList(), loadImage) }
      if (nodes.isEmpty()) null else Chapter(title, nodes) to fragment
    }
  }

  private fun findContentRoot(root: Element, fragmentIds: Set<String>): Element {
    var current = root
    while (true) {
      val elementChildren = current.children()
      if (elementChildren.size != 1) break
      val onlyChild = elementChildren[0]
      val allInside = fragmentIds.all { id ->
        onlyChild.id() == id || onlyChild.getElementById(id) != null
      }
      if (!allInside) break
      current = onlyChild
    }
    return current
  }

  // ─────────────────────── Node parsing ─────────────────────────────────────

  private fun parseNode(
    node: org.jsoup.nodes.Node,
    chapterPath: String,
    cssRules: List<CssRule>,
    ancestors: List<String>,
    loadImage: (String) -> ByteArray?
  ): ru.kode.epub.lib.entity.Node? = when (node) {
    is TextNode -> node.text().takeIf { it.isNotBlank() }?.let { ru.kode.epub.lib.entity.Node.Text(it) }
    is Element -> parseElement(node, chapterPath, cssRules, ancestors, loadImage)
    else -> null
  }

  @Suppress("CyclomaticComplexMethod", "ReturnCount")
  private fun parseElement(
    element: Element,
    chapterPath: String,
    cssRules: List<CssRule>,
    ancestors: List<String>,
    loadImage: (String) -> ByteArray?
  ): ru.kode.epub.lib.entity.Node? {
    val tag = element.tagName().lowercase()

    if (tag in NON_CONTENT_TAGS) return null
    if (element.attr("data-type") == "pagebreak") return null

    // Invisible via display:none
    val inlineStyle = element.attr("style").takeIf { it.isNotEmpty() }
    val rawDecls = CssResolver.resolveRaw(
      rules = cssRules,
      tag = tag,
      classes = element.classNames(),
      ancestors = ancestors,
      id = element.id().takeIf { it.isNotEmpty() },
      inlineStyle = inlineStyle
    )
    if (rawDecls["display"] == "none" || rawDecls["visibility"] == "hidden") return null

    val style = toStyle(rawDecls, chapterPath, loadImage)
    val epubType = element.attr("epub:type").ifEmpty { element.attr("epub|type") }.takeIf { it.isNotEmpty() }

    // ── <img> / <image> ────────────────────────────────────────────────────
    if (tag == "img" || tag == "image") {
      val src = element.attr("src")
        .ifBlank { element.attr("xlink:href") }
        .ifBlank { element.attr("href") }
        .trim()
      if (src.isEmpty()) return null
      val imagePath = resolvePath(chapterPath, src)
      val bytes = loadImage(imagePath) ?: return null
      return ru.kode.epub.lib.entity.Node.Image(
        data = bytes,
        mimeType = guessMimeType(src),
        alt = element.attr("alt"),
        style = style
      )
    }

    // ── <hr> ───────────────────────────────────────────────────────────────
    if (tag == "hr") return ru.kode.epub.lib.entity.Node.HorizontalRule(style)

    // ── <table> ───────────────────────────────────────────────────────────
    if (tag == "table") return parseTable(element, chapterPath, cssRules, ancestors + tag, loadImage, style)

    // ── <ul> / <ol> ───────────────────────────────────────────────────────
    if (tag == "ul" || tag == "ol") return parseList(
      element,
      chapterPath,
      cssRules,
      ancestors + tag,
      loadImage,
      style,
      ordered = tag == "ol"
    )

    // Skip bare <li> not inside a list we parsed — shouldn't happen normally
    if (tag == "li") {
      val children = parseChildren(element, chapterPath, cssRules, ancestors + tag, loadImage)
      return if (children.isEmpty()) null else ru.kode.epub.lib.entity.Node.Element(
        tag,
        element.idOrNull,
        epubType,
        style,
        children
      )
    }

    // ── Inline flattening: p, span, headings, etc. with only inline content ─
    if (hasOnlyInlineContent(element)) {
      val (raw, spans) = flattenInlineToSpans(element)
      if (raw.isBlank() && spans.isEmpty()) return null
      val textNode = ru.kode.epub.lib.entity.Node.Text(raw, spans)
      return ru.kode.epub.lib.entity.Node.Element(
        tag = tag,
        id = element.idOrNull,
        epubType = epubType,
        style = style,
        children = if (raw.isBlank()) emptyList() else listOf(textNode)
      )
    }

    // ── Generic block element ──────────────────────────────────────────────
    val children = parseChildren(element, chapterPath, cssRules, ancestors + tag, loadImage)
    return if (children.isEmpty() && style == null && epubType == null) {
      null
    } else {
      ru.kode.epub.lib.entity.Node.Element(tag, element.idOrNull, epubType, style, children)
    }
  }

  private fun parseChildren(
    element: Element,
    chapterPath: String,
    cssRules: List<CssRule>,
    ancestors: List<String>,
    loadImage: (String) -> ByteArray?
  ) = element.childNodes().mapNotNull { parseNode(it, chapterPath, cssRules, ancestors, loadImage) }

  private fun parseTable(
    table: Element,
    chapterPath: String,
    cssRules: List<CssRule>,
    ancestors: List<String>,
    loadImage: (String) -> ByteArray?,
    style: Style?
  ): ru.kode.epub.lib.entity.Node.Table {
    val rows = mutableListOf<TableRow>()
    // Collect tr elements from thead/tbody/tfoot/direct children
    val trElements = table.select("tr")
    for (tr in trElements) {
      val cells = mutableListOf<TableCell>()
      for (cell in tr.children()) {
        val cellTag = cell.tagName().lowercase()
        if (cellTag != "td" && cellTag != "th") continue
        val cellAncestors = ancestors + "tr"
        val cellStyle = toStyle(
          CssResolver.resolveRaw(
            cssRules,
            cellTag,
            cell.classNames(),
            cellAncestors,
            cell.idOrNull,
            cell.attr("style").takeIf { it.isNotEmpty() }
          ),
          chapterPath,
          loadImage
        )
        val children = parseChildren(cell, chapterPath, cssRules, cellAncestors + cellTag, loadImage)
        cells.add(
          TableCell(
            children = children,
            header = cellTag == "th",
            colspan = cell.attr("colspan").toIntOrNull() ?: 1,
            rowspan = cell.attr("rowspan").toIntOrNull() ?: 1,
            style = cellStyle
          )
        )
      }
      if (cells.isNotEmpty()) rows.add(TableRow(cells))
    }
    return ru.kode.epub.lib.entity.Node.Table(rows, style)
  }

  private fun parseList(
    list: Element,
    chapterPath: String,
    cssRules: List<CssRule>,
    ancestors: List<String>,
    loadImage: (String) -> ByteArray?,
    style: Style?,
    ordered: Boolean
  ): ru.kode.epub.lib.entity.Node.BulletList {
    val items = mutableListOf<ListItem>()
    var index = 1
    // Respect start attribute on <ol>
    list.attr("start").toIntOrNull()?.let { index = it }
    for (li in list.children()) {
      if (li.tagName().lowercase() != "li") continue
      val liStyle = toStyle(
        CssResolver.resolveRaw(
          cssRules,
          "li",
          li.classNames(),
          ancestors,
          li.idOrNull,
          li.attr("style").takeIf { it.isNotEmpty() }
        ),
        chapterPath,
        loadImage
      )
      val children = parseChildren(li, chapterPath, cssRules, ancestors + "li", loadImage)
      items.add(ListItem(index, children, liStyle))
      if (ordered) index++
    }
    return ru.kode.epub.lib.entity.Node.BulletList(ordered, items, style)
  }

  // ─────────────────────── Inline flattening ────────────────────────────────

  private val Element.idOrNull get() = id().takeIf { it.isNotEmpty() }

  /** Returns true if all direct children are inline (text or inline-tag elements). */
  private fun hasOnlyInlineContent(element: Element): Boolean =
    element.childNodes().all { child ->
      when (child) {
        is TextNode -> true
        is Element -> child.tagName().lowercase() in INLINE_TAGS
        else -> true
      }
    }

  /**
   * Recursively traverses [element]'s inline subtree and produces a plain [raw] string
   * together with a list of [Span] objects carrying formatting for character ranges.
   * Handles bold, italic, underline, strikethrough, super/subscript, small-caps, color.
   */
  private fun flattenInlineToSpans(element: Element): Pair<String, List<Span>> {
    val sb = StringBuilder()
    val spans = mutableListOf<Span>()
    flattenInlineRecursive(element, sb, spans, InlineStyle())
    return sb.toString() to spans
  }

  private data class InlineStyle(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val superscript: Boolean = false,
    val subscript: Boolean = false,
    val smallCaps: Boolean = false,
    val color: Long? = null,
    val fontSize: Length? = null
  )

  @Suppress("CyclomaticComplexMethod")
  private fun flattenInlineRecursive(
    element: Element,
    sb: StringBuilder,
    spans: MutableList<Span>,
    style: InlineStyle
  ) {
    for (child in element.childNodes()) {
      when (child) {
        is TextNode -> {
          val text = child.text()
          if (text.isEmpty()) continue
          val start = sb.length
          sb.append(text)
          val end = sb.length
          // Only add a span if there is any active formatting
          if (style.bold || style.italic || style.underline || style.strikethrough ||
            style.superscript || style.subscript || style.smallCaps ||
            style.color != null || style.fontSize != null
          ) {
            spans.add(
              Span(
                start = start,
                end = end,
                bold = style.bold,
                italic = style.italic,
                underline = style.underline,
                strikethrough = style.strikethrough,
                superscript = style.superscript,
                subscript = style.subscript,
                smallCaps = style.smallCaps,
                color = style.color,
                fontSize = style.fontSize
              )
            )
          }
        }

        is Element -> {
          val childTag = child.tagName().lowercase()
          if (childTag == "br") {
            sb.append('\n'); continue
          }
          val next = when (childTag) {
            "strong", "b" -> style.copy(bold = true)
            "em", "i" -> style.copy(italic = true)
            "u", "ins" -> style.copy(underline = true)
            "s", "del", "strike" -> style.copy(strikethrough = true)
            "sup" -> style.copy(superscript = true)
            "sub" -> style.copy(subscript = true)
            "small" -> style.copy(fontSize = Length.Em(0.8f))
            // Inspect inline style / class for span/a/etc.
            else -> {
              var next = style
              val inlineAttr = child.attr("style")
              if (inlineAttr.isNotEmpty()) {
                val decls = parseCssDeclarations(inlineAttr)
                if (decls["font-weight"]?.let {
                  it == "bold" || it.toIntOrNull()?.let { w -> w >= 600 } == true
                } == true
                ) next = next.copy(
                  bold = true
                )
                if (decls["font-style"] == "italic" || decls["font-style"] == "oblique") next = next.copy(italic = true)
                if (decls["text-decoration"]?.contains("underline") == true) next = next.copy(underline = true)
                if (decls["text-decoration"]?.contains("line-through") == true) next = next.copy(strikethrough = true)
                if (decls["vertical-align"] == "super") next = next.copy(superscript = true)
                if (decls["vertical-align"] == "sub") next = next.copy(subscript = true)
                if (decls["font-variant"]?.contains("small-caps") == true) next = next.copy(smallCaps = true)
                parseColor(decls["color"])?.let { next = next.copy(color = it) }
                decls["font-size"]?.let { parseLength(it) }?.let { next = next.copy(fontSize = it) }
              }
              next
            }
          }
          flattenInlineRecursive(child, sb, spans, next)
        }
      }
    }
  }

  // ─────────────────────── Misc helpers ─────────────────────────────────────

  private val NON_CONTENT_TAGS = setOf(
    "head",
    "script",
    "style",
    "link",
    "meta",
    "title",
    "noscript"
  )

  private val INLINE_TAGS = setOf(
    "strong", "b", "em", "i", "span", "a", "code", "br",
    "sub", "sup", "mark", "s", "del", "ins", "u", "small",
    "abbr", "acronym", "cite", "q", "time", "var", "samp", "kbd",
    "strike", "bdi", "bdo", "ruby", "rt", "rp", "wbr"
  )

  private fun parseXml(xml: String) = DocumentBuilderFactory.newInstance()
    .apply { isNamespaceAware = false; isValidating = false }
    .newDocumentBuilder()
    .apply { setErrorHandler(null); setEntityResolver { _, _ -> InputSource(StringReader("")) } }
    .parse(InputSource(StringReader(xml)))

  private fun joinPaths(dir: String, href: String): String {
    if (dir.isEmpty()) return href
    if (href.startsWith("/")) return href.removePrefix("/")
    return "$dir/$href"
  }
}
