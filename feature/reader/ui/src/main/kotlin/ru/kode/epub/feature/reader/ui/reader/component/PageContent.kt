package ru.kode.epub.feature.reader.ui.reader.component

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.launch
import ru.kode.epub.feature.reader.domain.entity.PageScrollMode
import ru.kode.epub.feature.reader.ui.reader.ColumnGap
import ru.kode.epub.feature.reader.ui.reader.ContentItem

@Composable
internal fun PagerScope.PageContent(
  state: PagerState,
  scrollMode: PageScrollMode,
  screenPageIndex: Int,
  params: ColumnParams,
  insets: ReaderInsets,
  onToggleTopBar: () -> Unit,
) {
  val scope = rememberCoroutineScope()
  Row(
    modifier = Modifier
      .fillMaxSize()
      .pointerInput(scrollMode) {
        detectTapGestures { offset ->
          val targetPage = when (scrollMode) {
            PageScrollMode.Horizontal -> when {
              offset.x < size.width * 0.2f ->
                (state.currentPage - 1).takeIf { it >= 0 }

              offset.x > size.width * 0.8f ->
                (state.currentPage + 1).takeIf { it < state.pageCount }

              else -> null
            }

            PageScrollMode.Vertical -> when {
              offset.y < size.height * 0.2f ->
                (state.currentPage - 1).takeIf { it >= 0 }

              offset.y > size.height * 0.8f ->
                (state.currentPage + 1).takeIf { it < state.pageCount }

              else -> null
            }
          }
          if (targetPage != null) {
            scope.launch { state.animateScrollToPage(targetPage) }
          } else {
            onToggleTopBar()
          }
        }
      }
      .padding(
        top = insets.statusBarPadding,
        bottom = insets.bottomPadding,
        start = insets.sideStart,
        end = insets.sideEnd
      )
  ) {
    repeat(params.columnCount) { colIdx ->
      if (colIdx > 0) Spacer(Modifier.width(ColumnGap))
      Column(modifier = Modifier.weight(1f)) {
        val calcIdx = screenPageIndex * params.columnCount + colIdx
        params.calculatorPages.getOrElse(calcIdx) { emptyList() }.forEach { indexed ->
          ContentItem(indexed.element)
        }
      }
    }
  }
}
