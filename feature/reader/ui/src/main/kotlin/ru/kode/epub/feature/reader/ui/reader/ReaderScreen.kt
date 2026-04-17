package ru.kode.epub.feature.reader.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.kode.epub.core.domain.mapDistinctNotNullChanges
import ru.kode.epub.core.ui.screen.AppScreen
import ru.kode.epub.core.uikit.component.CircularLoaderWithOverlay
import ru.kode.epub.core.uikit.theme.AppTheme
import ru.kode.epub.feature.reader.domain.entity.PageScrollMode
import ru.kode.epub.feature.reader.ui.bottombar.BottomBarStateRestoreEffect
import ru.kode.epub.feature.reader.ui.reader.component.ColumnParams
import ru.kode.epub.feature.reader.ui.reader.component.PageContent
import ru.kode.epub.feature.reader.ui.reader.component.ReaderBottomBar
import ru.kode.epub.feature.reader.ui.reader.component.ReaderTopBar
import ru.kode.epub.feature.reader.ui.reader.component.rememberColumnParams
import ru.kode.epub.feature.reader.ui.reader.component.rememberReaderInsets

@Suppress("CyclomaticComplexMethod")
@Composable
fun ReaderScreen(
  viewModel: ReaderViewModel
) = AppScreen(viewModel) { state ->
  val fontFamily = rememberEpubFontFamily(state.fontFiles)

  BottomBarStateRestoreEffect(visible = false)

  CompositionLocalProvider(LocalEpubFontFamily provides fontFamily) {
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .background(AppTheme.colors.surfaceBackground)
    ) {

      val insets = rememberReaderInsets()

      val params = rememberColumnParams(
        elements = state.elements,
        constraints = constraints,
        columnMode = state.columnMode,
        tocAnchors = state.tocAnchors,
        insets = insets,
      )

      val pagerState = rememberPagerState(pageCount = { params.screenPageCount.coerceAtLeast(1) })

      if (state.scrollMode != null && state.columnMode != null && params.calculatorPages.isNotEmpty()) {
        when (state.scrollMode) {
          PageScrollMode.Horizontal -> HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageContent = { scrollIndex ->
              PageContent(
                state = pagerState,
                scrollMode = state.scrollMode,
                screenPageIndex = scrollIndex,
                params = params,
                insets = insets,
                onToggleTopBar = viewModel::toggleTopBar,
              )
            }
          )

          PageScrollMode.Vertical -> VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageContent = { scrollIndex ->
              PageContent(
                state = pagerState,
                scrollMode = state.scrollMode,
                screenPageIndex = scrollIndex,
                params = params,
                insets = insets,
                onToggleTopBar = viewModel::toggleTopBar,
              )
            }
          )
        }


        ReaderTopBar(
          visible = state.isTopBarVisible,
          showTocButton = state.toc.isNotEmpty(),
          bookInfo = state.bookInfo,
          onBackClick = viewModel::navigateBack,
          onShowBookInfoClick = viewModel::showBookInfo,
          onShowTocClick = viewModel::showToc,
        )

        ReaderBottomBar(
          visible = state.isTopBarVisible && params.calculatorPages.isNotEmpty(),
          pagerState = pagerState,
          params = params,
          onScrollToElement = viewModel::scrollToElement,
          insets = insets,
          modifier = Modifier.align(Alignment.BottomCenter),
        )

        ScrollEffects(
          pagerState = pagerState,
          scope = viewModel.viewModelScope,
          params = params,
          currentElementIndex = state.currentElementIndex,
          scrollToElementIndex = state.scrollToElementIndex,
          onScrollHandled = viewModel::onScrollHandled,
          onScroll = viewModel::onScroll,
        )
      }
      AnimatedVisibility(
        visible = state.loading,
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        CircularLoaderWithOverlay(modifier = Modifier.fillMaxSize())
      }
    }
  }
}

@Composable
private fun ScrollEffects(
  pagerState: PagerState,
  params: ColumnParams,
  currentElementIndex: Int?,
  scrollToElementIndex: Int?,
  onScrollHandled: () -> Unit,
  onScroll: (String, Int) -> Unit,
  scope: CoroutineScope,
) {
  // TOC / chapter navigation scroll
  LaunchedEffect(scrollToElementIndex, params.calculatorPages) {
    scrollToElementIndex?.let { targetIndex ->
      val calcIdx = params.calculatorPages.indexOfFirst { page -> page.any { it.index == targetIndex } }
      if (calcIdx >= 0) pagerState.scrollToPage(calcIdx / params.columnCount)
      onScrollHandled()
    }
  }

  // Restore position after config change (pagerState resets to 0)
  LaunchedEffect(params.calculatorPages, params.columnCount) {
    if (params.calculatorPages.isNotEmpty() && pagerState.currentPage == 0) {
      currentElementIndex?.let { savedIndex ->
        val calcIdx = params.calculatorPages.indexOfFirst { page -> page.any { it.index == savedIndex } }
        if (calcIdx > 0) pagerState.scrollToPage(calcIdx / params.columnCount)
      }
    }
  }

  // Save position on page change
  LaunchedEffect(Unit) {
    snapshotFlow { pagerState.layoutInfo }
      .mapDistinctNotNullChanges { info ->
        info.visiblePagesInfo.firstOrNull()?.index?.let { idx ->
          params.calculatorPages.getOrNull(idx)?.firstOrNull()
        }
      }
      .flowOn(Dispatchers.Default)
      .onEach { element -> onScroll(element.key, element.index) }
      .launchIn(scope)
  }
}

internal val ColumnGap = 32.dp
