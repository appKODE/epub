package ru.kode.epub.core.uikit.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import ru.kode.epub.core.ui.screen.event.ViewEvent
import ru.kode.epub.core.ui.screen.event.ViewEventHostScope
import timber.log.Timber

/**
 * Modeled very closely to [androidx.compose.material.SnackbarHost] (except there's no auto-dismiss)
 * Check out those classes if you'll need to add support for queueing (using mutex) etc
 */
@Composable
fun DialogHost(
  hostState: DialogHostState,
  modifier: Modifier = Modifier
) {
  val currentDialogData = hostState.currentDialogData
  AnimatedContent(
    modifier = modifier.fillMaxSize(),
    targetState = currentDialogData,
    transitionSpec = {
      (targetState?.component?.enter ?: fadeIn())
        .togetherWith(initialState?.component?.exit ?: fadeOut())
        .using(sizeTransform = null)
    },
    label = "dialog host animation"
  ) { targetState ->
    val scope = remember(targetState) { ViewEventHostScope { currentDialogData?.dismiss() } }
    @Suppress("UnnecessaryApply") // apply actually cannot be replaced here without context receivers
    targetState?.component?.apply { scope.Content() }
  }
}

@Stable
class DialogHostState {
  internal var currentDialogData by mutableStateOf<DialogData?>(null)

  suspend fun showDialog(configuration: ViewEvent.Content) {
    val data = currentDialogData
    if (data != null) {
      Timber.e("Already showing a dialog for event: $data. New event $configuration will be ignored.")
    } else {
      try {
        return suspendCancellableCoroutine { continuation ->
          currentDialogData = DialogData(
            component = configuration,
            continuation = continuation
          )
        }
      } finally {
        currentDialogData = null
      }
    }
  }
}

@Stable
internal class DialogData(
  val component: ViewEvent.Content,
  private val continuation: CancellableContinuation<Unit>
) {
  fun dismiss() {
    if (continuation.isActive) continuation.resume(Unit, onCancellation = null)
  }
}
