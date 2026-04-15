package ru.kode.epub.feature.reader.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import ru.kode.epub.core.ui.screen.AppScreen

@Composable
fun SettingsScreen(
  viewModel: SettingsViewModel
) = AppScreen(viewModel) { state ->
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Red)
  )
}
