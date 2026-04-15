package ru.kode.epub.feature.reader.ui.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ru.kode.epub.core.ui.screen.AppScreen
import ru.kode.epub.core.uikit.theme.AppTheme

@Composable
fun ReaderScreen(
  viewModel: ReaderViewModel
) = AppScreen(viewModel) { state ->
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = viewModel.book.uri.toString(),
      color = AppTheme.colors.textAccent
    )
  }
}
