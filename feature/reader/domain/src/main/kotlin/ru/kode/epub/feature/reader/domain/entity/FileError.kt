package ru.kode.epub.feature.reader.domain.entity

import androidx.compose.runtime.Immutable

@Immutable
sealed class FileError : RuntimeException() {
  data object NotFound : FileError()
  data object Forbidden : FileError()
  data object NotAvailable : FileError()
  data object IncorrectFormat : FileError()
  data object IO : FileError()
}
