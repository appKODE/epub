package ru.kode.epub.feature.reader.data

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import ru.kode.epub.core.domain.di.AppScope
import ru.kode.epub.core.domain.di.SingleIn
import ru.kode.epub.core.domain.mapDistinctChanges
import ru.kode.epub.core.domain.randomUuid
import ru.kode.epub.feature.reader.domain.ReaderRepository
import ru.kode.epub.feature.reader.domain.entity.Book
import ru.kode.epub.feature.reader.domain.entity.FileError.IncorrectFormat
import ru.kode.epub.feature.reader.domain.entity.FileError.NotAvailable
import ru.kode.epub.feature.reader.domain.entity.FileError.NotFound
import ru.kode.epub.feature.reader.domain.entity.ReaderSettings
import ru.kode.epub.feature.reader.domain.entity.readerSettings
import ru.kode.epub.lib.EpubParser
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import ru.kode.epub.feature.reader.data.Book as StorageBook

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ReaderDataRepository @Inject constructor(
  private val context: Context,
  private val booksDatabase: BooksDatabase,
  private val settingsDatabase: SettingsDatabase
) : ReaderRepository {

  private val storage = File(context.storageDir, "/")

  override val books: Flow<List<Book>>
    get() = booksDatabase.bookQueries
      .selectAll(::domainBookMapper)
      .asFlow()
      .mapToList(Dispatchers.IO)
      .distinctUntilChanged()

  override val settings: Flow<List<ReaderSettings>> = settingsDatabase.settingsQueries
    .selectAll()
    .asFlow()
    .mapToList(Dispatchers.IO)
    .mapDistinctChanges { prefs ->
      val saved = prefs.associate { it.key to it.value_ }
      readerSettings().map { setting ->
        setting.copy(
          selected = setting.available
            .firstOrNull { it.value == saved[setting.key.value] }
            ?: setting.defaultSelected()
        )
      }
    }

  override suspend fun update(settings: ReaderSettings) {
    withContext(Dispatchers.IO) {
      settingsDatabase.settingsQueries.insertOrReplace(settings.toStorageModel())
    }
  }

  override suspend fun store(uri: Uri): Book = withContext(Dispatchers.IO) {
    val type = context.contentResolver.getType(uri)
    if (type != "application/epub+zip") throw IncorrectFormat
    val id = randomUuid()
    val file = uri.copyTo(id, storage)
    val copy = Uri.fromFile(file)
    val epub = EpubParser.parse(context, copy)
    val cover = epub.coverImage?.let { image ->
      runCatching {
        val file = File(context.storageDir, "$id.cover")
        FileOutputStream(file).use { fos -> fos.write(image.data) }
        file
      }
        .getOrNull()
    }
      ?.let(Uri::fromFile)
    val book = createBook(epub = epub, id = id, uri = copy.toString(), cover = cover.toString())
    booksDatabase.bookQueries.insertOrReplace(book)
    book.toDomainModel()
  }

  override suspend fun remove(id: String) {
    withContext(Dispatchers.IO) {
      val book = booksDatabase.bookQueries.select(id).executeAsOneOrNull() ?: return@withContext
      booksDatabase.bookQueries.delete(id)
      removeBookFiles(book)
    }
  }

  override suspend fun clear() {
    withContext(Dispatchers.IO) {
      val books = booksDatabase.bookQueries.selectAll().executeAsList()
      booksDatabase.transaction {
        for (book in books) {
          booksDatabase.bookQueries.delete(book.id)
          removeBookFiles(book)
        }
      }
    }
  }

  private fun removeBookFiles(book: StorageBook) {
    runCatching {
      File(book.uri.toUri().path).delete()
      if (book.cover != null) {
        File(book.cover.toUri().path).delete()
      }
    }
      .onFailure(Timber::e)
  }

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
