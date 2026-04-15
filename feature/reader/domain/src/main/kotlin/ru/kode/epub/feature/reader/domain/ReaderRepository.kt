package ru.kode.epub.feature.reader.domain

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import ru.kode.epub.feature.reader.domain.entity.Book
import ru.kode.epub.feature.reader.domain.entity.ReaderSettings

interface ReaderRepository {
  suspend fun store(uri: Uri): Book
  val books: Flow<List<Book>>

  val settings: Flow<List<ReaderSettings>>
  suspend fun update(settings: ReaderSettings)
}
