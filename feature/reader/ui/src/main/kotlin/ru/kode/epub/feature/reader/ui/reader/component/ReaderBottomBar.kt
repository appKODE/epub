package ru.kode.epub.feature.reader.ui.reader.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.kode.epub.core.uikit.theme.AppTheme
import ru.kode.epub.core.uikit.touch.disableClickThrough
import ru.kode.epub.feature.reader.ui.reader.Page
import ru.kode.epub.lib.entity.TocItem
import kotlin.math.roundToInt

@Suppress("ComposableEventParameterNaming")
@Composable
internal fun ReaderBottomBar(
  pagerState: PagerState,
  toc: List<TocItem>,
  pages: List<Page>,
  columnCount: Int,
  totalPages: Int,
  onPageSelected: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  val currentPage = pagerState.currentPage
  val tocScreenPages = remember(toc, pages, columnCount) {
    if (pages.isEmpty()) return@remember emptyList()
    val chapterFirstCalcPage = buildMap {
      pages.forEachIndexed { idx, page ->
        if (!containsKey(page.chapterIndex)) put(page.chapterIndex, idx)
      }
    }

    fun collectItems(items: List<TocItem>): List<Pair<TocItem, Int>> = buildList {
      for (item in items) {
        val calcIdx = chapterFirstCalcPage[item.chapterIndex] ?: continue
        add(item to (calcIdx / columnCount))
        addAll(collectItems(item.children))
      }
    }
    collectItems(toc).sortedBy { it.second }
  }
  val prevAnchor = tocScreenPages.lastOrNull { (_, pageIndex) -> pageIndex < currentPage }
  val nextAnchor = tocScreenPages.firstOrNull { (_, pageIndex) -> pageIndex > currentPage }
  val prevChapterDist = prevAnchor?.let { (_, pageIndex) -> currentPage - pageIndex }
  val nextChapterDist = nextAnchor?.let { (_, pageIndex) -> pageIndex - currentPage }
  val chapterFractions = remember(tocScreenPages, totalPages) {
    if (totalPages <= 1) emptyList()
    else tocScreenPages.map { (_, pageIndex) -> pageIndex.toFloat() / (totalPages - 1) }
  }
  val currentChapterTitle = tocScreenPages
    .lastOrNull { (_, pageIndex) -> pageIndex <= currentPage }
    ?.first?.title
    .orEmpty()
  val coroutineScope = rememberCoroutineScope()
  val sideInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
  val layoutDirection = LocalLayoutDirection.current
  val sideStart = sideInsets.asPaddingValues().calculateStartPadding(layoutDirection)
  val sideEnd = sideInsets.asPaddingValues().calculateEndPadding(layoutDirection)
  Column(
    modifier = modifier
      .background(AppTheme.colors.surfaceBackground)
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
          TextButton(
            onClick = { prevAnchor.let { (_, p) -> coroutineScope.launch { pagerState.animateScrollToPage(p) } } }
          ) {
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
        if (currentChapterTitle.isNotBlank()) append("  $currentChapterTitle")
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
          TextButton(
            onClick = { nextAnchor.let { (_, p) -> coroutineScope.launch { pagerState.animateScrollToPage(p) } } }
          ) {
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
      colors = SliderDefaults.colors(thumbColor = AppTheme.colors.iconAccent),
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
