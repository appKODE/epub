package ru.kode.epub.feature.reader.ui.reader.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.kode.epub.core.ui.compose.modifiers.borderBottom
import ru.kode.epub.core.uikit.R
import ru.kode.epub.core.uikit.theme.AppTheme
import ru.kode.epub.lib.entity.Metadata

@Composable
internal fun ReaderTopBar(
  visible: Boolean,
  metadata: Metadata,
  onBackClick: () -> Unit,
  onShowBookInfoClick: () -> Unit,
  onShowBookTocClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  AnimatedVisibility(
    visible = visible,
    enter = slideInVertically { -it },
    exit = slideOutVertically { -it },
    modifier = modifier
  ) {
    TopAppBar(
      modifier = Modifier.borderBottom(strokeWidth = 1.dp, color = AppTheme.colors.borderRegular),
      title = {
        Column {
          Text(
            text = metadata.title,
            style = AppTheme.typography.headline5,
            color = AppTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          if (metadata.author.isNotBlank()) {
            Text(
              text = metadata.author,
              style = AppTheme.typography.body2,
              color = AppTheme.colors.textSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      },
      navigationIcon = {
        IconButton(onClick = onBackClick) {
          Icon(
            painter = painterResource(R.drawable.ic_arrow_backward_24),
            contentDescription = null,
            tint = AppTheme.colors.iconPrimary
          )
        }
      },
      actions = {
        IconButton(onClick = onShowBookInfoClick) {
          Icon(
            painter = painterResource(R.drawable.ic_info_24),
            contentDescription = null,
            tint = AppTheme.colors.iconPrimary
          )
        }
        IconButton(onClick = onShowBookTocClick) {
          Icon(
            painter = painterResource(R.drawable.ic_list_24),
            contentDescription = null,
            tint = AppTheme.colors.iconPrimary
          )
        }
      },
      colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colors.surfaceBackground)
    )
  }
}
