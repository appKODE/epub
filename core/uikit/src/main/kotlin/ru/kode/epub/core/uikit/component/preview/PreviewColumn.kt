package ru.kode.epub.core.uikit.component.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import ru.kode.epub.core.uikit.theme.AppTheme

@Composable
fun PreviewColumn(
  useDarkTheme: Boolean = isSystemInDarkTheme(),
  backgroundColor: Color = if (useDarkTheme) Color.Black else Color.White,
  verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(16.dp),
  contentPadding: PaddingValues = PaddingValues(16.dp),
  content: @Composable ColumnScope.() -> Unit
) {
  AppTheme(useDarkTheme = useDarkTheme, layoutDirection = LocalLayoutDirection.current) {
    Column(
      modifier = Modifier
        .background(backgroundColor)
        .fillMaxWidth()
        .padding(contentPadding),
      verticalArrangement = verticalArrangement
    ) {
      content()
    }
  }
}
