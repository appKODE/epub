package ru.kode.epub.lib

import android.content.Context
import android.net.Uri
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.parser.Parser
import org.w3c.dom.Document
import org.xml.sax.InputSource
import ru.kode.epub.lib.entity.ContentElement
import ru.kode.epub.lib.entity.EpubBook
import ru.kode.epub.lib.entity.EpubChapter
import ru.kode.epub.lib.entity.EpubFontFile
import ru.kode.epub.lib.entity.StyledText
import ru.kode.epub.lib.entity.StyledTextBuilder
import ru.kode.epub.lib.entity.TocEntry
import ru.kode.epub.lib.entity.buildStyledText
import java.io.BufferedInputStream
import java.io.StringReader
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

object EpubParser {

  fun parse(context: Context, uri: Uri): EpubBook {
    val fileMap = readZipEntries(context, uri)

    val containerXml = fileMap["META-INF/container.xml"]
      ?.toString(Charsets.UTF_8)
      ?: error("META-INF/container.xml not found in epub")
    val opfPath = parseContainerXml(containerXml)

    val opfContent = fileMap[opfPath]
      ?.toString(Charsets.UTF_8)
      ?: error("OPF file not found at: $opfPath")
    val opfDir = opfPath.substringBeforeLast("/", "")

    val opfData = parseOpf(opfContent)

    // ── Collect CSS rules from all stylesheets in the manifest ───────
    val cssRules = mutableListOf<CssRule>()
    opfData.manifest.values.forEach { item ->
      val isCss = item.mediaType.contains("css", ignoreCase = true) ||
        item.href.endsWith(".css", ignoreCase = true)
      if (!isCss) return@forEach
      val cssPath = joinPaths(opfDir, item.href)
      val cssText = fileMap[cssPath]?.toString(Charsets.UTF_8) ?: return@forEach
      cssRules.addAll(CssParser.parse(cssText))
    }

    // ── Build chapters from spine ──────────────────────────────────
    val chapters = mutableListOf<EpubChapter>()
    val chapterPathToIndex = mutableMapOf<String, Int>()

    opfData.spine.forEachIndexed { index, spineItem ->
      val item = opfData.manifest[spineItem.idref] ?: return@forEachIndexed
      val itemPath = joinPaths(opfDir, item.href)
      val content = fileMap[itemPath]?.toString(Charsets.UTF_8) ?: return@forEachIndexed

      val chapter = parseHtmlChapter(
        html = content,
        chapterPath = itemPath,
        fileMap = fileMap,
        inToc = spineItem.linear,
        fallbackTitle = "Chapter ${index + 1}",
        cssRules = cssRules
      )
      chapterPathToIndex[itemPath] = chapters.size
      chapters.add(chapter)
    }

    // ── Parse NCX table of contents ───────────────────────────────
    val toc = opfData.tocNcxId
      ?.let { ncxId -> opfData.manifest[ncxId] }
      ?.let { ncxItem ->
        val ncxPath = joinPaths(opfDir, ncxItem.href)
        val ncxContent = fileMap[ncxPath]?.toString(Charsets.UTF_8)
        ncxContent?.let { parseToc(ncxContent, ncxPath, chapterPathToIndex) }
      }
      ?: emptyList()

    // ── Cover image ───────────────────────────────────────────────
    val coverImage = opfData.coverManifestId
      ?.let { coverId -> opfData.manifest[coverId] }
      ?.let { coverItem ->
        val coverPath = joinPaths(opfDir, coverItem.href)
        fileMap[coverPath]?.let { data ->
          ContentElement.EpubImage(data, "cover")
        }
      }

    // ── Font files ────────────────────────────────────────────────
    val fontFiles = opfData.manifest.values.mapNotNull { item ->
      val ext = item.href.substringAfterLast('.').lowercase()
      val isFontType = item.mediaType.contains("font", ignoreCase = true) ||
        ext in setOf("ttf", "otf", "woff", "woff2")
      if (!isFontType) return@mapNotNull null
      val fontPath = joinPaths(opfDir, item.href)
      val bytes = fileMap[fontPath] ?: return@mapNotNull null
      EpubFontFile(name = item.href.substringAfterLast('/'), bytes = bytes)
    }

    return EpubBook(
      title = opfData.metadata.title.ifBlank { "Unknown Title" },
      author = opfData.metadata.author.ifBlank { "Unknown Author" },
      coverImage = coverImage,
      categories = opfData.metadata.categories,
      language = opfData.metadata.language,
      description = opfData.metadata.description,
      dateYear = opfData.metadata.dateYear,
      chapters = chapters,
      toc = toc,
      fontFiles = fontFiles
    )
  }

