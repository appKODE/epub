package ru.kode.epub.feature.reader.domain.entity

import android.net.Uri
import androidx.compose.runtime.Immutable
import kotlinx.datetime.LocalDateTime

@Immutable
data class Book(
  val id: String,
  val name: String,
  val uri: Uri,
  val cover: Uri?,
  val updatedAt: LocalDateTime,
  val progress: Progress,
  val totalChapters: Long
) {
  @Immutable
  data class Progress(
    val chapter: Long,
    val tag: Long
  )

  val readProgress: Float = progress.chapter / totalChapters.toFloat()
}
