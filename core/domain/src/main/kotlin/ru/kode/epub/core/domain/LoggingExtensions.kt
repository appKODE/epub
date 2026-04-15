package ru.kode.epub.core.domain

import timber.log.Timber

fun Throwable.asLog(message: String? = null): String = buildString {
  if (message != null) appendLine(message)
  append(this@asLog.stackTraceToString())
}

inline fun <R> logWithExecutionTime(message: String, crossinline block: () -> R): R {
  val start = System.currentTimeMillis()
  val result = block()
  val end = System.currentTimeMillis()
  Timber.d("$message took ${end - start} ms")
  return result
}

suspend inline fun <R> suspendLogWithExecutionTime(message: String, crossinline block: suspend () -> R): R {
  val start = System.currentTimeMillis()
  val result = block()
  val end = System.currentTimeMillis()
  Timber.d("$message took ${end - start} ms")
  return result
}
