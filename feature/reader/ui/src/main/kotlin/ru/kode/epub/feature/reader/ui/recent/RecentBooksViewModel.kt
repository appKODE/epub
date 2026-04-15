package ru.kode.epub.feature.reader.ui.recent

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import me.tatarka.inject.annotations.Inject
import ru.kode.epub.core.ui.screen.ViewModel
import ru.kode.epub.feature.reader.domain.ReaderModel
import ru.kode.epub.feature.reader.domain.entity.Book

@Stable
class RecentBooksViewModel @Inject constructor(
  val model: ReaderModel
) : ViewModel<ViewState, RecentBooksResult>() {
  override fun initialState() = ViewState()

  override fun onStart() {
    model.books
      .onEach { books -> stateFlow.update { it.copy(books = books) } }
      .launchIn(viewModelScope)
  }

  fun openBook(book: Book) {
    emitResult(RecentBooksResult.Reader(book))
  }
}

sealed interface RecentBooksResult {
  data object Settings : RecentBooksResult
  data class Reader(val book: Book) : RecentBooksResult
}
