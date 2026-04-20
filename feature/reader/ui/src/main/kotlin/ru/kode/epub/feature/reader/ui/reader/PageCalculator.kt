package ru.kode.epub.feature.reader.ui.reader

import android.graphics.BitmapFactory
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import ru.kode.epub.lib.entity.Book
import ru.kode.epub.lib.entity.Node
import ru.kode.epub.lib.entity.PageBreak
import ru.kode.epub.lib.entity.Span
import ru.kode.epub.lib.entity.Style

/**
 * A block-level node with its position indices assigned during page calculation.
 * [index] is the global index across all chapters (used for progress save/restore).
 * [relativeIndex] is the index within the owning chapter's flattened block list.
 * [chapterIndex] is the index of the chapter this node belongs to.
 */
data class IndexedNode(
  val index: Int,
  val relativeIndex: Int,
  val chapterIndex: Int,
  val node: Node
)

/**
 * A single rendered page: flat list of block-level indexed nodes and the chapter it belongs to.
 */
data class Page(val items: List<IndexedNode>, val chapterIndex: Int = 0)

// CSS-default values for orphans and widows
private const val DEFAULT_ORPHANS = 2
private const val DEFAULT_WIDOWS = 2

// ─────────────────────────── Block flattening ─────────────────────────────────

private val BLOCK_ELEMENT_TAGS = setOf(
  "p", "h1", "h2", "h3", "h4", "h5", "h6",
  "blockquote", "pre", "figure", "figcaption",
  "dl", "dt", "dd", "aside", "header", "footer"
)

private val CONTAINER_TAGS = setOf(
  "div",
  "section",
  "article",
  "nav",
  "main",
  "body"
)

/**
 * Recursively flattens a node list to leaf block elements suitable for pagination.
 * Container divs/sections are unwrapped; typed block nodes (BulletList, Table, HorizontalRule)
 * and named block elements are kept as-is.
 */
internal fun List<Node>.flattenToBlocks(): List<Node> {
  val result = mutableListOf<Node>()
  for (node in this) {
    when {
      node is Node.Image -> result.add(node)
      node is Node.HorizontalRule -> result.add(node)
      node is Node.BulletList -> result.add(node)
      node is Node.Table -> result.add(node)
      node is Node.Text -> if (node.raw.isNotBlank()) result.add(node)
      node is Node.Element && node.tag in BLOCK_ELEMENT_TAGS -> result.add(node)
      node is Node.Element && node.tag in CONTAINER_TAGS -> {
        // Keep the container as a block if it carries visual styles (border, background),
        // so applyStyle() can render them. Otherwise just unwrap its children.
        if (node.style.hasVisualBox()) result.add(node)
        else result.addAll(node.children.flattenToBlocks())
      }
      node is Node.Element -> {
        // Unknown tag: if it has block children, unwrap; otherwise keep
        val hasBlockChildren = node.children.any {
          it is Node.Element && (it.tag in BLOCK_ELEMENT_TAGS || it.tag in CONTAINER_TAGS) ||
            it is Node.BulletList || it is Node.Table || it is Node.HorizontalRule
        }
        if (hasBlockChildren) result.addAll(node.children.flattenToBlocks()) else result.add(node)
      }
    }
  }
  return result
}

// ─────────────────────────── Main entry point ────────────────────────────────

/**
 * Paginates the entire [book] into a list of [Page] objects.
 *
 * - Each chapter always starts on a new page.
 * - `page-break-before: always` forces a new page before the element.
 * - Text paragraphs straddling a page boundary are split at a line boundary.
 * - A hyphen is appended when the split falls inside a word.
 * - CSS `orphans` and `widows` are respected: the split point may be adjusted
 *   to guarantee the minimum number of lines on each side of the break.
 *
 * No horizontal padding is added here — that must come from element CSS styles.
 */
