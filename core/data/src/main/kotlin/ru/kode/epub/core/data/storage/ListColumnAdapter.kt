package ru.kode.epub.core.data.storage

import app.cash.sqldelight.ColumnAdapter

class ListColumnAdapter<T : Any>(
  private val elementAdapter: ColumnAdapter<T, String>,
  private val separator: String = ";"
) : ColumnAdapter<List<T>, String> {
  override fun encode(value: List<T>): String {
    return value.joinToString(separator, transform = { elementAdapter.encode(it) })
  }

  override fun decode(databaseValue: String): List<T> {
    return databaseValue.split(separator)
      .map { elementAdapter.decode(it) }
  }
}
