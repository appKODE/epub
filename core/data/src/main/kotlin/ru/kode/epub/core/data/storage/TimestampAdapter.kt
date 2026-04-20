@file:OptIn(ExperimentalTime::class)

package ru.kode.epub.core.data.storage

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

fun timestampAdapter() = SimpleColumnAdapter<Instant, Long>(
  encodeFn = { it.toEpochMilliseconds() },
  decodeFn = { Instant.fromEpochMilliseconds(it) }
)
