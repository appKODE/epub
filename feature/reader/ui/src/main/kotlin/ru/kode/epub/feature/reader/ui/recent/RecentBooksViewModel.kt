package ru.kode.epub.feature.reader.ui.recent

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import me.tatarka.inject.annotations.Inject
import ru.kode.epub.core.domain.di.AppScope
import ru.kode.epub.core.domain.di.SingleIn
import ru.kode.epub.core.domain.entity.resRef
import ru.kode.epub.core.ui.screen.ViewModel
import ru.kode.epub.core.uikit.component.Dialog
import ru.kode.epub.core.uikit.component.Snackbar
import ru.kode.epub.feature.reader.domain.ReaderModel
import ru.kode.epub.feature.reader.domain.entity.Book
import ru.kode.epub.feature.reader.ui.R

@Stable
class RecentBooksViewModel @Inject constructor(
  private val launcher: AddBookLauncher,
  val model: ReaderModel
) : ViewModel<ViewState, RecentBooksResult>() {

  override fun initialState() = ViewState()

  override fun onStart() {
    model.books
      .onEach { books -> stateFlow.update { it.copy(books = books, loading = false) } }
      .launchIn(viewModelScope)

    model.bookRemovedEvents
      .onEach { sendViewEvent(Snackbar(resRef(R.string.book_removed_message))) }
      .launchIn(viewModelScope)

    launcher.results
      .onEach(model::readEpub)
      .launchIn(viewModelScope)
  }

  fun openBook(book: Book) {
    emitResult(RecentBooksResult.Reader(book))
  }

  fun removeBook(book: Book) {
    sendViewEvent(
      Dialog.Confirm(
        title = resRef(R.string.remove_book_title),
        text = resRef(R.string.remove_book_description),
        useConfirmNegative = true,
        confirmButtonText = resRef(R.string.remove_book_action),
        dismissButtonText = resRef(R.string.remove_book_cancel),
        onConfirm = { model.removeBook(book.id) }
      )
    )
  }

  fun addBook() {
    launcher.launch()
  }
}

sealed interface RecentBooksResult {
  data object Settings : RecentBooksResult
  data class Reader(val book: Book) : RecentBooksResult
}

@SingleIn(AppScope::class)
class AddBookLauncher @Inject constructor() {
  private val uris = MutableSharedFlow<Uri>(extraBufferCapacity = 3)
  val results = uris.asSharedFlow()

  private lateinit var launcher: ActivityResultLauncher<Array<String>>

  fun register(activity: ComponentActivity) {
    launcher = activity.registerForActivityResult(
      contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> if (uri != null) uris.tryEmit(uri) }
  }

  fun launch() {
    launcher.launch(arrayOf("application/epub+zip"))
  }
}
