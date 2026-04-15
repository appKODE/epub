package ru.kode.epub.core.ui.screen.event

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import me.tatarka.inject.annotations.Inject
import ru.kode.epub.core.domain.di.ForegroundScope
import ru.kode.epub.core.domain.di.SingleIn

/**
 * Wrapper class over ViewEvents stream rendered by root [ViewEventsHost], usually bound to application scope.
 * Primary use case is calling indirectly by [BaseViewModel.sendViewEvent],
 * but generally it can be injected by any component that needs to fire one time view events
 */
@Stable
@SingleIn(ForegroundScope::class)
class ViewEventsHostMediator @Inject constructor() {
  private val _events = MutableSharedFlow<ViewEvent>(extraBufferCapacity = 3)
  val events: Flow<ViewEvent> = _events

  fun sendViewEvent(event: ViewEvent) {
    _events.tryEmit(event)
  }
}

val LocalViewEventsHostMediator = staticCompositionLocalOf<ViewEventsHostMediator> {
  error("No ViewEventsHostMediator provided")
}

@Composable
fun rememberViewEventsMediator() = remember { ViewEventsHostMediator() }
