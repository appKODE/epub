@file:OptIn(ExperimentalTime::class)

package ru.kode.epub.core.data.storage

import app.cash.sqldelight.ColumnAdapter
import kotlinx.datetime.LocalDate
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

object LocalDateAdapter : ColumnAdapter<LocalDate, String> {
  override fun decode(databaseValue: String): LocalDate {
    val (year, month, dayOfMonth) = databaseValue.split("|").map { it.toInt() }
    return LocalDate(year, month, dayOfMonth)
  }

  override fun encode(value: LocalDate): String {
    return "${value.year}|${value.monthNumber}|${value.dayOfMonth}"
  }
}

fun timestampAdapter() = SimpleColumnAdapter<Instant, Long>(
  encodeFn = { it.toEpochMilliseconds() },
  decodeFn = { Instant.fromEpochMilliseconds(it) }
)
