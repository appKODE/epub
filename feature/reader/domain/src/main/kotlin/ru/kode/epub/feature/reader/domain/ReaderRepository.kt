package ru.kode.epub.feature.reader.domain

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import ru.kode.epub.feature.reader.domain.entity.Book
import ru.kode.epub.feature.reader.domain.entity.ReaderSettings

interface ReaderRepository {
  val books: Flow<List<Book>>
  val settings: Flow<List<ReaderSettings>>

  suspend fun updateBookPosition(id: String, positionKey: String)
  suspend fun updateBookTotalElements(id: String, totalElements: Int)

  suspend fun store(uri: Uri): Book
  suspend fun remove(id: String)
  suspend fun clear()

  suspend fun update(settings: ReaderSettings)
}
