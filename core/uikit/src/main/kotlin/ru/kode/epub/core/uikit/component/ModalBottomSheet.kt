package ru.kode.epub.core.uikit.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.kode.epub.core.uikit.theme.AppTheme

@Composable
fun ModalBottomSheet(
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
  showHandle: Boolean = true,
  sheetContent: @Composable ColumnScope.() -> Unit
) {
  val topPadding = WindowInsets.statusBars
    .union(WindowInsets.displayCutout)
    .asPaddingValues()
    .calculateTopPadding() + 10.dp
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(
    modifier = modifier.padding(top = topPadding),
    containerColor = AppTheme.colors.surfaceBackground,
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
