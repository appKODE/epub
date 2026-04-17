package ru.kode.epub.feature.reader.ui.reader

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import ru.kode.epub.core.uikit.theme.AppTheme
import ru.kode.epub.lib.entity.ContentElement
import ru.kode.epub.lib.entity.EpubTextAlign
import ru.kode.epub.lib.entity.StyledText
import ru.kode.epub.lib.entity.TextSpan

@Composable
fun rememberPageBreaks(
  elements: List<IndexedElement>,
  contentHeightPx: Int,
  contentWidthPx: Int
): List<List<IndexedElement>> {
  val textMeasurer = rememberTextMeasurer()
  val density = LocalDensity.current
  val fontFamily = LocalEpubFontFamily.current

  val h2 = AppTheme.typography.headline2
  val h3 = AppTheme.typography.headline3
  val h4 = AppTheme.typography.headline4
  val h5 = AppTheme.typography.headline5
  val subhead = AppTheme.typography.subhead1
  val body = AppTheme.typography.body1

  return remember(elements, contentHeightPx, contentWidthPx, fontFamily) {
    if (contentHeightPx <= 0 || contentWidthPx <= 0) return@remember emptyList()
    buildPages(
      elements = elements,
      pageHeightPx = contentHeightPx,
      contentWidthPx = contentWidthPx,
      fontFamily = fontFamily,
      headlineStyles = mapOf(1 to h2, 2 to h3, 3 to h4, 4 to h5),
      subheadStyle = subhead,
      bodyStyle = body,
      textMeasurer = textMeasurer,
      density = density
    )
  }
}

// ─────────────────────────── Result types ───────────────────────────────────

private sealed interface SplitElementResult {
  /** Element was split: put [first] on current page, queue [continuation] for the next. */
  data class Split(val first: IndexedElement, val continuation: IndexedElement) : SplitElementResult

  /** TextMeasurer overestimated height — element actually fits on the current page as-is. */
  data object FitsAfterAll : SplitElementResult
}

private sealed interface SplitTextResult {
  data class Split(val first: StyledText, val second: StyledText) : SplitTextResult
  data object AllFit : SplitTextResult
  data object NothingFits : SplitTextResult
}

// ─────────────────────────── Core algorithm ─────────────────────────────────
@Suppress("NestedBlockDepth")
private fun buildPages(
  elements: List<IndexedElement>,
  pageHeightPx: Int,
  contentWidthPx: Int,
  fontFamily: FontFamily,
  headlineStyles: Map<Int, TextStyle>,
  subheadStyle: TextStyle,
  bodyStyle: TextStyle,
  textMeasurer: TextMeasurer,
  density: Density
): List<List<IndexedElement>> {
  val pages = mutableListOf<MutableList<IndexedElement>>()
  var currentPage = mutableListOf<IndexedElement>()
  var usedHeightPx = 0

  val queue = ArrayDeque<IndexedElement>()
  queue.addAll(elements)

  while (queue.isNotEmpty()) {
    val indexed = queue.removeFirst()

    if (indexed.isChapterStart && currentPage.isNotEmpty()) {
      pages.add(currentPage)
      currentPage = mutableListOf()
      usedHeightPx = 0
    }

    val elementHeight = measureElementHeight(
      element = indexed.element,
      contentWidthPx = contentWidthPx,
      fontFamily = fontFamily,
      headlineStyles = headlineStyles,
      subheadStyle = subheadStyle,
      bodyStyle = bodyStyle,
      textMeasurer = textMeasurer,
      density = density
    )
    val available = pageHeightPx - usedHeightPx

    if (elementHeight <= available) {
      currentPage.add(indexed)
      usedHeightPx += elementHeight
    } else {
      when (
        val result = trySplitElement(
          indexed = indexed,
          availableHeightPx = available,
          contentWidthPx = contentWidthPx,
          fontFamily = fontFamily,
          bodyStyle = bodyStyle,
          textMeasurer = textMeasurer,
          density = density
        )
      ) {
        is SplitElementResult.Split -> {
          currentPage.add(result.first)
          pages.add(currentPage)
          currentPage = mutableListOf()
          usedHeightPx = 0
          queue.addFirst(result.continuation)
        }
        SplitElementResult.FitsAfterAll -> {
          // TextMeasurer overestimated — element actually fits
          currentPage.add(indexed)
          usedHeightPx += elementHeight
        }
        null -> when {
          currentPage.isEmpty() -> {
            // Unsplittable element larger than a full page — add anyway
            currentPage.add(indexed)
            usedHeightPx += elementHeight
          }
          else -> {
            // Nothing fits on current page — close and retry
            pages.add(currentPage)
            currentPage = mutableListOf()
            usedHeightPx = 0
            queue.addFirst(indexed)
          }
        }
      }
    }
  }

  if (currentPage.isNotEmpty()) pages.add(currentPage)
  return pages
}

