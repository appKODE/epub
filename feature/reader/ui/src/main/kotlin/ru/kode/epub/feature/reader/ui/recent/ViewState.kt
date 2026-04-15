package ru.kode.epub.feature.reader.ui.recent

import androidx.compose.runtime.Immutable
import ru.kode.epub.feature.reader.domain.entity.Book

@Immutable
data class ViewState(
  val loading: Boolean = false,
  val books: List<Book> = emptyList()
)
