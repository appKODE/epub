package ru.kode.epub.feature.reader.routing

import android.net.Uri
import androidx.compose.runtime.Stable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.bringToFront
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ru.kode.epub.core.routing.FlowComponentContext
import ru.kode.epub.core.routing.FlowNavigationComponent
import ru.kode.epub.core.routing.Node
import ru.kode.epub.core.routing.Screen
import ru.kode.epub.core.routing.viewModel
import ru.kode.epub.feature.reader.domain.entity.Book
import ru.kode.epub.feature.reader.routing.ReaderNavigationComponent.Child
import ru.kode.epub.feature.reader.routing.ReaderNavigationComponent.Config
import ru.kode.epub.feature.reader.ui.reader.ReaderViewModel
import ru.kode.epub.feature.reader.ui.recent.RecentBooksResult
import ru.kode.epub.feature.reader.ui.recent.RecentBooksViewModel
import ru.kode.epub.feature.reader.ui.settings.SettingsViewModel

@Stable
class ReaderNavigationComponent(
  context: FlowComponentContext,
  val params: ReaderFlowParams = ReaderFlowParams.Recent,
  val component: ReaderFlowComponent,
  val onFinish: () -> Unit
) : FlowNavigationComponent<Config, Child>(context) {

  val model = component.model

  override val scope = component.coroutineScope()
  override fun initialConfig(): List<Config> = listOf(Config.Recent)
  override fun onDismiss() = onFinish()

  override fun onCreate() {
    super.onCreate()
    when (params) {
      is ReaderFlowParams.Book -> model.readEpub(params.uri)
      is ReaderFlowParams.Recent -> Unit
    }
    model.epubReads
      .onEach { book -> navigate { bringToFront(Config.Reader(book)) } }
      .launchIn(scope)
  }

  override fun onScreenResult(result: Any?) {
    when (result) {
      is RecentBooksResult -> onRecentsResults(result)
    }
  }

  private fun onRecentsResults(result: RecentBooksResult) {
    when (result) {
      is RecentBooksResult.Reader -> navigate { bringToFront(Config.Reader(result.book)) }
      is RecentBooksResult.Settings -> navigate { bringToFront(Config.Settings) }
    }
  }

  override val childFactory: (Config, ComponentContext) -> Child = { config, context ->
    when (config) {
      is Config.Reader -> Child.Reader(component.viewModel(config.book))
      is Config.Recent -> Child.Recent(component.viewModel())
      is Config.Settings -> Child.Settings(component.viewModel())
    }
  }

  sealed interface Config {
    data object Recent : Config
    data object Settings : Config
    data class Reader(val book: Book) : Config
  }

  sealed interface Child : Node {
    data class Recent(override val viewModel: RecentBooksViewModel) : Child, Screen
    data class Settings(override val viewModel: SettingsViewModel) : Child, Screen
    data class Reader(override val viewModel: ReaderViewModel) : Child, Screen
  }
}

sealed interface ReaderFlowParams {
  data object Recent : ReaderFlowParams
  data class Book(val uri: Uri) : ReaderFlowParams
}
