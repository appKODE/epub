package ru.kode.epub.feature.reader.ui.reader

import androidx.compose.runtime.Stable
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import ru.kode.epub.core.ui.screen.ViewModel
import ru.kode.epub.feature.reader.domain.entity.Book

@Stable
class ReaderViewModel @Inject constructor(
  @Assisted val book: Book
) : ViewModel<ViewState, Unit>() {
  override fun initialState() = ViewState()
}
