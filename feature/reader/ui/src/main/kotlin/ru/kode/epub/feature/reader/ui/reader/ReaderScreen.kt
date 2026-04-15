package ru.kode.epub.feature.reader.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ru.kode.epub.core.ui.screen.AppScreen
import ru.kode.epub.core.uikit.R
import ru.kode.epub.core.uikit.theme.AppTheme

@Composable
fun ReaderScreen(
  viewModel: ReaderViewModel
) = AppScreen(viewModel) { state ->
  val fontFamily = rememberEpubFontFamily(state.fontFiles)
  val listState = rememberLazyListState()

  LaunchedEffect(state.scrollToElementIndex) {
    state.scrollToElementIndex?.let { index ->
      listState.scrollToItem(index)
      viewModel.onScrollHandled()
    }
  }

  CompositionLocalProvider(LocalEpubFontFamily provides fontFamily) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(AppTheme.colors.surfaceBackground)
        .pointerInput(Unit) {
          detectTapGestures { viewModel.toggleTopBar() }
        }
    ) {
      val topBarHeight = 64.dp
      val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
      val fixedTopPadding = statusBarPadding + topBarHeight + 8.dp

      LazyColumn(
        state = listState,
        contentPadding = PaddingValues(top = fixedTopPadding, bottom = 32.dp)
      ) {
        items(state.elements, key = { it.key }) { indexed ->
          ContentItem(indexed.element)
        }
      }

      AnimatedVisibility(
        visible = state.isTopBarVisible,
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        TopAppBar(
          title = {
            Text(
              text = state.bookInfo.title,
              color = AppTheme.colors.textPrimary
            )
          },
          navigationIcon = {
            IconButton(onClick = viewModel::navigateBack) {
              Icon(
                painter = painterResource(R.drawable.ic_arrow_backward_24),
                contentDescription = null,
                tint = AppTheme.colors.iconPrimary
              )
            }
          },
          actions = {
            IconButton(onClick = viewModel::showBookInfo) {
              Icon(
                painter = painterResource(R.drawable.ic_info_24),
                contentDescription = null,
                tint = AppTheme.colors.iconPrimary
              )
            }
            IconButton(onClick = viewModel::showToc) {
              Icon(
                painter = painterResource(R.drawable.ic_list_24),
                contentDescription = null,
                tint = AppTheme.colors.iconPrimary
              )
            }
          },
          windowInsets = WindowInsets.statusBars
        )
      }
    }
  }
}
