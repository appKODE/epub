package ru.kode.epub.feature.reader.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.kode.epub.core.ui.compose.modifiers.borderTop
import ru.kode.epub.core.ui.compose.modifiers.cutoutPadding
import ru.kode.epub.core.ui.compose.modifiers.surface
import ru.kode.epub.core.ui.compose.resolveRef
import ru.kode.epub.core.ui.screen.AppScreen
import ru.kode.epub.core.uikit.component.DropdownField
import ru.kode.epub.core.uikit.component.TopAppBar
import ru.kode.epub.core.uikit.compose.itemsIndexedWithShape
import ru.kode.epub.core.uikit.theme.AppTheme
import ru.kode.epub.feature.reader.domain.entity.ColumnMode
import ru.kode.epub.feature.reader.domain.entity.NightMode
import ru.kode.epub.feature.reader.domain.entity.PageScrollMode
import ru.kode.epub.feature.reader.domain.entity.ReaderSettings
import ru.kode.epub.feature.reader.ui.R
import ru.kode.epub.feature.reader.ui.component.NightModeListItem

@Composable
fun SettingsScreen(
  viewModel: SettingsViewModel
) = AppScreen(viewModel) { state ->
  Column(
    modifier = Modifier
      .cutoutPadding()
      .statusBarsPadding()
      .background(color = AppTheme.colors.surfaceBackground)
      .fillMaxSize()
  ) {
    TopAppBar(title = stringResource(id = R.string.settings_screen_title))
    LazyColumn(
      modifier = Modifier
        .fillMaxWidth(),
      contentPadding = PaddingValues(16.dp)
    ) {
      state.settings.forEach { settings ->
        when (settings.selected) {
          is NightMode -> nightMode(
            settings = settings,
            onSelect = { viewModel.apply(settings.copy(selected = it)) }
          )

          is ColumnMode,
          is PageScrollMode -> dropDownMenu(
            settings = settings,
            onSelect = { viewModel.apply(settings.copy(selected = it)) }
          )

          else -> Unit
        }
      }
    }
  }
}

private fun LazyListScope.dropDownMenu(
  settings: ReaderSettings,
  onSelect: (ReaderSettings.Entry) -> Unit
) {
  item {
    DropdownField(
      modifier = Modifier.padding(top = 8.dp),
      options = settings.available,
      selected = settings.selected ?: settings.defaultSelected(),
      onSelect = onSelect,
      title = { entry -> resolveRef(entry.displayName) },
      label = resolveRef(settings.name),
      hint = settings.description?.let { resolveRef(it) }.orEmpty()
    )
  }
}

private fun LazyListScope.nightMode(
  settings: ReaderSettings,
  onSelect: (ReaderSettings.Entry) -> Unit
) {
  stickyHeader {
    Text(
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp),
      text = stringResource(R.string.settings_theme),
      style = AppTheme.typography.subhead2,
      color = AppTheme.colors.textSecondary
    )
  }
  itemsIndexedWithShape(
    items = NightMode.entries,
    shape = RoundedCornerShape(16.dp)
  ) { shape, index, theme ->
    NightModeListItem(
      modifier = Modifier
        .fillParentMaxWidth()
        .surface(
          backgroundColor = AppTheme.colors.surfaceLayer1,
          shape = shape,
          onClick = { onSelect(theme) }
        )
        .borderTop(
          strokeWidth = 1.dp,
          color = AppTheme.colors.borderRegular,
          alpha = remember(index) { if (index == 0) 0f else 1f },
          startIndent = 16.dp
        ),
      nightMode = theme,
      selected = theme == settings.selected
    )
  }
}