internal fun calculatePages(
  book: Book,
  pageWidthPx: Int,
  pageHeightPx: Int,
  textMeasurer: TextMeasurer,
  density: Density,
  fontFamilyMap: Map<String, FontFamily>
): List<Page> {
  if (pageWidthPx <= 0 || pageHeightPx <= 0) return listOf(Page(emptyList()))

  val pages = mutableListOf<Page>()
  var currentItems = mutableListOf<IndexedNode>()
  var usedHeight = 0
  var currentChapterIndex = 0
  // Global block index counter — increments once per original block (before any split)
  var globalIndex = 0

  fun flushPage() {
    pages.add(Page(currentItems.toList(), currentChapterIndex))
    currentItems = mutableListOf()
    usedHeight = 0
  }

  for ((chapterIndex, chapter) in book.chapters.withIndex()) {
    if (currentItems.isNotEmpty()) flushPage()
    currentChapterIndex = chapterIndex

    for ((blockIdx, block) in chapter.nodes.flattenToBlocks().withIndex()) {
      // Assign a stable global index to this original block (both split halves share it)
      val nodeIndex = globalIndex++

      // Force page break before element if CSS requests it
      if (block.pageBreakBefore() == PageBreak.Always && currentItems.isNotEmpty()) {
        flushPage()
      }

      val blockHeight = measureNodeHeight(block, pageWidthPx, textMeasurer, density, fontFamilyMap)
      val remaining = pageHeightPx - usedHeight

      when {
        // Oversized block that alone exceeds the page — place it on its own page
        usedHeight == 0 && blockHeight >= pageHeightPx -> {
          currentItems.add(IndexedNode(nodeIndex, blockIdx, chapterIndex, block))
          flushPage()
        }
        // Block fits in remaining space
        blockHeight <= remaining -> {
          currentItems.add(IndexedNode(nodeIndex, blockIdx, chapterIndex, block))
          usedHeight += blockHeight
        }
        // Block doesn't fit — try splitting
        else -> {
          val split = if (remaining > 0) {
            trySplitParagraph(block, pageWidthPx, remaining, textMeasurer, density, fontFamilyMap)
          } else null

          if (split != null) {
            if (!split.first.isEmpty()) currentItems.add(IndexedNode(nodeIndex, blockIdx, chapterIndex, split.first))
            flushPage()
            currentItems.add(IndexedNode(nodeIndex, blockIdx, chapterIndex, split.second))
            usedHeight = measureNodeHeight(split.second, pageWidthPx, textMeasurer, density, fontFamilyMap)
          } else {
            // Can't split — push to next page
            flushPage()
            currentItems.add(IndexedNode(nodeIndex, blockIdx, chapterIndex, block))
            usedHeight = blockHeight
          }
        }
      }
    }
  }

  if (currentItems.isNotEmpty()) flushPage()
  return pages.ifEmpty { listOf(Page(emptyList())) }
}

// ─────────────────────────── Measurement ─────────────────────────────────────

internal fun measureNodeHeight(
  node: Node,
  availableWidthPx: Int,
  textMeasurer: TextMeasurer,
  density: Density,
  fontFamilyMap: Map<String, FontFamily>
): Int = when (node) {
  is Node.Text -> {
    val annotated = node.toAnnotatedString()
    textMeasurer.measure(
      text = annotated,
      style = TextStyle.Default.copy(hyphens = Hyphens.Auto),
      constraints = Constraints.fixedWidth(availableWidthPx.coerceAtLeast(1))
    ).size.height
  }
  is Node.Image -> measureImageHeight(node, availableWidthPx)?.plus(node.style.verticalSpacingPx(density)) ?: 0
  is Node.HorizontalRule -> with(density) { 16.dp.roundToPx() }
  is Node.BulletList -> measureBulletListHeight(node, availableWidthPx, textMeasurer, density, fontFamilyMap)
  is Node.Table -> measureTableHeight(node, availableWidthPx, textMeasurer, density, fontFamilyMap)
  is Node.Element -> measureElementHeight(node, availableWidthPx, textMeasurer, density, fontFamilyMap)
}

