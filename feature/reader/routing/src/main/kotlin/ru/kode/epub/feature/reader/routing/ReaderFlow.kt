package ru.kode.epub.feature.reader.routing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import ru.kode.epub.core.domain.entity.ScreenOrientation
import ru.kode.epub.core.ui.compose.LocalScreenOrientation
import ru.kode.epub.core.ui.flow.LocalDecomposeBackHandler
import ru.kode.epub.core.uikit.component.CircularLoaderWithOverlay
import ru.kode.epub.feature.reader.routing.ReaderNavigationComponent.Child
import ru.kode.epub.feature.reader.ui.bottombar.BottomBarController
import ru.kode.epub.feature.reader.ui.bottombar.LocalBottomBarController
import ru.kode.epub.feature.reader.ui.bottombar.ReaderBottomAppBar
import ru.kode.epub.feature.reader.ui.reader.ReaderScreen
import ru.kode.epub.feature.reader.ui.recent.RecentBooksScreen
import ru.kode.epub.feature.reader.ui.settings.SettingsScreen

@Composable
fun ReaderFlow(
  component: ReaderNavigationComponent
) {
  LaunchedEffect(component) { component.onCreate() }
  ReaderFlowContent(component.bottomBarController) {
    CompositionLocalProvider(
      LocalBottomBarController provides component.bottomBarController,
      LocalDecomposeBackHandler provides component.backHandler
    ) {
      Children(
        stack = component.stack,
        animation = predictiveBackAnimation(
          backHandler = component.backHandler,
          fallbackAnimation = stackAnimation(slide()),
          onBack = component::handleBack
        ),
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
  val importInProgress by component.epubImportInProgress.collectAsState(false)
  AnimatedVisibility(
    visible = importInProgress,
    enter = fadeIn(),
    exit = fadeOut()
  ) {
    CircularLoaderWithOverlay(modifier = Modifier.fillMaxSize())
  }
}

@Composable
fun ReaderFlowContent(
  bottomBarController: BottomBarController,
  content: @Composable () -> Unit
) {
  val orientation = LocalScreenOrientation.current
  when (orientation) {
    ScreenOrientation.Portrait -> Column {
      Box(modifier = Modifier.weight(1f)) {
        content()
      }
      ReaderBottomAppBar(
        controller = bottomBarController
      )
    }

    ScreenOrientation.Landscape -> Row {
      Box(modifier = Modifier.weight(1f)) {
        content()
      }
      ReaderBottomAppBar(
        controller = bottomBarController
      )
    }
  }
}
