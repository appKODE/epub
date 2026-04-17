package ru.kode.epub.feature.reader.ui.reader.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.kode.epub.core.ui.compose.modifiers.navigationBarsPadding
import ru.kode.epub.core.uikit.theme.AppTheme
import ru.kode.epub.core.uikit.touch.disableClickThrough
import kotlin.math.roundToInt

@Composable
internal fun ReaderBottomBar(
  visible: Boolean,
  pagerState: PagerState,
  params: ColumnParams,
  onScrollToElement: (Int) -> Unit,
  insets: ReaderInsets,
  modifier: Modifier = Modifier,
) {
  AnimatedVisibility(
    visible = visible,
    enter = slideInVertically { it },
    exit = slideOutVertically { it },
    modifier = modifier
  ) {
    val scope = rememberCoroutineScope()
    val currentPage = pagerState.currentPage
    val prevAnchor = params.tocAnchorPages.lastOrNull { (_, pageIndex) -> pageIndex < currentPage }
    val nextAnchor = params.tocAnchorPages.firstOrNull { (_, pageIndex) -> pageIndex > currentPage }
    // In 2-column mode two chapters can appear on the same screen — show the last one
    val currentChapterTitle = params.tocAnchorPages
      .lastOrNull { (_, pageIndex) -> pageIndex <= currentPage }
      ?.first
      ?.entry
      ?.title
      .orEmpty()
    val chapterFractions = remember(params.tocAnchorPages, params.screenPageCount) {
      if (params.screenPageCount <= 1) emptyList()
      else params.tocAnchorPages.map { (_, pageIndex) -> pageIndex.toFloat() / (params.screenPageCount - 1) }
    }

    BottomBar(
      currentPage = currentPage,
      totalPages = params.screenPageCount,
      chapterTitle = currentChapterTitle,
      chapterFractions = chapterFractions,
      prevChapterDist = prevAnchor?.let { (_, pageIndex) -> currentPage - pageIndex },
      nextChapterDist = nextAnchor?.let { (_, pageIndex) -> pageIndex - currentPage },
      onPrevChapter = { prevAnchor?.let { (anchor, _) -> onScrollToElement(anchor.elementIndex) } },
      onNextChapter = { nextAnchor?.let { (anchor, _) -> onScrollToElement(anchor.elementIndex) } },
      onPageSelected = { page -> scope.launch { pagerState.scrollToPage(page) } },
      modifier = Modifier
        .fillMaxWidth()
        .background(AppTheme.colors.surfaceBackground)
        .navigationBarsPadding(start = false, end = false),
      sideStart = insets.sideStart,
      sideEnd = insets.sideEnd
    )
  }
}

@Suppress("ComposableEventParameterNaming")
@Composable
private fun BottomBar(
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