  // ─────────────────────────── ZIP ───────────────────────────────────
  @Suppress("NestedBlockDepth", "CyclomaticComplexMethod")
  private fun readZipEntries(context: Context, uri: Uri): Map<String, ByteArray> {
    val map = mutableMapOf<String, ByteArray>()
    context.contentResolver.openInputStream(uri)?.use { raw ->
      ZipInputStream(BufferedInputStream(raw)).use { zip ->
        var entry = zip.nextEntry
        while (entry != null) {
          if (!entry.isDirectory) {
            map[entry.name] = zip.readBytes()
          }
          zip.closeEntry()
          entry = zip.nextEntry
        }
      }
    }
    return map
  }

  // ──────────────────────── container.xml ────────────────────────────

  private fun parseContainerXml(xml: String): String {
    val doc = parseXml(xml)
    val rootfiles = doc.getElementsByTagName("rootfile")
    for (i in 0 until rootfiles.length) {
      val path = rootfiles.item(i).attributes?.getNamedItem("full-path")?.nodeValue
      if (!path.isNullOrBlank()) return path
    }
    error("rootfile/@full-path not found in container.xml")
  }

  // ──────────────────────────── OPF ──────────────────────────────────

  private data class OpfMetadata(
    val title: String,
    val author: String,
    val categories: List<String>,
    val language: String,
    val description: String,
    val dateYear: Int?
  )

  private data class ManifestItem(val id: String, val href: String, val mediaType: String)

  private data class SpineItem(val idref: String, val linear: Boolean)

  private data class OpfData(
    val metadata: OpfMetadata,
    val manifest: Map<String, ManifestItem>,
    val spine: List<SpineItem>,
    val tocNcxId: String?,
    val coverManifestId: String?
  )

