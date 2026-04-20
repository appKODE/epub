package ru.kode.epub.feature.reader.routing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateBounds
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
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.layout
import com.arkivanov.decompose.Child.Created
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.Direction
import com.arkivanov.decompose.extensions.compose.stack.animation.StackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.scale
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimator
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
        animation = readerFlowAnimation(),
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
  LookaheadScope {
    when (orientation) {
      ScreenOrientation.Portrait -> Column {
        Box(
          modifier = Modifier
            .weight(1f)
            .animateBounds(this@LookaheadScope),
          content = { content() }
        )
        ReaderBottomAppBar(bottomBarController)
      }

      ScreenOrientation.Landscape -> Row {
        Box(
          modifier = Modifier
            .weight(1f)
            .animateBounds(this@LookaheadScope),
          content = { content() }
        )
        ReaderBottomAppBar(bottomBarController)
      }
    }
  }
}

private fun readerFlowAnimation(): StackAnimation<Any, Child> = stackAnimation(
  selector = { from: Created<*, Child>, to: Created<*, Child>, direction: Direction ->
    if (from.instance is Child.Reader || to.instance is Child.Reader) {
      stack()
    } else when (from.instance) {
      is Child.Recent -> when (direction) {
        Direction.ENTER_FRONT -> slideOut()
        Direction.EXIT_FRONT -> slideOut()
        Direction.ENTER_BACK -> slideIn()
        Direction.EXIT_BACK -> slideIn()
      }

      is Child.Settings -> when (direction) {
        Direction.ENTER_FRONT -> slideIn()
        Direction.EXIT_FRONT -> slideIn()
        Direction.ENTER_BACK -> slideOut()
        Direction.EXIT_BACK -> slideOut()
      }

      else -> stack()
    }
  }
)

private fun stack() = slide() + scale()
private fun slideIn() = stackAnimator { factor, _, content -> content(Modifier.offsetXFactor(factor)) }
private fun slideOut() = stackAnimator { factor, _, content -> content(Modifier.offsetXFactor(-factor)) }

private fun Modifier.offsetXFactor(factor: Float): Modifier =
  layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)

    layout(placeable.width, placeable.height) {
      placeable.placeRelative(x = (placeable.width.toFloat() * factor).toInt(), y = 0)
    }
  }
