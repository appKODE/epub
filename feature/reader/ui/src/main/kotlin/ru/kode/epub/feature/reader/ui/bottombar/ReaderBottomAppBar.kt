package ru.kode.epub.feature.reader.ui.bottombar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.kode.epub.core.domain.entity.ScreenOrientation
import ru.kode.epub.core.domain.entity.resRef
import ru.kode.epub.core.ui.compose.LocalScreenOrientation
import ru.kode.epub.core.ui.compose.modifiers.cutoutPadding
import ru.kode.epub.core.ui.compose.modifiers.navigationBarsPadding
import ru.kode.epub.core.uikit.component.HorizontalBottomAppBar
import ru.kode.epub.core.uikit.component.Tab
import ru.kode.epub.core.uikit.component.VerticalBottomAppBar
import ru.kode.epub.core.uikit.theme.AppTheme
import ru.kode.epub.feature.reader.ui.R
import ru.kode.epub.core.uikit.R as UiKitR

val LocalBottomBarController = staticCompositionLocalOf<BottomBarController> {
  error("No BottomBarController provided")
}

@Composable
fun ReaderBottomAppBar(
  controller: BottomBarController,
  modifier: Modifier = Modifier
) {
  val state by controller.state().collectAsState(BottomBarState())
  val isVisible by controller.isVisible().collectAsState(false)

  MainBottomAppBar(
    state = state,
    isVisible = isVisible,
    modifier = modifier,
    onSectionClick = { controller.reportSectionClick(it) }
  )
}

@Composable
fun MainBottomAppBar(
  state: BottomBarState,
  isVisible: Boolean,
  onSectionClick: (BottomBarSection) -> Unit,
  modifier: Modifier = Modifier
) {
  val orientation = LocalScreenOrientation.current
  AnimatedVisibility(
    modifier = modifier,
    visible = isVisible,
    enter = if (orientation == ScreenOrientation.Portrait) slideInVertically { it } else fadeIn(),
    exit = if (orientation == ScreenOrientation.Portrait) slideOutVertically { it } else fadeOut()
  ) {
    val tabs = BottomBarSection.entries.map { section ->
      Tab(
        id = section.id,
        iconResId = section.getIconResId(),
        titleRef = resRef(section.titleResId),
        isActive = section.isActive(state)
      )
    }
    when (orientation) {
      ScreenOrientation.Portrait -> {
        Box(
          modifier = Modifier
            .background(color = AppTheme.colors.surfaceLayer1)
            .navigationBarsPadding()
        ) {
          HorizontalBottomAppBar(
            tabs = tabs,
            onTabClick = { onSectionClick(it.section) }
          )
        }
      }

      ScreenOrientation.Landscape -> {
        VerticalBottomAppBar(
          modifier = Modifier
            .statusBarsPadding()
            .navigationBarsPadding(start = false, bottom = true, end = true)
            .cutoutPadding(start = false, top = false, bottom = false, end = true)
            .padding(top = 8.dp, end = 16.dp, bottom = 16.dp),
          tabs = tabs.reversed(),
          onTabClick = { onSectionClick(it.section) }
        )
      }
    }
  }
}

private fun BottomBarSection.isActive(state: BottomBarState): Boolean =
  this == state.selectedSection

private val Tab.Id.section: BottomBarSection
  get() {
    return when (this.value) {
      BottomBarSection.Recent.name -> BottomBarSection.Recent
      BottomBarSection.Settings.name -> BottomBarSection.Settings
      else -> error("can't find section for id: ${this.value}")
    }
  }

private val BottomBarSection.id: Tab.Id
  get() = Tab.Id(this.name)

private fun BottomBarSection.getIconResId(): Int {
  return when (this) {
    BottomBarSection.Recent -> UiKitR.drawable.ic_book_24
    BottomBarSection.Settings -> UiKitR.drawable.ic_settings_24
  }
}

private val BottomBarSection.titleResId: Int
  get() {
    return when (this) {
      BottomBarSection.Recent -> R.string.reader_recent_tab
      BottomBarSection.Settings -> R.string.reader_settings_tab
    }
  }

@Composable
fun BottomBarStateRestoreEffect(visible: Boolean) {
  val controller = LocalBottomBarController.current
  DisposableEffect(Unit) {
    val stateId = if (visible) controller.show() else controller.hide()
    onDispose { controller.popVisibilityState(stateId) }
  }
}
