package ru.kode.epub.core.uikit.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import ru.kode.epub.core.uikit.theme.AppTheme

@Composable
fun ModalBottomSheet(
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
  showHandle: Boolean = true,
  sheetContent: @Composable ColumnScope.() -> Unit
) {
  val safeInsets = WindowInsets.safeDrawing.asPaddingValues()
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val layoutDirection = LocalLayoutDirection.current
  ModalBottomSheet(
    modifier = modifier.padding(
      start = safeInsets.calculateStartPadding(layoutDirection) + 10.dp,
      top = safeInsets.calculateTopPadding() + 10.dp,
      end = safeInsets.calculateEndPadding(layoutDirection) + 10.dp
    ),
    containerColor = AppTheme.colors.surfaceLayer1,
    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    dragHandle = if (showHandle) {
      { BottomSheetHandle() }
    } else {
      null
    },
    sheetState = sheetState,
    onDismissRequest = onDismissRequest,
    contentWindowInsets = { WindowInsets.navigationBars.only(WindowInsetsSides.Bottom) },
    content = sheetContent
  )
}