private fun measureImageHeight(node: Node.Image, availableWidthPx: Int): Int? {
  val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
  BitmapFactory.decodeByteArray(node.data, 0, node.data.size, opts)
  return if (opts.outWidth > 0 && opts.outHeight > 0) {
    val heightPx = opts.outHeight.toFloat()
    val aspectRatio = opts.outWidth.toFloat() / heightPx
    minOf((availableWidthPx / aspectRatio).toInt(), heightPx.toInt())
  } else {
    null
  }
}

private fun measureBulletListHeight(
  list: Node.BulletList,
  availableWidthPx: Int,
  textMeasurer: TextMeasurer,
  density: Density,
  fontFamilyMap: Map<String, FontFamily>
): Int {
  val bulletIndentPx = with(density) { 24.dp.roundToPx() }
  val itemWidth = (availableWidthPx - bulletIndentPx).coerceAtLeast(1)
  val listVPx = list.style.verticalSpacingPx(density)
  return listVPx + list.items.sumOf { item ->
    val itemVPx = item.style.verticalSpacingPx(density)
    itemVPx + item.children.sumOf { measureNodeHeight(it, itemWidth, textMeasurer, density, fontFamilyMap) }
  }
}

private fun measureTableHeight(
  table: Node.Table,
  availableWidthPx: Int,
  textMeasurer: TextMeasurer,
  density: Density,
  fontFamilyMap: Map<String, FontFamily>
): Int {
  val tableVPx = table.style.verticalSpacingPx(density)
  return tableVPx + table.rows.sumOf { row ->
    val cellCount = row.cells.size.coerceAtLeast(1)
    val cellWidth = (availableWidthPx / cellCount).coerceAtLeast(1)
    row.cells.maxOfOrNull { cell ->
      cell.children.sumOf { measureNodeHeight(it, cellWidth, textMeasurer, density, fontFamilyMap) }
    } ?: 0
  }
}

private fun measureElementHeight(
  element: Node.Element,
  availableWidthPx: Int,
  textMeasurer: TextMeasurer,
  density: Density,
  fontFamilyMap: Map<String, FontFamily>
): Int {
  val style = element.style

  val hPx = style.horizontalSpacingPx(density)
  val contentWidth = (availableWidthPx - hPx).coerceAtLeast(1)
  // Margins and padding are kept separate: CSS min-height (border-box) applies to content+padding,
  // not to margins. Margins must always be added on top of the min-height check.
  val marginPx = style.marginVerticalPx(density)
  val paddingPx = style.paddingVerticalPx(density)

  val textStyle = style?.toTextStyle(fontFamilyMap) ?: TextStyle.Default.copy(
    hyphens = Hyphens.Auto
  )

  val contentHeight = when {
    element.children.size == 1 && element.children[0] is Node.Text -> {
      val textNode = element.children[0] as Node.Text
      textMeasurer.measure(
        text = textNode.toAnnotatedString(),
        style = textStyle,
        constraints = Constraints.fixedWidth(contentWidth)
      ).size.height
    }
    else -> element.children.sumOf {
      measureNodeHeight(it, contentWidth, textMeasurer, density, fontFamilyMap)
    }
  }

  val minHeightPx = style?.minHeight?.toPx(density)?.toInt() ?: 0
  // min-height covers the content+padding box; margins are always added on top
  return maxOf(contentHeight + paddingPx, minHeightPx) + marginPx
}

// ─────────────────────────── Splitting ───────────────────────────────────────

/**
 * Tries to split [block] so the first part fits within [availableHeightPx].
 * Returns a pair (firstPart, secondPart) or null if splitting is impossible
 * (e.g. not enough space for even one line, or orphans/widows constraints prevent it).
 *
 * Only [Node.Element] nodes with a single [Node.Text] child can be split.
 * Multi-child elements attempt a child-boundary split instead.
 */