// ─────────────────────────── Splitting ──────────────────────────────────────

private fun trySplitElement(
  indexed: IndexedElement,
  availableHeightPx: Int,
  contentWidthPx: Int,
  fontFamily: FontFamily,
  bodyStyle: TextStyle,
  textMeasurer: TextMeasurer,
  density: Density
): SplitElementResult? = with(density) {
  when (val element = indexed.element) {
    is ContentElement.Paragraph -> {
      val css = element.styles
      val style = bodyStyle.copy(
        fontFamily = fontFamily,
        lineHeight = 26.sp,
        textIndent = TextIndent(firstLine = (css.textIndentEm ?: 1f) * 16.sp),
        textAlign = css.textAlign?.toComposeAlign() ?: TextAlign.Justify,
        hyphens = Hyphens.Auto,
        lineBreak = LineBreak.Paragraph,
        fontStyle = css.italic?.toFontStyle() ?: FontStyle.Normal,
        fontWeight = css.bold?.toFontWeight() ?: FontWeight.Normal
      )
      val paddingStartPx = css.paddingStart.toDp(default = 16.dp).roundToPx()
      val paddingEndPx = css.paddingEnd.toDp(default = 16.dp).roundToPx()
      val paddingTopPx = css.paddingTop.toDp(default = 0.dp).roundToPx()
      val paddingBottomPx = css.paddingBottom.toDp(default = 0.dp).roundToPx()
      val textWidthPx = (contentWidthPx - paddingStartPx - paddingEndPx).coerceAtLeast(0)
      val marginTopPx = css.marginTop.toDp(default = 0.dp).roundToPx()
      val marginBottomPx = css.marginBottom.toDp(default = PARAGRAPH_MARGIN_BOTTOM_DEFAULT).roundToPx()
      val textAvailable = availableHeightPx - marginTopPx - marginBottomPx - paddingTopPx - paddingBottomPx

      when (
        val textResult = splitTextToHeight(
          text = element.text,
          style = style,
          widthPx = textWidthPx,
          availableHeightPx = textAvailable,
          textMeasurer = textMeasurer
        )
      ) {
        is SplitTextResult.Split -> SplitElementResult.Split(
          first = indexed.copy(element = element.copy(text = textResult.first)),
          continuation = indexed.copy(
            key = "${indexed.key}-c",
            element = element.copy(
              text = textResult.second.trimLeadingWhitespace(),
              styles = css.copy(textIndentEm = 0f, marginTop = null)
            )
          )
        )
        SplitTextResult.AllFit -> SplitElementResult.FitsAfterAll
        SplitTextResult.NothingFits -> null
      }
    }

    is ContentElement.Quote -> {
      val css = element.styles
      val style = bodyStyle.copy(
        fontFamily = fontFamily,
        lineHeight = 26.sp,
        fontStyle = css.italic?.toFontStyle() ?: FontStyle.Normal
      )
      val marginTopPx = css.marginTop.toDp(default = QUOTE_MARGIN_TOP_DEFAULT).roundToPx()
      val marginBottomPx = css.marginBottom.toDp(default = QUOTE_MARGIN_BOTTOM_DEFAULT).roundToPx()
      val marginStartPx = css.marginStart.toDp(default = QUOTE_MARGIN_START_DEFAULT).roundToPx()
      val quoteWidthPx = (contentWidthPx - marginStartPx).coerceAtLeast(contentWidthPx / 2)
      val textAvailable = availableHeightPx - marginTopPx - marginBottomPx

      when (
        val textResult = splitTextToHeight(
          text = element.text,
          style = style,
          widthPx = quoteWidthPx,
          availableHeightPx = textAvailable,
          textMeasurer = textMeasurer
        )
      ) {
        is SplitTextResult.Split -> SplitElementResult.Split(
          first = indexed.copy(element = element.copy(text = textResult.first)),
          continuation = indexed.copy(
            key = "${indexed.key}-c",
            element = element.copy(
              text = textResult.second.trimLeadingWhitespace(),
              styles = css.copy(marginTop = null)
            )
          )
        )
        SplitTextResult.AllFit -> SplitElementResult.FitsAfterAll
        SplitTextResult.NothingFits -> null
      }
    }

    // Headings stay whole — move to next page if they don't fit
    is ContentElement.Heading -> null
    is ContentElement.EpubImage -> null
  }
}

