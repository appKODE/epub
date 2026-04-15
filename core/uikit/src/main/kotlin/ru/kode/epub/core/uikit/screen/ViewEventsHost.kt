package ru.kode.epub.core.uikit.screen

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ru.kode.epub.core.ui.screen.event.ViewEvent
import kotlin.math.roundToInt

@Composable
fun ViewEventsHost(
  configurationsFlow: Flow<ViewEvent>,
  modifier: Modifier = Modifier,
  resetEvents: Flow<Unit> = emptyFlow()
) {
  val snackbarHostState = remember { SnackbarHostState() }
  val errorSnackbarHostState = remember { SnackbarHostState() }
  val dialogHostState = remember { DialogHostState() }
  val bottomSheetHostState = remember { BottomSheetHostState() }
  val scope = rememberCoroutineScope()
  var dialogJob: Job? = null
  var bottomSheetJob: Job? = null
  var snackJob: Job? = null

  LaunchedEffect(Unit) {
    resetEvents
      .onEach {
        dialogJob?.cancel()
        bottomSheetJob?.cancel()
        snackJob?.cancel()
      }
      .launchIn(scope)

    configurationsFlow.filterIsInstance<ViewEvent.Snackbar>()
      .onEach { snackbarConfiguration ->
        snackJob = scope.launch {
          if (errorSnackbarHostState.currentSnackbarData != null) {
            errorSnackbarHostState.currentSnackbarData!!.dismiss()
          }

          if (snackbarHostState.currentSnackbarData != null) {
            snackbarHostState.currentSnackbarData!!.dismiss()
          }

          if (snackbarConfiguration.isError) {
            errorSnackbarHostState.showSnackbar(snackbarConfiguration)
          } else {
            snackbarHostState.showSnackbar(snackbarConfiguration)
          }
        }
      }
      .launchIn(scope)

    configurationsFlow.filterIsInstance<ViewEvent.Content>()
      .onEach { dialogConfiguration ->
        // dialogHostState should decide how to handle overflow errors by itself,
        // without external Cancellation or waiting for a collector to finish processing
        dialogJob = scope.launch { dialogHostState.showDialog(dialogConfiguration) }
      }
      .launchIn(scope)

    configurationsFlow.filterIsInstance<ViewEvent.BottomSheet>()
      .onEach { bottomSheetConfiguration ->
        bottomSheetJob = scope.launch { bottomSheetHostState.showBottomSheet(bottomSheetConfiguration) }
      }
      .launchIn(scope)
  }

  Box(
    modifier = modifier
      .systemBarsPadding()
      .displayCutoutPadding()
      .imePadding()
  ) {
    BottomSheetHost(
      modifier = Modifier.align(Alignment.TopCenter),
      hostState = bottomSheetHostState
    )

    DialogHost(hostState = dialogHostState)

    SwipeDismissSnackbar(errorSnackbarHostState) {
      errorSnackbarHostState.currentSnackbarData?.dismiss()
    }

    SwipeDismissSnackbar(snackbarHostState) {
      snackbarHostState.currentSnackbarData?.dismiss()
    }
  }
}

@Composable
private fun SwipeDismissSnackbar(
  snackbarHostState: SnackbarHostState,
  onDismiss: () -> Unit
) {
  var offsetY by remember { mutableStateOf(0f) }

  Box(
    modifier = Modifier
      .offset { IntOffset(0, offsetY.roundToInt()) }
      .pointerInput(Unit) {
        detectVerticalDragGestures(
          onVerticalDrag = { change, dragAmount ->
            val newOffset = offsetY + dragAmount
            offsetY = newOffset.coerceAtMost(0f)
            change.consume()
          },
          onDragEnd = {
            if (offsetY < -150f) {
              onDismiss()
            }
            offsetY = 0f
          }
        )
      }
  ) {
    SnackbarHost(
      hostState = snackbarHostState
    )
  }
}
