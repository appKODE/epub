package ru.kode.epub.feature.reader.routing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import ru.kode.epub.core.ui.flow.LocalDecomposeBackHandler
import ru.kode.epub.feature.reader.routing.ReaderNavigationComponent.Child
import ru.kode.epub.feature.reader.ui.reader.ReaderScreen
import ru.kode.epub.feature.reader.ui.recent.RecentBooksScreen
import ru.kode.epub.feature.reader.ui.settings.SettingsScreen

@Composable
fun ReaderFlow(
  component: ReaderNavigationComponent
) {
  LaunchedEffect(component) { component.onCreate() }

  CompositionLocalProvider(
    LocalDecomposeBackHandler provides component.backHandler
  ) {
    Children(
      stack = component.stack,
      animation = stackAnimation(slide()),
      content = { child ->
        when (val instance = child.instance) {
          is Child.Reader -> ReaderScreen(instance.viewModel)
          is Child.Recent -> RecentBooksScreen(instance.viewModel)
          is Child.Settings -> SettingsScreen(instance.viewModel)
        }
      }
    )
  }
}
