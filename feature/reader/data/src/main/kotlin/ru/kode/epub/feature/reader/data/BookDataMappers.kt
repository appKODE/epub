package ru.kode.epub.feature.reader.data

import android.net.Uri
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant
import ru.kode.epub.feature.reader.data.Book as DataBook
import ru.kode.epub.feature.reader.domain.entity.Book as DomainBook

internal fun createBook(
  id: String,
  uri: String,
  name: String = id
): DataBook {
  return DataBook(
    id = id,
    uri = uri,
    name = name,
    updatedAt = Clock.System.now(),
    lastChapter = null,
    lastTag = null,
    chapters = null
  )
}

internal fun DataBook.toDomainModel(): DomainBook {
  return domainBookMapper(
    id = id,
    name = name,
    uri = uri,
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
  updatedAt: Instant,
  lastChapter: String?,
  lastTag: Long?,
  chapters: Long?
): DomainBook {
  return DomainBook(
    id = id,
    name = name,
    uri = Uri.parse(uri),
    updatedAt = updatedAt.toLocalDateTime(TimeZone.currentSystemDefault()),
    progress = DomainBook.Progress(
      chapter = lastChapter,
      tag = lastTag ?: 0L
    ),
    totalChapters = chapters ?: 0L
  )
}
