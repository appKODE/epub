package ru.kode.epub.feature.reader.data

import android.net.Uri
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ru.kode.epub.feature.reader.domain.entity.ReaderSettings
import kotlin.time.Clock
import kotlin.time.Instant
import ru.kode.epub.feature.reader.data.Book as DataBook
import ru.kode.epub.feature.reader.domain.entity.Book as DomainBook

internal fun createBook(
  id: String,
  uri: String,
  cover: String = "",
  name: String = ""
): DataBook {
  return DataBook(
    id = id,
    uri = uri,
    name = name,
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