private fun trySplitParagraph(
  block: Node,
  availableWidthPx: Int,
  availableHeightPx: Int,
  textMeasurer: TextMeasurer,
  density: Density,
  fontFamilyMap: Map<String, FontFamily>
): Pair<Node, Node>? {
  if (block !is Node.Element) return null
  val element = block

  // Multi-child element: try splitting between children
  if (element.children.size > 1) {
    return trySplitBetweenChildren(element, availableWidthPx, availableHeightPx, textMeasurer, density, fontFamilyMap)
  }

  val textNode = element.children.singleOrNull() as? Node.Text ?: return null
  val style = element.style

  val hPx = style.horizontalSpacingPx(density)
  val contentWidth = (availableWidthPx - hPx).coerceAtLeast(1)
  val vPx = style.verticalSpacingPx(density)
  val textAvailable = (availableHeightPx - vPx).coerceAtLeast(0)
  if (textAvailable <= 0) return null

  val textStyle = style?.toTextStyle(fontFamilyMap)
    ?: TextStyle.Default.copy(hyphens = Hyphens.Auto)

  val layout = textMeasurer.measure(
    text = textNode.toAnnotatedString(),
    style = textStyle,
    constraints = Constraints.fixedWidth(contentWidth)
  )
  if (layout.size.height <= textAvailable) return null
  val totalLines = layout.lineCount

  // Find the last line whose bottom edge fits within available height
  var lastFitLine = -1
  for (i in 0 until totalLines) {
    if (layout.getLineBottom(i).toInt() <= textAvailable) lastFitLine = i else break
  }
  if (lastFitLine < 0) return null

  // ── Apply orphans / widows constraints ──────────────────────────────────
  val orphans = style?.orphans ?: DEFAULT_ORPHANS
  val widows = style?.widows ?: DEFAULT_WIDOWS

  // Lines in first part = lastFitLine + 1, lines in second part = totalLines - (lastFitLine + 1)
  val firstPartLines = lastFitLine + 1
  val secondPartLines = totalLines - firstPartLines

  var splitLine = lastFitLine

  // widows: second part must have >= widows lines
  if (secondPartLines < widows) {
    // Move the split point back so second part gains lines
    val needed = widows - secondPartLines
    splitLine -= needed
  }

  // orphans: first part must have >= orphans lines
  if (splitLine + 1 < orphans) return null // impossible to satisfy both

  if (splitLine < 0) return null

  // ── Find split offset in text ────────────────────────────────────────────
  val splitOffset = layout.getLineEnd(splitLine, visibleEnd = true)
    .coerceAtMost(textNode.raw.length)

  val rawBefore = textNode.raw.substring(0, splitOffset)
  val rawAfter = textNode.raw.substring(splitOffset)
  if (rawAfter.isBlank()) return null

  val isMidWord = rawBefore.lastOrNull()?.isLetterOrDigit() == true &&
    rawAfter.firstOrNull()?.isLetterOrDigit() == true

  val (firstText, secondText) = splitNodeText(textNode, splitOffset, addHyphen = isMidWord)
  if (secondText.raw.isBlank()) return null

  // Second part of a split paragraph has no text-indent (it's a continuation)
  val secondStyle = if (style?.textIndent != null) style.copy(textIndent = null) else style

  val firstPart = element.copy(children = listOf(firstText))
  val secondPart = element.copy(style = secondStyle, children = listOf(secondText))

  return firstPart to secondPart
}

/**
 * Tries to split an element with multiple children at a child boundary,
 * so that as many children as possible fit within [availableHeightPx].
 */
private fun trySplitBetweenChildren(
  element: Node.Element,
  availableWidthPx: Int,
  availableHeightPx: Int,
  textMeasurer: TextMeasurer,
  density: Density,
  fontFamilyMap: Map<String, FontFamily>
): Pair<Node, Node>? {
  val style = element.style
  val hPx = style.horizontalSpacingPx(density)
  val contentWidth = (availableWidthPx - hPx).coerceAtLeast(1)
  val vPx = style.verticalSpacingPx(density)
  val contentAvailable = (availableHeightPx - vPx).coerceAtLeast(0)

  var accumulated = 0
  var splitIdx = -1
  for ((idx, child) in element.children.withIndex()) {
    val h = measureNodeHeight(child, contentWidth, textMeasurer, density, fontFamilyMap)
    if (accumulated + h > contentAvailable) break
    accumulated += h
    splitIdx = idx
  }
  if (splitIdx < 0) return null
  val firstChildren = element.children.subList(0, splitIdx + 1)
  val secondChildren = element.children.subList(splitIdx + 1, element.children.size)
  if (secondChildren.isEmpty()) return null

  return element.copy(children = firstChildren) to element.copy(children = secondChildren)
}

