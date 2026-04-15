package ru.kode.epub.core.ui.screen.event

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/**
 * One time ui event presentation, requested by screens or flows and
 * handled by [ViewEventHost]
 * Standard ui kit implementation can be found in [component.event]
 */
@Immutable
sealed interface ViewEvent {
  /**
   * Provides a means to override presentation of given view event,
   * if standard presentations are not fit to your needs
   */
  @Composable
  fun ViewEventHostScope.Content()

  @Immutable
  interface Snackbar : ViewEvent {
    val duration: Duration
    val isError: Boolean
    val hasAction: Boolean

    enum class Duration {
      Long, Short
    }
  }

  @Immutable
  interface Content : ViewEvent {
    val enter: EnterTransition
      get() = fadeIn()
    val exit: ExitTransition
      get() = fadeOut()
  }

  @Immutable
  interface BottomSheet : ViewEvent
}

@Stable
fun interface ViewEventHostScope {
  fun dismissEventPresentation()
}
