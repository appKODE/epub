package ru.kode.epub.core.uikit.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AccessibilityManager
import androidx.compose.ui.platform.LocalAccessibilityManager
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.kode.epub.core.ui.screen.event.ViewEvent
import ru.kode.epub.core.ui.screen.event.ViewEventHostScope
import kotlin.coroutines.resume

/**
 * Modified version of [androidx.compose.material.SnackbarHost], with support for custom snackbars
 */
@Composable
fun SnackbarHost(
  hostState: SnackbarHostState,
  modifier: Modifier = Modifier
) {
  val currentSnackbarData = hostState.currentSnackbarData
  val accessibilityManager = LocalAccessibilityManager.current
  LaunchedEffect(currentSnackbarData) {
    if (currentSnackbarData != null) {
      delay(currentSnackbarData.component.durationMillis(accessibilityManager))
      currentSnackbarData.dismiss()
    }
  }
  AnimatedVisibility(
    modifier = modifier,
    visible = currentSnackbarData != null,
    enter = slideInVertically(),
    exit = slideOutVertically()
  ) {
    val scope = remember(currentSnackbarData) { ViewEventHostScope { currentSnackbarData?.dismiss() } }
    @Suppress("UnnecessaryApply") // apply actually cannot be replaced here without context receivers
    currentSnackbarData?.component?.apply { scope.Content() }
  }
}

@Stable
class SnackbarHostState {
  private val mutex = Mutex()

  internal var currentSnackbarData by mutableStateOf<SnackbarData?>(null)
    private set

  suspend fun showSnackbar(
    configuration: ViewEvent.Snackbar
  ): SnackbarResult = mutex.withLock {
    try {
      return suspendCancellableCoroutine { continuation ->
        currentSnackbarData = SnackbarData(configuration, continuation)
      }
    } finally {
      currentSnackbarData = null
      // we need to give the system time for the snackbar to close
      // without this delay, the new snackbar will be displayed with the data of the previous snackbar
      delay(150)
    }
  }
}

@Stable
internal class SnackbarData(
  val component: ViewEvent.Snackbar,
  private val continuation: CancellableContinuation<SnackbarResult>
) {
  fun dismiss() {
    if (continuation.isActive) continuation.resume(SnackbarResult.Dismissed)
  }
}

private fun ViewEvent.Snackbar.durationMillis(
  accessibilityManager: AccessibilityManager?
): Long {
  val original = when (duration) {
    ViewEvent.Snackbar.Duration.Long -> 30_000L
    ViewEvent.Snackbar.Duration.Short -> 5_000L
  }
  if (accessibilityManager == null) {
    return original
  }
  return accessibilityManager.calculateRecommendedTimeoutMillis(
    original,
    containsIcons = true,
    containsText = true,
    containsControls = hasAction
  )
}