// ─────────────────────────── Node.Text splitting ─────────────────────────────

/**
 * Splits [text] at character offset [offset] in [Node.Text.raw].
 * Span ranges are adjusted: spans fully before the split stay in first part,
 * spans fully after the split (offset-shifted) stay in second part,
 * spans crossing the boundary are clipped to their respective side.
 * If [addHyphen] is true a "-" is appended to the first part.
 */
private fun splitNodeText(text: Node.Text, offset: Int, addHyphen: Boolean): Pair<Node.Text, Node.Text> {
  val safeOffset = offset.coerceIn(0, text.raw.length)

  val firstRaw = text.raw.substring(0, safeOffset).trimEnd() + if (addHyphen) "-" else ""
  val trimmedSecondRaw = text.raw.substring(safeOffset).trimStart()
  // How many chars were stripped from the beginning of second part
  val secondStartInOriginal = safeOffset + (text.raw.length - safeOffset - trimmedSecondRaw.length)

  val firstSpans = text.spans.mapNotNull { span ->
    when {
      span.end <= safeOffset -> span
      span.start >= safeOffset -> null
      else -> span.copy(end = safeOffset.coerceAtMost(firstRaw.length))
    }?.validOrNull(firstRaw.length)
  }

  val secondSpans = text.spans.mapNotNull { span ->
    val newStart = (span.start - secondStartInOriginal).coerceAtLeast(0)
    val newEnd = (span.end - secondStartInOriginal).coerceAtMost(trimmedSecondRaw.length)
    if (newEnd <= newStart || newEnd <= 0 || newStart >= trimmedSecondRaw.length) null
    else span.copy(start = newStart, end = newEnd)
  }

  return Node.Text(firstRaw, firstSpans) to Node.Text(trimmedSecondRaw, secondSpans)
}

private fun Span.validOrNull(maxLen: Int): Span? =
  takeIf { start < end && start < maxLen && end > 0 }
    ?.let { copy(end = end.coerceAtMost(maxLen)) }

// ─────────────────────────── Helpers ─────────────────────────────────────────

private fun Node.isEmpty(): Boolean = when (this) {
  is Node.Text -> raw.isBlank()
  is Node.Element -> children.isEmpty()
  is Node.Image -> data.isEmpty()
  is Node.HorizontalRule -> false
  is Node.BulletList -> items.isEmpty()
  is Node.Table -> rows.isEmpty()
}

private fun Node.pageBreakBefore(): PageBreak? = when (this) {
  is Node.Element -> style?.pageBreakBefore
  else -> null
}

/** Total horizontal spacing (margins + padding) in pixels. */
private fun Style?.horizontalSpacingPx(density: Density): Int = if (this == null) 0 else with(density) {
  (
    (
      marginStart.toDpOrZero(density) + marginEnd.toDpOrZero(density) +
        paddingStart.toDpOrZero(density) + paddingEnd.toDpOrZero(density)
      ).toPx()
    ).toInt()
}

/** Total vertical spacing (margins + padding) in pixels. */
private fun Style?.verticalSpacingPx(density: Density): Int =
  marginVerticalPx(density) + paddingVerticalPx(density)

/** Vertical margins only (top + bottom) in pixels. */
private fun Style?.marginVerticalPx(density: Density): Int = if (this == null) 0 else with(density) {
  ((marginTop.toDpOrZero(density) + marginBottom.toDpOrZero(density)).toPx()).toInt()
}

/** Vertical padding only (top + bottom) in pixels. */
private fun Style?.paddingVerticalPx(density: Density): Int = if (this == null) 0 else with(density) {
  ((paddingTop.toDpOrZero(density) + paddingBottom.toDpOrZero(density)).toPx()).toInt()
}

/** Returns true if the style has visual box properties (background or border) that must be preserved. */
private fun Style?.hasVisualBox(): Boolean =
  this != null && (background != null || border != null)
