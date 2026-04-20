package ru.kode.epub.core.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.transformLatest

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
