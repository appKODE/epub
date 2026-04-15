package ru.kode.epub.feature.reader.ui.settings

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import me.tatarka.inject.annotations.Inject
import ru.kode.epub.core.ui.screen.ViewModel
import ru.kode.epub.feature.reader.domain.ReaderModel
import ru.kode.epub.feature.reader.domain.entity.ReaderSettings

@Stable
class SettingsViewModel @Inject constructor(
  private val model: ReaderModel
) : ViewModel<ViewState, Unit>() {
  override fun initialState() = ViewState()

  override fun onStart() {
    model.readerSettings
      .onEach { settings -> stateFlow.update { it.copy(settings = settings) } }
      .launchIn(viewModelScope)
  }

  fun apply(settings: ReaderSettings) {
    model.apply(settings)
  }
}
