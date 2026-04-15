package ru.kode.epub.feature.reader.ui.settings

import androidx.compose.runtime.Stable
import me.tatarka.inject.annotations.Inject
import ru.kode.epub.core.ui.screen.ViewModel

@Stable
class SettingsViewModel @Inject constructor() : ViewModel<ViewState, Unit>() {
  override fun initialState() = ViewState()
}
