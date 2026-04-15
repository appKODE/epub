package ru.kode.epub.core.routing

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import ru.kode.epub.core.domain.mapDistinctChanges
import ru.kode.epub.core.routing.di.FlowComponent
import ru.kode.epub.core.ui.screen.ViewModel

inline fun <reified VM : ViewModel<*, *>> FlowComponent.viewModel(
  vararg params: Any?
): VM {
  val key = VM::class.qualifiedName.orEmpty()
  return viewModelProviders()
    .firstOrNull { it.key == key }
    ?.factory
    ?.build(*params)
    as? VM
    ?: error("ViewModel $key is not registered")
}

fun <T : Any> Value<T>.asFlow(): Flow<T> {
  return callbackFlow {
    val disposable = subscribe { e -> trySend(e) }
    awaitClose { disposable.cancel() }
  }
}

fun Value<ChildStack<*, *>>.screenResultFlow(): Flow<Any> {
  return activeViewModel()
    .flatMapLatest { it.screenEvents }
    .filterNotNull()
}

fun Value<ChildStack<*, *>>.navigateBackEvents(): Flow<Unit> {
  return activeViewModel().flatMapLatest { it.navigateBackEvents }
}

private fun Value<ChildStack<*, *>>.activeViewModel(): Flow<ViewModel<*, *>> {
  return asFlow()
    .mapDistinctChanges { it.active.instance }
    .filterIsInstance<Screen?>()
    .filterNotNull()
    .mapLatest { it.viewModel }
}
