package ru.kode.epub.feature.reader.domain

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import ru.kode.epub.feature.reader.domain.entity.Book

interface ReaderRepository {
  suspend fun store(uri: Uri): Book
  val books: Flow<List<Book>>
}
