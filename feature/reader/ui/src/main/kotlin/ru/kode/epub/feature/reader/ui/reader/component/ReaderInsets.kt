package ru.kode.epub.feature.reader.ui.reader.component

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class ReaderInsets(
  val statusBarPadding: Dp,
  val bottomPadding: Dp,
  val sideStart: Dp,
  val sideEnd: Dp,
)

@Composable
internal fun rememberReaderInsets(): ReaderInsets {
  val layoutDirection = LocalLayoutDirection.current

  val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp
  val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
  val bottomPadding = navBarPadding + 16.dp

  val sideInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
  val sideInsetPaddingValues = sideInsets.asPaddingValues()
  val sideStart: Dp = sideInsetPaddingValues.calculateStartPadding(layoutDirection)
  val sideEnd: Dp = sideInsetPaddingValues.calculateEndPadding(layoutDirection)

  return remember(statusBarPadding, bottomPadding, sideStart, sideEnd) {
    ReaderInsets(
      statusBarPadding = statusBarPadding,
      bottomPadding = bottomPadding,
      sideStart = sideStart,
      sideEnd = sideEnd,
    )
  }
}
