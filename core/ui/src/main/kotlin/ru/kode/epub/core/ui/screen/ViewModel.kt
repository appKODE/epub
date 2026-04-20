package ru.kode.epub.core.ui.screen

import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import ru.kode.epub.core.ui.screen.event.ViewEvent
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
@Stable
abstract class ViewModel<VS : Any, R : Any> {

  val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private val attachedScope = CoroutineScope(viewModelScope.coroutineContext + SupervisorJob())

  private val _screenEvents = MutableSharedFlow<R>(
    extraBufferCapacity = 3,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
  )
  private val _navigateBackEvents = MutableSharedFlow<Unit>(
    extraBufferCapacity = 3,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
  )
  private val eventsFlow = MutableSharedFlow<ViewEvent>()

  val navigateBackEvents = _navigateBackEvents.asSharedFlow()
  val screenEvents = _screenEvents.asSharedFlow()
  val viewEventsFlow = eventsFlow.asSharedFlow()

  abstract fun initialState(): VS
  protected val stateFlow = MutableStateFlow(initialState())

  val viewStateFlow: StateFlow<VS>
    get() = stateFlow.asStateFlow()

  fun detach() {
    attachedScope.coroutineContext.cancelChildren()
  }

  fun destroy() {
    viewModelScope.cancel()
  }

  protected fun sendViewEvent(event: ViewEvent) {
    attachedScope.launch {
      eventsFlow.emitOnSubscribed(event)
    }
  }

  protected fun emitResult(result: R) {
    _screenEvents.tryEmit(result)
  }

  fun navigateBack() {
    attachedScope.launch {
      _navigateBackEvents.emitOnSubscribed(Unit)
    }
  }
}

private suspend fun <T> MutableSharedFlow<T>.emitOnSubscribed(element: T) {
  this.subscriptionCount.firstOrNull { it > 0 }
  emit(element)
}
