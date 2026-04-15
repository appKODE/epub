package ru.kode.epub.core.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import ru.kode.epub.core.ui.flow.BackPressedHandler
import ru.kode.epub.core.ui.screen.event.LocalViewEventsHostMediator

@Composable
inline fun <S : Any> AppScreen(
  viewModel: ViewModel<S, *>,
  noinline onBackPress: (() -> Unit)? = null,
  gestureNavigationEnabled: Boolean = true,
  content: @Composable (state: S) -> Unit
) {
  val scope = rememberCoroutineScope()
  val state by viewModel.viewStateFlow.collectAsState(scope.coroutineContext)
  val viewEventsHostController = LocalViewEventsHostMediator.current
  BackPressedHandler(
    onBack = onBackPress?.let { { onBackPress() } } ?: { viewModel.navigateBack() },
    enabled = gestureNavigationEnabled
  )
  LaunchedEffect(viewModel) {
    viewModel.viewEventsFlow
      .onEach { event -> viewEventsHostController.sendViewEvent(event) }
      .collect()
  }
  content(state)
}
