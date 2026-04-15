package ru.kode.epub.core.uikit.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.kode.epub.core.uikit.theme.AppTheme

@Composable
fun BottomSheetHandle(
  modifier: Modifier = Modifier,
  handleColor: Color = AppTheme.colors.textQuaternary
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(36.dp),
    contentAlignment = Alignment.Center
  ) {
    Spacer(
      modifier = Modifier
        .size(width = 32.dp, height = 4.dp)
        .background(
          color = handleColor,
          shape = RoundedCornerShape(4.dp)
        )
    )
  }
}
