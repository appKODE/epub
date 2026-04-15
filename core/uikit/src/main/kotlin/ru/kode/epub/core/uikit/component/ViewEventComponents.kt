package ru.kode.epub.core.uikit.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import ru.kode.epub.core.domain.entity.TextRef
import ru.kode.epub.core.ui.compose.resolveRef
import ru.kode.epub.core.ui.screen.event.ViewEvent
import ru.kode.epub.core.ui.screen.event.ViewEventHostScope

/**
 * One time message event ui presentation. Will be dismissed by calling [ViewEventHostScope.dismissEventPresentation].
 * If you need more customization, consider overriding [ViewEvent.Snackbar] and handling it in
 * in ViewEvents of your Screen instead of modifying this standard cases dialogs, see [BaseViewModel.viewEvents]
 */
@Immutable
data class Snackbar(
  val message: TextRef,
  val actionLabel: TextRef? = null,
  val action: () -> Unit = {},
  override val isError: Boolean = false,
  override val duration: ViewEvent.Snackbar.Duration = ViewEvent.Snackbar.Duration.Short,
  override val hasAction: Boolean = actionLabel != null
) : ViewEvent.Snackbar {
  @Composable
  override fun ViewEventHostScope.Content() {
    val actionText = actionLabel?.let { resolveRef(source = it) }.orEmpty()
    val messageText = resolveRef(source = message)
    val data = remember {
      object : SnackbarData {
        override val visuals: SnackbarVisuals
          get() = object : SnackbarVisuals {
            override val actionLabel: String = actionText
            override val duration: SnackbarDuration = when (this@Snackbar.duration) {
              ViewEvent.Snackbar.Duration.Long -> SnackbarDuration.Long
              ViewEvent.Snackbar.Duration.Short -> SnackbarDuration.Short
            }
            override val message: String = messageText
            override val withDismissAction: Boolean = this@Snackbar.actionLabel != null
          }

        override fun dismiss() = dismissEventPresentation()
        override fun performAction() = action()
      }
    }
    val swipeToDismissBoxState = rememberSwipeToDismissBoxState()
    LaunchedEffect(swipeToDismissBoxState.currentValue) {
      if (swipeToDismissBoxState.currentValue != SwipeToDismissBoxValue.Settled) {
        dismissEventPresentation()
      }
    }
    SwipeToDismissBox(
      state = swipeToDismissBoxState,
      backgroundContent = {}
    ) {
      if (isError) {
        ErrorSnackbar(snackbarData = data)
      } else {
        Snackbar(snackbarData = data)
      }
    }
  }
}

/**
 * One time dialog event ui presentation. Will be dismissed by calling [ViewEventHostScope.dismissEventPresentation].
 * If you need more customization, consider overriding [ViewEvent.Content] and handling it in
 * in ViewEvents of your Screen instead of modifying this standard cases dialogs, see [BaseViewModel.viewEvents]
 */
@Immutable
sealed class Dialog : ViewEvent.Content {
  abstract val title: TextRef?
  abstract val text: TextRef?

  /**
   * Confirm dialog with two buttons. Cancellable by clicking outside.
   */
  @Immutable
  data class Confirm(
    override val title: TextRef? = null,
    override val text: TextRef? = null,
    val useConfirmNegative: Boolean = false,
    val useDismissNegative: Boolean = false,
    val dismissButtonText: TextRef,
    val confirmButtonText: TextRef,
    val onConfirm: (() -> Unit)? = null,
    val onDismiss: (() -> Unit)? = null
  ) : Dialog() {
    @Composable
    override fun ViewEventHostScope.Content() {
      AlertDialog(
        title = title?.let { resolveRef(source = it) },
        text = text?.let { resolveRef(source = it) },
        onDismissRequest = {
          onDismiss?.invoke()
          dismissEventPresentation()
        },
        onConfirmClick = {
          onConfirm?.invoke()
          dismissEventPresentation()
        },
        onDismissClick = {
          onDismiss?.invoke()
          dismissEventPresentation()
        },
        confirmButtonText = resolveRef(confirmButtonText),
        dismissButtonText = resolveRef(dismissButtonText),
        useConfirmNegative = useConfirmNegative,
        useDismissNegative = useDismissNegative
      )
    }
  }

  /**
   * Information message with a single  button. Cancellable by clicking outside.
   */
  @Immutable
  data class Info(
    override val title: TextRef,
    val buttonText: TextRef,
    override val text: TextRef? = null,
    val onButtonClick: (() -> Unit)? = null,
    val onDismiss: (() -> Unit)? = null
  ) : Dialog() {
    @Composable
    override fun ViewEventHostScope.Content() {
      AlertDialog(
        title = resolveRef(source = title),
        text = text?.let { resolveRef(source = it) },
        buttonText = resolveRef(source = buttonText),
        onDismissRequest = {
          onDismiss?.invoke()
          dismissEventPresentation()
        },
        onButtonClick = {
          onButtonClick?.invoke()
          dismissEventPresentation()
        }
      )
    }
  }

  /**
   * Information message with a single button. Not cancellable by clicking outside.
   */
  @Immutable
  data class Action(
    override val title: TextRef,
    override val text: TextRef,
    val buttonText: TextRef,
    val onButtonClick: (() -> Unit)? = null
  ) : Dialog() {
    @Composable
    override fun ViewEventHostScope.Content() {
      AlertDialog(
        title = resolveRef(source = title),
        text = resolveRef(source = text),
        buttonText = resolveRef(source = buttonText),
        onDismissRequest = {
          // non-cancellable, dismiss is not allowed, consuming it here without calling data.dismiss()
        },
        onButtonClick = {
          onButtonClick?.invoke()
          dismissEventPresentation()
        }
      )
    }
  }
}

@Immutable
data class ModalBottomSheet(val showHandle: Boolean = true, val content: @Composable ViewEventHostScope.() -> Unit) :
  ViewEvent.BottomSheet {
  @Composable
  override fun ViewEventHostScope.Content() {
    ModalBottomSheet(
      onDismissRequest = ::dismissEventPresentation,
      showHandle = showHandle
    ) {
      Column(Modifier.navigationBarsPadding()) {
        content()
      }
    }
  }
}
