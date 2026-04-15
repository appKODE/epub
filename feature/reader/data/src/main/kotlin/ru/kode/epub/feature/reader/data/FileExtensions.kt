package ru.kode.epub.feature.reader.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.kode.epub.core.domain.runSuspendCatching
import ru.kode.epub.feature.reader.domain.entity.FileError
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream

internal fun Uri.findExtension(context: Context): String? {
  return if (scheme == ContentResolver.SCHEME_CONTENT) {
    runCatching {
      context.contentResolver
        .query(this, MediaStore.MediaColumns.DISPLAY_NAME)
        ?.let(::File)
        ?.extension
    }
      .getOrNull()
  } else {
    path?.let(::File)?.extension
  }
    ?.takeUnless { it.isBlank() }
    ?: "epub"
}

private fun ContentResolver.query(uri: Uri, projection: String): String? =
  runCatching<String?> {
    query(uri, arrayOf(projection), null, null, null)
      ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
  }
    .getOrNull()

@Suppress("ThrowsCount") // ok for FS functions
internal suspend fun InputStream.write(file: File) {
  runSuspendCatching {
    file.parentFile?.mkdirs()
    file.createNewFile()
    withContext(Dispatchers.IO) {
      use { input -> file.outputStream().use { input.copyTo(it) } }
    }
  }
    .onFailure { error ->
      Timber.e(error)
      runCatching { file.delete() }
      when (error) {
        is SecurityException -> throw FileError.Forbidden
        is FileNotFoundException -> throw FileError.NotFound
        else -> throw FileError.IO
      }
    }
}