private fun splitTextToHeight(
  text: StyledText,
  style: TextStyle,
  widthPx: Int,
  availableHeightPx: Int,
  textMeasurer: TextMeasurer
): SplitTextResult {
  if (availableHeightPx <= 0 || text.text.isBlank()) return SplitTextResult.NothingFits

  val measured = textMeasurer.measure(
    text = text.toAnnotatedString(),
    style = style,
    constraints = Constraints.fixedWidth(widthPx)
  )

  var lastFittingLine = -1
  for (line in 0 until measured.lineCount) {
    if (measured.getLineBottom(line).toInt() <= availableHeightPx) {
      lastFittingLine = line
    } else {
      break
    }
  }

  if (lastFittingLine < 0) return SplitTextResult.NothingFits
  if (lastFittingLine >= measured.lineCount - 1) return SplitTextResult.AllFit

  val splitIndex = measured.getLineEnd(lastFittingLine, visibleEnd = true)
    .coerceIn(1, text.text.length - 1)

  // Hyphens.Auto adds a hyphen visually at the line break but does not insert it into the text.
  // When the first part is rendered alone the word is no longer broken, so no hyphen appears.
  // Fix: if the split landed in the middle of a word, append an explicit hyphen.
  val needsHyphen = text.text.getOrNull(splitIndex - 1)?.isLetter() == true &&
    text.text.getOrNull(splitIndex)?.isLetter() == true

  val (first, second) = text.splitAt(splitIndex)
  val firstPart = if (needsHyphen) first.copy(text = first.text + "-") else first
  return SplitTextResult.Split(firstPart, second)
}

// ─────────────────────────── Height measurement ─────────────────────────────

