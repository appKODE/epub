package ru.kode.epub.core.ui.compose

import androidx.compose.runtime.staticCompositionLocalOf
import ru.kode.epub.core.domain.entity.ScreenOrientation

val LocalScreenOrientation = staticCompositionLocalOf<ScreenOrientation> {
  error("No ScreenOrientation provided")
}
