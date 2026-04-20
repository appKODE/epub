package ru.kode.epub.feature.reader.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.kode.epub.core.domain.entity.ScreenOrientation
import ru.kode.epub.core.domain.mapDistinctNotNullChanges
import ru.kode.epub.core.ui.compose.LocalScreenOrientation
import ru.kode.epub.core.ui.screen.AppScreen
import ru.kode.epub.core.uikit.component.CircularLoaderWithOverlay
import ru.kode.epub.core.uikit.theme.AppTheme
import ru.kode.epub.feature.reader.domain.entity.ColumnMode
import ru.kode.epub.feature.reader.domain.entity.PageScrollMode
import ru.kode.epub.feature.reader.domain.entity.TurnPageMode
import ru.kode.epub.feature.reader.ui.bottombar.BottomBarStateRestoreEffect
import ru.kode.epub.feature.reader.ui.reader.component.PageContent
import ru.kode.epub.feature.reader.ui.reader.component.ReaderBottomBar
import ru.kode.epub.feature.reader.ui.reader.component.ReaderTopBar
import ru.kode.epub.lib.entity.Book

@Composable
fun ReaderScreen(viewModel: ReaderViewModel) = AppScreen(viewModel) { state ->
  BottomBarStateRestoreEffect(visible = false)

  state.book?.let {
    BookContent(
      book = it,
      fontFamilyMap = state.fontFamilyMap,
      scrollMode = state.scrollMode,
      columnMode = state.columnMode,
      turnPageMode = state.turnPageMode,
      barsVisible = state.barsVisible,
      scrollToElementIndex = state.scrollToElementIndex,
      currentElementIndex = state.currentElementIndex,
      onBack = viewModel::navigateBack,
      onShowBookInfo = viewModel::showBookInfo,
      onShowToc = viewModel::showToc,
      onScrollHandled = viewModel::onScrollHandled,
      onToggleBars = viewModel::toggleBars,
      onPageChanged = viewModel::onPageChanged
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

@Composable
private fun BookContent(
  book: Book,
  fontFamilyMap: Map<String, FontFamily>,
  scrollMode: PageScrollMode?,
  columnMode: ColumnMode?,
  turnPageMode: TurnPageMode?,
  barsVisible: Boolean,
  scrollToElementIndex: Int?,
  currentElementIndex: Int?,
  onBack: () -> Unit,
  onShowBookInfo: () -> Unit,
  onShowToc: () -> Unit,
  onScrollHandled: () -> Unit,
  onToggleBars: () -> Unit,
  onPageChanged: (chapterIndex: Int, relativeIndex: Int, globalIndex: Int) -> Unit
) {
  val density = LocalDensity.current
  val textMeasurer = rememberTextMeasurer()
  val orientation = LocalScreenOrientation.current
  val coroutineScope = rememberCoroutineScope()
  val layoutDirection = LocalLayoutDirection.current

  val columnCount = if (columnMode == ColumnMode.Double && orientation == ScreenOrientation.Landscape) 2 else 1

  var pages by remember { mutableStateOf<List<Page>>(emptyList()) }
  val screenPageCount = (pages.size + columnCount - 1) / columnCount
  val pagerState = rememberPagerState(pageCount = { screenPageCount.coerceAtLeast(1) })

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .background(AppTheme.colors.surfaceBackground)
  ) {
    val sideInsetPaddings = WindowInsets.safeDrawing.asPaddingValues()
    val sideTop = sideInsetPaddings.calculateTopPadding()
    val sideBottom = sideInsetPaddings.calculateBottomPadding()
    val sideStart = sideInsetPaddings.calculateStartPadding(layoutDirection)
    val sideEnd = sideInsetPaddings.calculateEndPadding(layoutDirection)

    val effectiveWidth = maxWidth - sideStart - sideEnd
    val widthPx = with(density) {
      (effectiveWidth / columnCount - PAGE_HORIZONTAL_PADDING * 2).roundToPx().coerceAtLeast(1)
    }
    val heightPx = with(density) {
      (maxHeight - sideTop - sideBottom - PAGE_VERTICAL_PADDING * 2).roundToPx().coerceAtLeast(1)
    }

    LaunchedEffect(book, widthPx, heightPx) {
      if (widthPx > 0 && heightPx > 0) {
        pages = withContext(Dispatchers.Default) {
          calculatePages(
            book = book,
            pageWidthPx = widthPx,
            pageHeightPx = heightPx,
            textMeasurer = textMeasurer,
            density = density,
            fontFamilyMap = fontFamilyMap
          )
        }
      }
    }

    val tapModifier = Modifier.pointerInput(scrollMode, turnPageMode) {
      detectTapGestures { offset ->
        val targetPage = if (turnPageMode == TurnPageMode.TapAndGesture) {
          when (scrollMode) {
            PageScrollMode.Horizontal -> when {
              offset.x < size.width * 0.2f -> (pagerState.currentPage - 1).takeIf { it >= 0 }
              offset.x > size.width * 0.8f -> (pagerState.currentPage + 1).takeIf { it < pagerState.pageCount }
              else -> null
            }
            else -> when {
              offset.y < size.height * 0.2f -> (pagerState.currentPage - 1).takeIf { it >= 0 }
              offset.y > size.height * 0.8f -> (pagerState.currentPage + 1).takeIf { it < pagerState.pageCount }
              else -> null
            }
          }
        } else null
        if (targetPage != null) {
          coroutineScope.launch { pagerState.animateScrollToPage(targetPage) }
        } else {
          onToggleBars()
        }
      }
    }

    val pagerModifier = Modifier
      .fillMaxSize()
      .padding(start = sideStart, end = sideEnd)
      .then(tapModifier)

    if (scrollMode == PageScrollMode.Horizontal) {
      HorizontalPager(state = pagerState, modifier = pagerModifier) { index ->
        PageContent(
          modifier = Modifier.fillMaxSize(),
          pages = pages,
          columnCount = columnCount,
          screenPageIndex = index,
          fontFamilyMap = fontFamilyMap,
          paddingValues = PaddingValues(top = sideTop, bottom = sideBottom)
        )
      }
    } else {
      VerticalPager(state = pagerState, modifier = pagerModifier) { index ->
        PageContent(
          modifier = Modifier.fillMaxSize(),
          pages = pages,
          columnCount = columnCount,
          screenPageIndex = index,
          fontFamilyMap = fontFamilyMap,
          paddingValues = PaddingValues(top = sideTop, bottom = sideBottom)
        )
      }
    }

    ReaderTopBar(
      visible = barsVisible,
      metadata = book.metadata,
      onBackClick = onBack,
      onShowBookInfoClick = onShowBookInfo,
      onShowBookTocClick = onShowToc,
      modifier = Modifier.align(Alignment.TopStart).fillMaxWidth()
    )

    ReaderBottomBar(
      visible = barsVisible,
      pagerState = pagerState,
      toc = book.toc,
      pages = pages,
      columnCount = columnCount,
      totalPages = screenPageCount,
      onPageSelected = { page -> coroutineScope.launch { pagerState.scrollToPage(page) } },
      modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
    )

    ScrollEffects(
      pagerState = pagerState,
      pages = pages,
      columnCount = columnCount,
      currentElementIndex = currentElementIndex,
      scrollToElementIndex = scrollToElementIndex,
      onScrollHandled = onScrollHandled,
      onPageChanged = onPageChanged
    )

    AnimatedVisibility(
      visible = pages.isEmpty(),
      enter = fadeIn(),
      exit = fadeOut()
    ) {
      CircularLoaderWithOverlay(modifier = Modifier.fillMaxSize())
    }
  }
}

@Composable
private fun ScrollEffects(
  pagerState: PagerState,
  pages: List<Page>,
  columnCount: Int,
  currentElementIndex: Int?,
  scrollToElementIndex: Int?,
  onScrollHandled: () -> Unit,
  onPageChanged: (chapterIndex: Int, relativeIndex: Int, globalIndex: Int) -> Unit
) {
  // Restore saved position when book is first opened
  LaunchedEffect(scrollToElementIndex, pages) {
    if (scrollToElementIndex != null && pages.isNotEmpty()) {
      val targetCalcPage = pages.indexOfLast { page -> page.items.any { it.index == scrollToElementIndex } }
      if (targetCalcPage >= 0) pagerState.scrollToPage(targetCalcPage / columnCount)
      onScrollHandled()
    }
  }

  // Restore position after config change (e.g. screen rotation resets pagerState to 0)
  LaunchedEffect(pages, columnCount) {
    if (pages.isNotEmpty() && pagerState.currentPage == 0) {
      currentElementIndex?.let { savedIndex ->
        val calcIdx = pages.indexOfFirst { page -> page.items.any { it.index == savedIndex } }
        if (calcIdx > 0) pagerState.scrollToPage(calcIdx / columnCount)
      }
    }
  }

  val currentPages by rememberUpdatedState(pages)
  val currentColumnCount by rememberUpdatedState(columnCount)

  LaunchedEffect(Unit) {
    snapshotFlow { pagerState.layoutInfo }
      .mapDistinctNotNullChanges { info ->
        info.visiblePagesInfo.firstOrNull()?.index?.let { screenPageIdx ->
          currentPages.getOrNull(screenPageIdx * currentColumnCount)?.items?.lastOrNull()
        }
      }
      .flowOn(Dispatchers.Default)
      .onEach { lastNode ->
        onPageChanged(lastNode.chapterIndex, lastNode.relativeIndex, lastNode.index)
      }
      .launchIn(this)
  }
}

internal val PAGE_HORIZONTAL_PADDING = 16.dp
internal val PAGE_VERTICAL_PADDING = 8.dp
