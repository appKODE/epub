package ru.kode.epub.feature.reader.ui.bottombar

import androidx.compose.runtime.Immutable

@Immutable
data class BottomBarState(
  val selectedSection: BottomBarSection? = null
)

enum class BottomBarSection {
  Recent, Settings
}
