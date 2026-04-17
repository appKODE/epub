package ru.kode.epub.feature.reader.ui.reader.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import ru.kode.epub.core.domain.entity.ScreenOrientation
import ru.kode.epub.core.ui.compose.LocalScreenOrientation
import ru.kode.epub.feature.reader.domain.entity.ColumnMode
import ru.kode.epub.feature.reader.ui.reader.ColumnGap
import ru.kode.epub.feature.reader.ui.reader.IndexedElement
import ru.kode.epub.feature.reader.ui.reader.TocAnchor
import ru.kode.epub.feature.reader.ui.reader.rememberPageBreaks

@Immutable
internal data class ColumnParams(
  val columnCount: Int = 0,
  val screenPageCount: Int = 0,
  val tocAnchorPages: List<Pair<TocAnchor, Int>> = emptyList(),
  val calculatorPages: List<List<IndexedElement>> = emptyList(),
)

@Composable
internal fun rememberColumnParams(
  elements: List<IndexedElement>,
  tocAnchors: List<TocAnchor>,
  constraints: Constraints,
  columnMode: ColumnMode?,
  insets: ReaderInsets,
): ColumnParams {
  val orientation = LocalScreenOrientation.current
  val density = LocalDensity.current
  val columnCount = if (
    columnMode == ColumnMode.Double && orientation == ScreenOrientation.Landscape
  ) 2 else 1

  val horizontalPaddingPx = with(density) { 32.dp.roundToPx() }
  val columnGapPx = with(density) { if (columnCount > 1) ColumnGap.roundToPx() else 0 }
  val contentHeightPx = with(density) {
    (constraints.maxHeight - insets.statusBarPadding.roundToPx() - insets.bottomPadding.roundToPx())
      .coerceAtLeast(0)
  }
  val sideHorizontalPx = with(density) { insets.sideStart.roundToPx() + insets.sideEnd.roundToPx() }

  // Width available for all columns (subtract item padding and side system insets)
  val totalContentWidthPx = (constraints.maxWidth - horizontalPaddingPx - sideHorizontalPx).coerceAtLeast(0)
  val columnWidthPx = ((totalContentWidthPx - columnGapPx) / columnCount).coerceAtLeast(0)

  // Each calculator page = one column
  val calculatorPages = rememberPageBreaks(
    elements = elements,
    contentHeightPx = contentHeightPx,
    contentWidthPx = columnWidthPx
  )
  val screenPageCount = (calculatorPages.size + columnCount - 1) / columnCount


  // Map each TOC anchor to its screen page index
  val tocAnchorPages = remember(tocAnchors, calculatorPages, columnCount) {
    tocAnchors.mapNotNull { anchor ->
      val calcIdx = calculatorPages.indexOfFirst { page -> page.any { it.index == anchor.elementIndex } }
      if (calcIdx >= 0) anchor to (calcIdx / columnCount) else null
    }
  }
  return remember(constraints, columnCount, screenPageCount, calculatorPages, tocAnchorPages) {
    ColumnParams(
      columnCount = columnCount,
      screenPageCount = screenPageCount,
      calculatorPages = calculatorPages,
      tocAnchorPages = tocAnchorPages,
    )
  }
}
