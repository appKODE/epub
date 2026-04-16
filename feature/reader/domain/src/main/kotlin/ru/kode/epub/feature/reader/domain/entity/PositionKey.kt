package ru.kode.epub.feature.reader.domain.entity

import androidx.compose.runtime.Immutable

@Immutable
data class PositionKey(
  val elementRelativeIdx: Long = 0L,
  val elementIdx: Long = 0L,
  val chapterIdx: Long = 0L
)

fun String.toPositionKey(): PositionKey {
  val indices = split("-")
  return PositionKey(
    chapterIdx = indices.getOrNull(0)?.toLongOrNull() ?: 0L,
    elementRelativeIdx = indices.getOrNull(1)?.toLongOrNull() ?: 0L,
    elementIdx = indices.getOrNull(2)?.toLongOrNull() ?: 0L
  )
}
