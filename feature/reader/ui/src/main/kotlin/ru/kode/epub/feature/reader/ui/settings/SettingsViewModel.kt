package ru.kode.epub.feature.reader.ui.settings

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import me.tatarka.inject.annotations.Inject
import ru.kode.epub.core.domain.entity.resRef
import ru.kode.epub.core.ui.screen.ViewModel
import ru.kode.epub.core.uikit.component.Dialog
import ru.kode.epub.core.uikit.component.Snackbar
import ru.kode.epub.feature.reader.domain.ReaderModel
import ru.kode.epub.feature.reader.domain.entity.ReaderSettings
import ru.kode.epub.feature.reader.ui.R

@Stable
class SettingsViewModel @Inject constructor(
  private val model: ReaderModel
) : ViewModel<ViewState, Unit>() {
  override fun initialState() = ViewState()

  init {
    model.readerSettings
      .onEach { settings -> stateFlow.update { it.copy(settings = settings) } }
      .launchIn(viewModelScope)

    model.clearStorageEvents
      .onEach { sendViewEvent(Snackbar(resRef(R.string.all_books_removed_message))) }
      .launchIn(viewModelScope)
  }

  fun apply(settings: ReaderSettings) {
    model.apply(settings)
  }

  fun clearStorage() {
    sendViewEvent(
      Dialog.Confirm(
        title = resRef(R.string.remove_all_books_title),
        text = resRef(R.string.remove_book_description),
        useConfirmNegative = true,
        confirmButtonText = resRef(R.string.remove_book_action),
        dismissButtonText = resRef(R.string.remove_book_cancel),
        onConfirm = { model.removeAllBooks() }
      )
    )
  }
}
