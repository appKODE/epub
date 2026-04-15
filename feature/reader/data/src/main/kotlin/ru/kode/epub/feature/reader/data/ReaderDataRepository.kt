package ru.kode.epub.feature.reader.data

import android.content.Context
import android.net.Uri
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import ru.kode.epub.core.domain.di.SingleIn
import ru.kode.epub.core.domain.randomUuid
import ru.kode.epub.feature.reader.domain.ReaderRepository
import ru.kode.epub.feature.reader.domain.di.ReaderScope
import ru.kode.epub.feature.reader.domain.entity.Book
import ru.kode.epub.feature.reader.domain.entity.FileError.IncorrectFormat
import ru.kode.epub.feature.reader.domain.entity.FileError.NotAvailable
import ru.kode.epub.feature.reader.domain.entity.FileError.NotFound
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import java.io.File
import java.io.FileNotFoundException

@SingleIn(ReaderScope::class)
@ContributesBinding(ReaderScope::class)
class ReaderDataRepository @Inject constructor(
  private val context: Context,
  private val database: BooksDatabase
) : ReaderRepository {

  private val storage = File(context.storageDir, "/")

  override suspend fun store(uri: Uri): Book = withContext(Dispatchers.IO) {
    val type = context.contentResolver.getType(uri)
    if (type != "application/epub+zip") throw IncorrectFormat
    val id = randomUuid()
    val file = uri.copyTo(id, storage)
    val book = createBook(id = id, uri = Uri.fromFile(file).toString())
    database.bookQueries.insertOrReplace(book)
    book.toDomainModel()
  }

  override val books: Flow<List<Book>>
    get() = database.bookQueries
      .selectAll(::domainBookMapper)
      .asFlow()
      .mapToList(Dispatchers.IO)
      .distinctUntilChanged()

  private suspend fun Uri.copyTo(id: String, dir: File): File {
    val file = File(dir, "$id.${findExtension(context)}")
    runCatching { context.contentResolver.openInputStream(this) }
      .getOrElse { throw if (it is FileNotFoundException) NotFound else NotAvailable }
      ?.write(file)
    return file
  }
}

private val Context.storageDir: File
  get() = (getExternalFilesDir(null) ?: filesDir)
