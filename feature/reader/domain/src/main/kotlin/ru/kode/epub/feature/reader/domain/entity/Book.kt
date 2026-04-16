package ru.kode.epub.feature.reader.domain.entity

import android.net.Uri
import androidx.compose.runtime.Immutable
import kotlinx.datetime.LocalDateTime

@Immutable
data class Book(
  val id: String,
  val name: String,
  val author: String,
  val uri: Uri,
  val cover: Uri? = null,
  val updatedAt: LocalDateTime,
  val progress: Progress = Progress(),
  val totalChapters: Long = 0L
) {
  @Immutable
  data class Progress(
    val chapter: Long = 0L,
    val tag: Long = 0L
  )

  val readProgress: Float = progress.chapter / totalChapters.toFloat()
}