private fun measureElementHeight(
  element: ContentElement,
  contentWidthPx: Int,
  fontFamily: FontFamily,
  headlineStyles: Map<Int, TextStyle>,
  subheadStyle: TextStyle,
  bodyStyle: TextStyle,
  textMeasurer: TextMeasurer,
  density: Density
): Int = with(density) {
  when (element) {
    is ContentElement.Heading -> {
      val css = element.styles
      val baseStyle = headlineStyles[element.level] ?: subheadStyle
      val textWidthPx = (contentWidthPx - 2 * HEADING_HORIZONTAL_PADDING_DEFAULT.roundToPx()).coerceAtLeast(0)
      val measured = textMeasurer.measure(
        text = element.text.toAnnotatedString(),
        style = baseStyle.copy(
          fontFamily = fontFamily,
          fontWeight = css.bold?.toFontWeight() ?: baseStyle.fontWeight,
          fontStyle = css.italic?.toFontStyle() ?: baseStyle.fontStyle
        ),
        constraints = Constraints.fixedWidth(textWidthPx)
      )
      measured.size.height +
        css.marginTop.toDp(default = HEADING_MARGIN_TOP_DEFAULT).roundToPx() +
        css.marginBottom.toDp(default = HEADING_MARGIN_BOTTOM_DEFAULT).roundToPx()
    }

    is ContentElement.Paragraph -> {
      val css = element.styles
      val paddingStartPx = css.paddingStart.toDp(default = 16.dp).roundToPx()
      val paddingEndPx = css.paddingEnd.toDp(default = 16.dp).roundToPx()
      val paddingTopPx = css.paddingTop.toDp(default = 0.dp).roundToPx()
      val paddingBottomPx = css.paddingBottom.toDp(default = 0.dp).roundToPx()
      val textWidthPx = (contentWidthPx - paddingStartPx - paddingEndPx).coerceAtLeast(0)
      val measured = textMeasurer.measure(
        text = element.text.toAnnotatedString(),
        style = bodyStyle.copy(
          fontFamily = fontFamily,
          lineHeight = 26.sp,
          textIndent = TextIndent(firstLine = (css.textIndentEm ?: 1f) * 16.sp),
          textAlign = css.textAlign?.toComposeAlign() ?: TextAlign.Justify,
          hyphens = Hyphens.Auto,
          lineBreak = LineBreak.Paragraph,
          fontStyle = css.italic?.toFontStyle() ?: FontStyle.Normal,
          fontWeight = css.bold?.toFontWeight() ?: FontWeight.Normal
        ),
        constraints = Constraints.fixedWidth(textWidthPx)
      )
      val contentHeight = measured.size.height +
        css.marginTop.toDp(default = 0.dp).roundToPx() +
        css.marginBottom.toDp(default = PARAGRAPH_MARGIN_BOTTOM_DEFAULT).roundToPx() +
        paddingTopPx + paddingBottomPx
      val minHeightPx = css.minHeight.toDp(default = 0.dp).roundToPx()
      maxOf(contentHeight, minHeightPx)
    }

    is ContentElement.Quote -> {
      val css = element.styles
      val marginStartPx = css.marginStart.toDp(default = QUOTE_MARGIN_START_DEFAULT).roundToPx()
      val quoteWidthPx = (contentWidthPx - marginStartPx).coerceAtLeast(contentWidthPx / 2)
      val measured = textMeasurer.measure(
        text = element.text.toAnnotatedString(),
        style = bodyStyle.copy(
          fontFamily = fontFamily,
          lineHeight = 26.sp,
          fontStyle = css.italic?.toFontStyle() ?: FontStyle.Normal
        ),
        constraints = Constraints.fixedWidth(quoteWidthPx)
      )
      measured.size.height +
        css.marginTop.toDp(default = QUOTE_MARGIN_TOP_DEFAULT).roundToPx() +
        css.marginBottom.toDp(default = QUOTE_MARGIN_BOTTOM_DEFAULT).roundToPx()
    }

    is ContentElement.EpubImage -> {
      val bitmap = BitmapFactory.decodeByteArray(element.data, 0, element.data.size)
      val verticalPaddingPx = (IMAGE_VERTICAL_PADDING_DEFAULT * 2).roundToPx()
      if (bitmap != null) {
        val heightPx = bitmap.height.toFloat()
        val aspectRatio = bitmap.width.toFloat() / heightPx
        minOf((contentWidthPx / aspectRatio).toInt(), heightPx.toInt()) + verticalPaddingPx
      } else {
        verticalPaddingPx
      }
    }
  }
}

// ─────────────────────────── StyledText helpers ─────────────────────────────

private fun StyledText.splitAt(charIndex: Int): Pair<StyledText, StyledText> {
  val idx = charIndex.coerceIn(0, text.length)
  val firstSpans = mutableListOf<TextSpan>()
  val secondSpans = mutableListOf<TextSpan>()
  for (span in spans) {
    when {
      span.end <= idx -> firstSpans.add(span)
      span.start >= idx -> secondSpans.add(span.copy(start = span.start - idx, end = span.end - idx))
      else -> {
        // span.start < idx is guaranteed by the when conditions above
        firstSpans.add(span.copy(end = idx))
        secondSpans.add(span.copy(start = 0, end = span.end - idx))
      }
    }
  }
  return StyledText(text.substring(0, idx), firstSpans) to
    StyledText(text.substring(idx), secondSpans)
}

private fun StyledText.trimLeadingWhitespace(): StyledText {
  val trimmed = text.trimStart()
  if (trimmed.length == text.length) return this
  val offset = text.length - trimmed.length
  return splitAt(offset).second
}

// ─────────────────────────── Private helpers ────────────────────────────────

private fun Boolean.toFontWeight() = if (this) FontWeight.Bold else FontWeight.Normal
private fun Boolean.toFontStyle() = if (this) FontStyle.Italic else FontStyle.Normal
private fun EpubTextAlign.toComposeAlign(): TextAlign = when (this) {
  EpubTextAlign.Left -> TextAlign.Left
  EpubTextAlign.Right -> TextAlign.Right
  EpubTextAlign.Center -> TextAlign.Center
  EpubTextAlign.Justify -> TextAlign.Justify
}
