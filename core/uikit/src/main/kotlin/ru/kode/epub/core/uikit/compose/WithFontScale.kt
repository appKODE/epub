package ru.kode.epub.core.uikit.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

@Composable
fun WithFontScale(fontScale: Float = 1f, content: @Composable () -> Unit) {
  val density = LocalDensity.current.density
  val updatedDensity = remember(density, fontScale) {
    Density(density = density, fontScale = fontScale)
  }
  CompositionLocalProvider(
    value = LocalDensity provides updatedDensity,
    content = content
  )
}
