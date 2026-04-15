package ru.kode.epub.feature.reader.domain.entity

import android.net.Uri
import androidx.compose.runtime.Immutable
import kotlinx.datetime.LocalDateTime

@Immutable
data class Book(
  val id: String,
  val name: String,
  val uri: Uri,
  val updatedAt: LocalDateTime,
  val progress: Progress,
  val totalChapters: Long
) {
  @Immutable
  data class Progress(
    val chapter: String?,
    val tag: Long
  )
}
