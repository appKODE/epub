package ru.kode.epub.feature.reader.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ru.kode.epub.core.domain.entity.ScreenOrientation
import ru.kode.epub.core.domain.mapDistinctNotNullChanges
import ru.kode.epub.core.ui.compose.LocalScreenOrientation
import ru.kode.epub.core.ui.screen.AppScreen
import ru.kode.epub.core.uikit.R
import ru.kode.epub.core.uikit.component.CircularLoaderWithOverlay
import ru.kode.epub.core.uikit.theme.AppTheme
import ru.kode.epub.core.uikit.touch.disableClickThrough
import ru.kode.epub.feature.reader.domain.entity.ColumnMode
import ru.kode.epub.feature.reader.domain.entity.PageScrollMode
import ru.kode.epub.feature.reader.ui.bottombar.BottomBarStateRestoreEffect
import kotlin.math.roundToInt

private val ColumnGap = 32.dp

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
      val density = LocalDensity.current
      val layoutDirection = LocalLayoutDirection.current
      val orientation = LocalScreenOrientation.current

      val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp
      val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
      val bottomPadding = navBarPadding + 16.dp

      val sideInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
      val sideInsetPaddingValues = sideInsets.asPaddingValues()
      val sideStart: Dp = sideInsetPaddingValues.calculateStartPadding(layoutDirection)
      val sideEnd: Dp = sideInsetPaddingValues.calculateEndPadding(layoutDirection)
      val sideHorizontalPx = with(density) { sideStart.roundToPx() + sideEnd.roundToPx() }

      val columnCount = if (
        state.columnMode == ColumnMode.Double && orientation == ScreenOrientation.Landscape
      ) 2 else 1

      val horizontalPaddingPx = with(density) { 32.dp.roundToPx() }
      val columnGapPx = with(density) { if (columnCount > 1) ColumnGap.roundToPx() else 0 }
      val contentHeightPx = with(density) {
        (constraints.maxHeight - statusBarPadding.roundToPx() - bottomPadding.roundToPx())
          .coerceAtLeast(0)
      }
      // Width available for all columns (subtract item padding and side system insets)
      val totalContentWidthPx = (constraints.maxWidth - horizontalPaddingPx - sideHorizontalPx).coerceAtLeast(0)
      val columnWidthPx = ((totalContentWidthPx - columnGapPx) / columnCount).coerceAtLeast(0)

      // Each calculator page = one column
      val calculatorPages = rememberPageBreaks(
        elements = state.elements,
        contentHeightPx = contentHeightPx,
        contentWidthPx = columnWidthPx
      )

      val screenPageCount = (calculatorPages.size + columnCount - 1) / columnCount
      val pagerState = rememberPagerState(pageCount = { screenPageCount.coerceAtLeast(1) })
      val coroutineScope = rememberCoroutineScope()

      // TOC / chapter navigation scroll
      LaunchedEffect(state.scrollToElementIndex, calculatorPages) {
        state.scrollToElementIndex?.let { targetIndex ->
          val calcIdx = calculatorPages.indexOfFirst { page -> page.any { it.index == targetIndex } }
          if (calcIdx >= 0) pagerState.scrollToPage(calcIdx / columnCount)
          viewModel.onScrollHandled()
        }
      }

      // Restore position after config change (pagerState resets to 0)
      LaunchedEffect(calculatorPages, columnCount) {
        if (calculatorPages.isNotEmpty() && pagerState.currentPage == 0) {
          state.currentElementIndex?.let { savedIndex ->
            val calcIdx = calculatorPages.indexOfFirst { page -> page.any { it.index == savedIndex } }
            if (calcIdx > 0) pagerState.scrollToPage(calcIdx / columnCount)
          }
        }
      }

      // Save position on every page change
      LaunchedEffect(pagerState.currentPage, columnCount) {
        val calcIdx = pagerState.currentPage * columnCount
        calculatorPages.getOrNull(calcIdx)?.firstOrNull()?.let {
          viewModel.onCurrentPageChanged(it.index)
        }
      }

      if (state.scrollMode != null && state.columnMode != null && calculatorPages.isNotEmpty()) {
        val pageContent: @Composable PagerScope.(screenPageIndex: Int) -> Unit = { screenPageIndex ->
          Row(
            modifier = Modifier
              .fillMaxSize()
              .pointerInput(state.scrollMode) {
                detectTapGestures { offset ->
                  val targetPage = when (state.scrollMode) {
                    PageScrollMode.Horizontal -> when {
                      offset.x < size.width * 0.2f ->
                        (pagerState.currentPage - 1).takeIf { it >= 0 }
                      offset.x > size.width * 0.8f ->
                        (pagerState.currentPage + 1).takeIf { it < pagerState.pageCount }
                      else -> null
                    }
                    PageScrollMode.Vertical -> when {
                      offset.y < size.height * 0.2f ->
                        (pagerState.currentPage - 1).takeIf { it >= 0 }
                      offset.y > size.height * 0.8f ->
                        (pagerState.currentPage + 1).takeIf { it < pagerState.pageCount }
                      else -> null
                    }
                  }
                  if (targetPage != null) {
                    coroutineScope.launch { pagerState.animateScrollToPage(targetPage) }
                  } else {
                    viewModel.toggleTopBar()
                  }
                }
              }
              .padding(top = statusBarPadding, bottom = bottomPadding, start = sideStart, end = sideEnd)
          ) {
            repeat(columnCount) { colIdx ->
              if (colIdx > 0) Spacer(Modifier.width(ColumnGap))
              Column(modifier = Modifier.weight(1f)) {
                val calcIdx = screenPageIndex * columnCount + colIdx
                calculatorPages.getOrElse(calcIdx) { emptyList() }.forEach { indexed ->
                  ContentItem(indexed.element)
                }
              }
            }
          }
        }
        when (state.scrollMode) {
          PageScrollMode.Horizontal -> HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageContent = pageContent
          )
          PageScrollMode.Vertical -> VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageContent = pageContent
          )
        }
      }

      // Map each TOC anchor to its screen page index
      val tocAnchorPages = remember(state.tocAnchors, calculatorPages, columnCount) {
        state.tocAnchors.mapNotNull { anchor ->
          val calcIdx = calculatorPages.indexOfFirst { page -> page.any { it.index == anchor.elementIndex } }
          if (calcIdx >= 0) anchor to (calcIdx / columnCount) else null
        }
      }

      LaunchedEffect(Unit) {
        snapshotFlow { pagerState.layoutInfo }
          .mapDistinctNotNullChanges { info ->
            info.visiblePagesInfo.firstOrNull()?.index?.let { idx ->
              calculatorPages[idx].firstOrNull()?.key
            }
          }
          .flowOn(Dispatchers.Default)
          .onEach(viewModel::onScroll)
          .launchIn(viewModel.viewModelScope)
      }

      AnimatedVisibility(
        visible = state.isTopBarVisible,
        enter = slideInVertically { -it },
        exit = slideOutVertically { -it }
      ) {
        TopAppBar(
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AppTheme.colors.surfaceBackground
          ),
          title = {
            Column {
              Text(
                text = state.bookInfo.title,
                style = AppTheme.typography.headline5,
                color = AppTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              if (state.bookInfo.author.isNotBlank()) {
                Text(
                  text = state.bookInfo.author,
                  style = AppTheme.typography.body2,
                  color = AppTheme.colors.textSecondary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          },
          navigationIcon = {
            IconButton(onClick = viewModel::navigateBack) {
              Icon(
                painter = painterResource(R.drawable.ic_arrow_backward_24),
                contentDescription = null,
                tint = AppTheme.colors.iconPrimary
              )
            }
          },
          actions = {
            IconButton(onClick = viewModel::showBookInfo) {
              Icon(
                painter = painterResource(R.drawable.ic_info_24),
                contentDescription = null,
                tint = AppTheme.colors.iconPrimary
              )
            }
            IconButton(onClick = viewModel::showToc) {
              Icon(
                painter = painterResource(R.drawable.ic_list_24),
                contentDescription = null,
                tint = AppTheme.colors.iconPrimary
              )
            }
          },
          windowInsets = WindowInsets.statusBars.union(sideInsets)
        )
      }

      AnimatedVisibility(
        visible = state.isTopBarVisible && calculatorPages.isNotEmpty(),
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = Modifier.align(Alignment.BottomCenter)
      ) {
        val currentPage = pagerState.currentPage
        val prevAnchor = tocAnchorPages.lastOrNull { (_, pageIndex) -> pageIndex < currentPage }
        val nextAnchor = tocAnchorPages.firstOrNull { (_, pageIndex) -> pageIndex > currentPage }
        // In 2-column mode two chapters can appear on the same screen — show the last one
        val currentChapterTitle = tocAnchorPages
          .lastOrNull { (_, pageIndex) -> pageIndex <= currentPage }
          ?.first
          ?.entry
          ?.title
          .orEmpty()
        val chapterFractions = remember(tocAnchorPages, screenPageCount) {
          if (screenPageCount <= 1) emptyList()
          else tocAnchorPages.map { (_, pageIndex) -> pageIndex.toFloat() / (screenPageCount - 1) }
        }

        ReadingBottomBar(
          currentPage = currentPage,
          totalPages = screenPageCount,
          chapterTitle = currentChapterTitle,
          chapterFractions = chapterFractions,
          prevChapterDist = prevAnchor?.let { (_, pageIndex) -> currentPage - pageIndex },
          nextChapterDist = nextAnchor?.let { (_, pageIndex) -> pageIndex - currentPage },
          onPrevChapter = { prevAnchor?.let { (anchor, _) -> viewModel.scrollToElement(anchor.elementIndex) } },
          onNextChapter = { nextAnchor?.let { (anchor, _) -> viewModel.scrollToElement(anchor.elementIndex) } },
          onPageSelected = { page -> coroutineScope.launch { pagerState.scrollToPage(page) } },
          modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.colors.surfaceBackground)
            .padding(bottom = navBarPadding),
          sideStart = sideStart,
          sideEnd = sideEnd
        )
      }
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

@Suppress("ComposableEventParameterNaming")
@Composable
private fun ReadingBottomBar(
  currentPage: Int,
  totalPages: Int,
  chapterTitle: String,
  chapterFractions: List<Float>,
  prevChapterDist: Int?,
  nextChapterDist: Int?,
  onPrevChapter: () -> Unit,
  onNextChapter: () -> Unit,
  onPageSelected: (Int) -> Unit,
  modifier: Modifier = Modifier,
  sideStart: Dp = 0.dp,
  sideEnd: Dp = 0.dp
) {
  Column(
    modifier = modifier
      .disableClickThrough()
      .padding(start = 8.dp + sideStart, end = 8.dp + sideEnd, top = 4.dp, bottom = 4.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth()
    ) {
      Box(modifier = Modifier.width(64.dp)) {
        if (prevChapterDist != null) {
          TextButton(onClick = onPrevChapter) {
            Text(
              text = "<$prevChapterDist",
              style = AppTheme.typography.body2,
              color = AppTheme.colors.textSecondary
            )
          }
        }
      }
      val label = buildString {
        append("${currentPage + 1}/$totalPages")
        if (chapterTitle.isNotBlank()) append("  $chapterTitle")
      }
      Text(
        text = label,
        style = AppTheme.typography.body2,
        color = AppTheme.colors.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier.weight(1f)
      )
      Box(modifier = Modifier.width(64.dp), contentAlignment = Alignment.CenterEnd) {
        if (nextChapterDist != null) {
          TextButton(onClick = onNextChapter) {
            Text(
              text = "$nextChapterDist>",
              style = AppTheme.typography.body2,
              color = AppTheme.colors.textSecondary
            )
          }
        }
      }
    }

    var sliderPosition by remember { mutableFloatStateOf(currentPage.toFloat()) }
    LaunchedEffect(currentPage) { sliderPosition = currentPage.toFloat() }

    val trackColor = AppTheme.colors.textSecondary.copy(alpha = 0.3f)
    val chapterMarkColor = AppTheme.colors.textSecondary

    Slider(
      value = sliderPosition,
      colors = SliderDefaults.colors(thumbColor = AppTheme.colors.textOnAccent),
      onValueChange = {
        sliderPosition = it
        onPageSelected(it.roundToInt())
      },
      valueRange = 0f..(totalPages - 1).toFloat().coerceAtLeast(0f),
      steps = (totalPages - 2).coerceAtLeast(0),
      track = {
        Canvas(
          modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
        ) {
          drawLine(
            color = trackColor,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = size.height,
            cap = StrokeCap.Round
          )
          for (fraction in chapterFractions) {
            val x = fraction * size.width
            drawLine(
              color = chapterMarkColor,
              start = Offset(x, -4.dp.toPx()),
              end = Offset(x, size.height + 4.dp.toPx()),
              strokeWidth = 1.5.dp.toPx()
            )
          }
        }
      },
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)
    )
  }
}
