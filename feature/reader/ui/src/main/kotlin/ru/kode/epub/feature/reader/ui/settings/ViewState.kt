package ru.kode.epub.feature.reader.ui.settings

import androidx.compose.runtime.Immutable
import ru.kode.epub.feature.reader.domain.entity.ReaderSettings

@Immutable
data class ViewState(
  val loading: Boolean = false,
  val settings: List<ReaderSettings> = emptyList()
)
