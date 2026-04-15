package ru.kode.epub.core.domain

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.isActive
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun <T1, T2> Flow<T1>.mapDistinctChanges(transform: suspend (T1) -> T2): Flow<T2> =
  this.map(transform).distinctUntilChanged()

fun <T1, T2 : Any> Flow<T1>.mapDistinctNotNullChanges(transform: suspend (T1) -> T2?): Flow<T2> =
  this.mapNotNull(transform).distinctUntilChanged()

/**
 * onEach operator which executes suspending action, which will be canceled
 * if new emit is received before execution completes
 * https://github.com/Kotlin/kotlinx.coroutines/issues/2214
 */
fun <T> Flow<T>.onEachLatest(action: suspend (T) -> Unit): Flow<T> = transformLatest { value ->
  action(value)
  return@transformLatest emit(value)
}

fun <T> Flow<T>.throttleFirst(periodMillis: Long): Flow<T> {
  require(periodMillis > 0) { "period should be positive" }
  return flow {
    var lastTime = 0L
    collect { value ->
      val currentTime = System.currentTimeMillis()
      if (currentTime - lastTime >= periodMillis) {
        lastTime = currentTime
        emit(value)
      }
    }
  }
}

fun <T : Any> Flow<T>.pairwise(): Flow<Pair<T, T>> = flow {
  var prev: T? = null
  collect { next ->
    prev?.let { emit(Pair(it, next)) }
    prev = next
  }
}

@Suppress("UseOnStartEmit") // this is the definition :)
fun <T> Flow<T>.onStartEmit(value: T): Flow<T> = this.onStart { emit(value) }

fun tickerFlow(period: Duration = 1.seconds, delay: Duration = 1.seconds): Flow<Duration> = callbackFlow {
  var time = Duration.ZERO
  while (isActive) {
    delay(delay)
    time += period
    trySend(time)
  }
  awaitClose()
}
