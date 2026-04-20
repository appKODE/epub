package ru.kode.epub.feature.reader.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.kode.epub.core.domain.entity.TextRef
import ru.kode.epub.core.domain.entity.resRef
import ru.kode.epub.core.ui.compose.resolveRef
import ru.kode.epub.core.uikit.theme.AppTheme
import ru.kode.epub.feature.reader.domain.entity.NightMode
import ru.kode.epub.feature.reader.ui.R
import ru.kode.epub.core.uikit.R as UiKitR

@Composable
internal fun NightModeListItem(
  nightMode: NightMode,
  selected: Boolean,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      modifier = Modifier.size(24.dp),
      painter = painterResource(id = nightMode.iconRes),
      contentDescription = nightMode.iconContentDescription,
      tint = AppTheme.colors.iconTertiary
    )
    Text(
      modifier = Modifier.weight(1f),
      text = resolveRef(nightMode.displayValue),
      style = AppTheme.typography.body1,
      color = AppTheme.colors.textPrimary
    )
    if (selected) {
      Icon(
        modifier = Modifier.size(24.dp),
        painter = painterResource(id = UiKitR.drawable.ic_checkmark_24),
        contentDescription = stringResource(R.string.settings_checkmark_description),
        tint = AppTheme.colors.iconAccent
      )
    } else {
      // Reserve space if not selected to prevent content from adjustment when selected
      Box(modifier = Modifier.size(24.dp))
    }
  }
}

internal val NightMode.displayValue: TextRef
  get() = when (this) {
    NightMode.Day -> resRef(R.string.settings_theme_light)
    NightMode.Night -> resRef(R.string.settings_theme_dark)
    NightMode.Auto -> resRef(R.string.settings_theme_system)
  }

internal val NightMode.iconRes: Int
  @DrawableRes get() = when (this) {
    NightMode.Day -> UiKitR.drawable.ic_sun_24
    NightMode.Night -> UiKitR.drawable.ic_night_24
    NightMode.Auto -> UiKitR.drawable.ic_mobile_24
  }

private val NightMode.iconContentDescription: String
  @Composable
  get() = when (this) {
    NightMode.Day -> stringResource(R.string.settings_day_mode_description)
    NightMode.Night -> stringResource(R.string.settings_night_mode_description)
    NightMode.Auto -> stringResource(R.string.settings_system_mode_description)
  }
