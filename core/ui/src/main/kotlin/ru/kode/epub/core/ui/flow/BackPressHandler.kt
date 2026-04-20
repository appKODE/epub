package ru.kode.epub.core.ui.flow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.backhandler.BackHandler

val LocalDecomposeBackHandler = compositionLocalOf<BackHandler> {
  object : BackHandler {
    override fun isRegistered(callback: BackCallback): Boolean = false
    override fun register(callback: BackCallback) = Unit
    override fun unregister(callback: BackCallback) = Unit
  }
}

@Composable
fun BackPressedHandler(
  onBack: (() -> Unit)? = null,
  enabled: Boolean = true
) {
  val handler = LocalDecomposeBackHandler.current

  val callback = remember {
    onBack?.let { BackCallback(isEnabled = enabled) { onBack() } }
  }

  SideEffect { callback?.isEnabled = enabled }

  DisposableEffect(handler) {
    callback?.let(handler::register)
    onDispose { callback?.let(handler::unregister) }
  }
}
