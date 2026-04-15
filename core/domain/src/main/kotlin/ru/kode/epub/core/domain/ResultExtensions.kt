package ru.kode.epub.core.domain

import kotlin.coroutines.cancellation.CancellationException

public inline fun <V> runSuspendCatching(block: () -> V): Result<V> {
  return runCatching(block).throwIf { it is CancellationException }
}

inline infix fun <T, V> T.runSuspendCatching(block: T.() -> V): Result<V> {
  return runCatching(block).throwIf { it is CancellationException }
}

inline fun <V> Result<V>.throwIf(predicate: (Throwable) -> Boolean): Result<V> {
  exceptionOrNull()?.let { error -> if (predicate(error)) throw error }
  return this
}
