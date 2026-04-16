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
  val progress: Progress = Progress()
) {
  @Immutable
  data class Progress(
    val positionKey: PositionKey = PositionKey(),
    val totalChapters: Long = 0L,
    val totalElements: Long = 0L
  )

  val readProgress: Float = progress.positionKey.elementIdx / progress.totalElements.toFloat()
}