  @Suppress("LoopWithTooManyJumpStatements", "CyclomaticComplexMethod")
  private fun parseOpf(xml: String): OpfData {
    val doc = parseXml(xml)

    // ── Metadata ──
    val title = doc.getElementsByTagName("dc:title").item(0)?.textContent?.trim() ?: ""
    val author = doc.getElementsByTagName("dc:creator").item(0)?.textContent?.trim() ?: ""
    val language = doc.getElementsByTagName("dc:language").item(0)?.textContent?.trim() ?: ""
    val description = doc.getElementsByTagName("dc:description").item(0)?.textContent?.trim() ?: ""

    val dateRaw = doc.getElementsByTagName("dc:date").item(0)?.textContent?.trim() ?: ""
    val dateYear = dateRaw.take(4).toIntOrNull()

    val categories = mutableListOf<String>()
    val subjectNodes = doc.getElementsByTagName("dc:subject")
    for (i in 0 until subjectNodes.length) {
      subjectNodes.item(i).textContent?.trim()?.let { if (it.isNotEmpty()) categories.add(it) }
    }

    // Cover: <meta name="cover" content="manifest-id"/>
    var coverManifestId: String? = null
    val metaNodes = doc.getElementsByTagName("meta")
    for (i in 0 until metaNodes.length) {
      val attrs = metaNodes.item(i).attributes ?: continue
      if (attrs.getNamedItem("name")?.nodeValue == "cover") {
        coverManifestId = attrs.getNamedItem("content")?.nodeValue
        break
      }
    }

    // ── Manifest ──
    val manifest = mutableMapOf<String, ManifestItem>()
    val manifestNodes = doc.getElementsByTagName("item")
    for (i in 0 until manifestNodes.length) {
      val attrs = manifestNodes.item(i).attributes ?: continue
      val id = attrs.getNamedItem("id")?.nodeValue ?: continue
      val href = attrs.getNamedItem("href")?.nodeValue ?: continue
      val mediaType = attrs.getNamedItem("media-type")?.nodeValue ?: ""
      manifest[id] = ManifestItem(id, href, mediaType)
    }

    // ── Spine ──
    var tocNcxId: String? = null
    val spineNodes = doc.getElementsByTagName("spine")
    if (spineNodes.length > 0) {
      tocNcxId = spineNodes.item(0).attributes?.getNamedItem("toc")?.nodeValue
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

    return OpfData(
      metadata = OpfMetadata(title, author, categories, language, description, dateYear),
      manifest = manifest,
      spine = spine,
      tocNcxId = tocNcxId,
      coverManifestId = coverManifestId
    )
  }

  // ──────────────────────── NCX / TOC ────────────────────────────────

  private fun parseToc(
    ncxContent: String,
    ncxPath: String,
    chapterPathToIndex: Map<String, Int>
  ): List<TocEntry> {
    val doc = Jsoup.parse(ncxContent, "", Parser.xmlParser())
    return doc.select("navMap > navPoint")
      .sortedBy { it.attr("playOrder").toIntOrNull() ?: Int.MAX_VALUE }
      .mapNotNull { parseNavPoint(it, ncxPath, chapterPathToIndex) }
  }

  private fun parseNavPoint(
    navPoint: Element,
    ncxPath: String,
    chapterPathToIndex: Map<String, Int>
  ): TocEntry? {
    val title = navPoint.selectFirst("> navLabel > text")
      ?.text()
      ?.trim()
      ?.takeIf { it.isNotEmpty() }
      ?: return null

    val src = navPoint.selectFirst("> content")
      ?.attr("src")
      ?.takeIf { it.isNotEmpty() }
      ?: return null

    val srcFile = src.substringBefore("#")
    val anchorId = src.substringAfter("#", "").takeIf { it.isNotEmpty() }
    val absolutePath = resolvePath(ncxPath, srcFile)
    val chapterIndex = chapterPathToIndex[absolutePath] ?: return null

    val children = navPoint.select("> navPoint")
      .sortedBy { it.attr("playOrder").toIntOrNull() ?: Int.MAX_VALUE }
      .mapNotNull { parseNavPoint(it, ncxPath, chapterPathToIndex) }

    return TocEntry(
      title = title,
      chapterIndex = chapterIndex,
      anchorId = anchorId,
      children = children
    )
  }

  // ───────────────────────── HTML chapter ────────────────────────────

  private fun parseHtmlChapter(
    html: String,
    chapterPath: String,
    fileMap: Map<String, ByteArray>,
    inToc: Boolean,
    fallbackTitle: String,
    cssRules: List<CssRule>
  ): EpubChapter {
    val doc = Jsoup.parse(html)
    val chapterTitle = doc.title().ifBlank {
      doc.selectFirst("h1, h2, h3")?.text()?.ifBlank { fallbackTitle } ?: fallbackTitle
    }

    val elements = mutableListOf<ContentElement>()
    val anchorIndex = mutableMapOf<String, Int>()
    val body = doc.body() ?: return EpubChapter(chapterTitle, inToc, emptyList(), emptyMap())
    processElement(body, elements, anchorIndex, chapterPath, fileMap, cssRules, emptyList())

    return EpubChapter(chapterTitle, inToc, elements, anchorIndex)
  }

  @Suppress("NestedBlockDepth", "CyclomaticComplexMethod")
  private fun processElement(
    element: Element,
    result: MutableList<ContentElement>,
    anchorIndex: MutableMap<String, Int>,
    chapterPath: String,
    fileMap: Map<String, ByteArray>,
    cssRules: List<CssRule>,
    ancestors: List<String>
  ) {
    val tag = element.tagName().lowercase()
    val domId = element.id().takeIf { it.isNotEmpty() }

    fun styles() = CssResolver.resolve(
      rules = cssRules,
      tag = tag,
      classes = element.classNames(),
      ancestors = ancestors,
      inlineStyle = element.attr("style").takeIf { it.isNotEmpty() }
    )

    when {
      tag.matches(Regex("h[1-6]")) -> {
        val text = buildAnnotatedText(element)
        if (text.text.isNotBlank()) {
          domId?.let { anchorIndex[it] = result.size }
          result.add(ContentElement.Heading(text, tag[1].digitToInt(), styles()))
        }
      }

      tag == "img" || tag == "image" -> {
        addImage(element, chapterPath, fileMap, result)
      }

      tag == "p" -> {
        val text = buildAnnotatedText(element)
        if (text.text.isNotBlank()) {
          domId?.let { anchorIndex[it] = result.size }
          result.add(ContentElement.Paragraph(text, styles()))
        }
        element.select("img, image").forEach { img ->
          addImage(img, chapterPath, fileMap, result)
        }
      }

      tag == "blockquote" -> {
        domId?.let { anchorIndex[it] = result.size }
        val blockquoteStyles = styles()
        val blockChildren = element.children().filter {
          it.tagName().lowercase() in setOf("p", "div", "h1", "h2", "h3", "h4", "h5", "h6")
        }
        val newAncestors = ancestors + tag
        if (blockChildren.isNotEmpty()) {
          blockChildren.forEach { child ->
            val text = buildAnnotatedText(child)
            if (text.text.isNotBlank()) {
              child.id().takeIf { it.isNotEmpty() }?.let { anchorIndex[it] = result.size }
              val childStyles = CssResolver.resolve(
                rules = cssRules,
                tag = child.tagName().lowercase(),
                classes = child.classNames(),
                ancestors = newAncestors,
                inlineStyle = child.attr("style").takeIf { it.isNotEmpty() }
              )
              result.add(ContentElement.Quote(text, blockquoteStyles.mergeTextWith(childStyles)))
            }
          }
        } else {
          val text = buildAnnotatedText(element)
          if (text.text.isNotBlank()) result.add(ContentElement.Quote(text, blockquoteStyles))
        }
      }

      tag in setOf(
        "div", "section", "article", "body", "figure",
        "aside", "main", "header", "footer"
      ) -> {
        domId?.let { anchorIndex[it] = result.size }
        val newAncestors = ancestors + tag
        for (child in element.children()) {
          processElement(child, result, anchorIndex, chapterPath, fileMap, cssRules, newAncestors)
        }
      }

      tag == "br" || tag == "hr" -> {}

      else -> {
        if (element.children().isNotEmpty()) {
          for (child in element.children()) {
            processElement(child, result, anchorIndex, chapterPath, fileMap, cssRules, ancestors)
          }
        } else {
          val text = buildAnnotatedText(element)
          if (text.text.isNotBlank()) {
            domId?.let { anchorIndex[it] = result.size }
            result.add(ContentElement.Paragraph(text, styles()))
          }
        }
      }
    }
  }

  private fun addImage(
    img: Element,
    chapterPath: String,
    fileMap: Map<String, ByteArray>,
    result: MutableList<ContentElement>
  ) {
    val src = img.attr("src")
      .ifBlank { img.attr("xlink:href") }
      .ifBlank { img.attr("href") }
      .trim()
    if (src.isEmpty()) return

    val resolvedPath = resolvePath(chapterPath, src)
    val data = fileMap[resolvedPath]
      ?: fileMap.entries.firstOrNull { it.key.endsWith(resolvedPath) }?.value
      ?: return

    result.add(ContentElement.EpubImage(data, img.attr("alt")))
  }

  /**
   * Строит [StyledText] из узла, обрабатывая:
   * - <br>           → \n
   * - <b>, <strong>  → жирный
   * - <i>, <em>      → курсив
   */
  private fun buildAnnotatedText(node: Node): StyledText = buildStyledText {
    appendNode(node)
  }

  private fun StyledTextBuilder.appendNode(node: Node) {
    when {
      node is TextNode -> append(node.text())
      node is Element -> when (node.tagName().lowercase()) {
        "br" -> append('\n')
        "b", "strong" -> withBold {
          node.childNodes().forEach { appendNode(it) }
        }

        "i", "em" -> withItalic {
          node.childNodes().forEach { appendNode(it) }
        }

        else -> node.childNodes().forEach { appendNode(it) }
      }
    }
  }

  // ─────────────────────────── Helpers ───────────────────────────────

  private fun parseXml(xml: String): Document {
    val factory = DocumentBuilderFactory.newInstance().apply {
      isNamespaceAware = false
      isValidating = false
    }
    val builder = factory.newDocumentBuilder().apply {
      setErrorHandler(null)
      setEntityResolver { _, _ -> InputSource(StringReader("")) }
    }
    return builder.parse(InputSource(StringReader(xml)))
  }

  private fun resolvePath(base: String, relative: String): String {
    val rel = relative.substringBefore("#").trim()
    if (rel.isEmpty()) return base
    if (rel.startsWith("/")) return rel.removePrefix("/")

    val baseDir = base.substringBeforeLast("/", "")
    val stack: MutableList<String> =
      if (baseDir.isEmpty()) mutableListOf() else baseDir.split("/").toMutableList()

    rel.split("/").forEach { segment ->
      when (segment) {
        ".." -> if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
        ".", "" -> {}
        else -> stack.add(segment)
      }
    }
    return stack.joinToString("/")
  }

  private fun joinPaths(dir: String, href: String): String {
    if (dir.isEmpty()) return href
    if (href.startsWith("/")) return href.removePrefix("/")
    return "$dir/$href"
  }
}
