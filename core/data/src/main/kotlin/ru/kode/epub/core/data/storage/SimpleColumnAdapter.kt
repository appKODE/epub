package ru.kode.epub.core.data.storage

import app.cash.sqldelight.ColumnAdapter

class SimpleColumnAdapter<T : Any, R : Any>(
  private val encodeFn: (value: T) -> R,
  private val decodeFn: (databaseValue: R) -> T
) : ColumnAdapter<T, R> {
  override fun decode(databaseValue: R) = decodeFn(databaseValue)
  override fun encode(value: T) = encodeFn(value)
}
