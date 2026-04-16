package ru.kode.epub.feature.reader.ui.reader

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.kode.epub.core.ui.screen.AppScreen
import ru.kode.epub.core.uikit.R
import ru.kode.epub.core.uikit.theme.AppTheme
import ru.kode.epub.core.uikit.touch.disableClickThrough
import ru.kode.epub.feature.reader.domain.entity.PageScrollMode
import ru.kode.epub.feature.reader.ui.bottombar.BottomBarStateRestoreEffect
import kotlin.math.roundToInt

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
      val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp
      val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
      val bottomPadding = navBarPadding + 16.dp

      val horizontalPaddingPx = with(density) { 32.dp.roundToPx() }
      val contentHeightPx = with(density) {
        (constraints.maxHeight - statusBarPadding.roundToPx() - bottomPadding.roundToPx())
          .coerceAtLeast(0)
      }
      val contentWidthPx = (constraints.maxWidth - horizontalPaddingPx).coerceAtLeast(0)

      val pages = rememberPageBreaks(
        elements = state.elements,
        contentHeightPx = contentHeightPx,
        contentWidthPx = contentWidthPx
      )

      val pagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })
      val coroutineScope = rememberCoroutineScope()

      LaunchedEffect(state.scrollToElementIndex, pages) {
        state.scrollToElementIndex?.let { targetIndex ->
          val pageIndex = pages.indexOfFirst { page -> page.any { it.index == targetIndex } }
          if (pageIndex >= 0) pagerState.scrollToPage(pageIndex)
          viewModel.onScrollHandled()
        }
      }

      if (state.scrollMode != null && pages.isNotEmpty()) {
        val pageContent: @Composable PagerScope.(pageIndex: Int) -> Unit = { pageIndex ->
          Column(
            modifier = Modifier
              .fillMaxSize()
              .pointerInput(state.scrollMode) {
                detectTapGestures { offset ->
                  val targetPage = when (state.scrollMode) {
                    PageScrollMode.Horizontal -> when {
                      offset.x < size.width * 0.2f -> (pagerState.currentPage - 1).takeIf { it >= 0 }
                      offset.x > size.width * 0.8f -> (pagerState.currentPage + 1).takeIf { it < pagerState.pageCount }
                      else -> null
                    }
                    PageScrollMode.Vertical -> when {
                      offset.y < size.height * 0.2f -> (pagerState.currentPage - 1).takeIf { it >= 0 }
                      offset.y > size.height * 0.8f -> (pagerState.currentPage + 1).takeIf { it < pagerState.pageCount }
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
              .padding(top = statusBarPadding, bottom = bottomPadding)
          ) {
            pages[pageIndex].forEach { indexed ->
              ContentItem(indexed.element)
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

      // Precompute page index for each TOC anchor once pages are ready
      val tocAnchorPages = remember(state.tocAnchors, pages) {
        state.tocAnchors.mapNotNull { anchor ->
          val pageIndex = pages.indexOfFirst { page -> page.any { it.index == anchor.elementIndex } }
          if (pageIndex >= 0) anchor to pageIndex else null
        }
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
          windowInsets = WindowInsets.statusBars
        )
      }

      AnimatedVisibility(
        visible = state.isTopBarVisible && pages.isNotEmpty(),
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = Modifier.align(Alignment.BottomCenter)
      ) {
        val currentPage = pagerState.currentPage
        val prevAnchor = tocAnchorPages.lastOrNull { (_, pageIndex) -> pageIndex < currentPage }
        val nextAnchor = tocAnchorPages.firstOrNull { (_, pageIndex) -> pageIndex > currentPage }
        val currentChapterTitle = tocAnchorPages
          .lastOrNull { (_, pageIndex) -> pageIndex <= currentPage }
          ?.first?.entry?.title
          .orEmpty()
        val chapterFractions = remember(tocAnchorPages, pages.size) {
          if (pages.size <= 1) emptyList()
          else tocAnchorPages.map { (_, pageIndex) -> pageIndex.toFloat() / (pages.size - 1) }
        }

        ReadingBottomBar(
          currentPage = currentPage,
          totalPages = pages.size,
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
            .padding(bottom = navBarPadding)
        )
      }
    }
  }
}

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
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .disableClickThrough()
      .padding(horizontal = 8.dp, vertical = 4.dp)
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
          // Uniform track — no active fill, no tick dots
          drawLine(
            color = trackColor,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = size.height,
            cap = StrokeCap.Round
          )
          // Vertical chapter marks
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
