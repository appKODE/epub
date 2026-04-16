package ru.kode.epub.feature.reader.data

import android.net.Uri
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ru.kode.epub.feature.reader.domain.entity.ReaderSettings
import ru.kode.epub.lib.entity.EpubBook
import kotlin.time.Clock
import kotlin.time.Instant
import ru.kode.epub.feature.reader.data.Book as DataBook
import ru.kode.epub.feature.reader.domain.entity.Book as DomainBook

internal fun createBook(
  epub: EpubBook,
  id: String,
  uri: String,
  cover: String
): DataBook {
  return DataBook(
    id = id,
    uri = uri,
    name = epub.title,
    author = epub.author,
    updatedAt = Clock.System.now(),
    lastChapter = null,
    lastTag = null,
    chapters = null,
    cover = cover
  )
}

internal fun DataBook.toDomainModel(): DomainBook {
  return domainBookMapper(
    id = id,
    name = name,
    author = author,
    uri = uri,
    cover = cover,
    updatedAt = updatedAt,
    lastChapter = lastChapter,
    lastTag = lastTag,
    chapters = chapters
  )
}

internal fun domainBookMapper(
  id: String,
  name: String,
  author: String,
  uri: String,
  cover: String?,
  updatedAt: Instant,
  lastChapter: Long?,
  lastTag: Long?,
  chapters: Long?
): DomainBook {
  return DomainBook(
    id = id,
    name = name,
    author = author,
    uri = Uri.parse(uri),
    cover = cover?.let(Uri::parse),
    updatedAt = updatedAt.toLocalDateTime(TimeZone.currentSystemDefault()),
    progress = DomainBook.Progress(
      chapter = lastChapter ?: 0L,
      tag = lastTag ?: 0L
    ),
    totalChapters = chapters ?: 0L
  )
}

internal fun ReaderSettings.toStorageModel(): Settings {
  return Settings(key.value, selected?.value)
}
