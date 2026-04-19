package ru.kode.epub.feature.reader.ui.reader.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import ru.kode.epub.core.uikit.R
import ru.kode.epub.core.uikit.theme.AppTheme
import ru.kode.epub.feature.reader.ui.reader.BookInfo

@Composable
internal fun ReaderTopBar(
  visible: Boolean,
  showTocButton: Boolean,
  bookInfo: BookInfo,
  onBackClick: () -> Unit,
  onShowBookInfoClick: () -> Unit,
  onShowTocClick: () -> Unit
) {
  AnimatedVisibility(
    visible = visible,
    enter = slideInVertically { -it },
    exit = slideOutVertically { -it }
  ) {
    TopAppBar(
      colors = TopAppBarDefaults.topAppBarColors(
        containerColor = AppTheme.colors.surfaceBackground
      ),
      title = {
        Column {
          Text(
            text = bookInfo.title,
            style = AppTheme.typography.headline5,
            color = AppTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          if (bookInfo.author.isNotBlank()) {
            Text(
              text = bookInfo.author,
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
        if (showTocButton) {
          IconButton(onClick = onShowTocClick) {
            Icon(
              painter = painterResource(R.drawable.ic_list_24),
              contentDescription = null,
              tint = AppTheme.colors.iconPrimary
            )
          }
        }
      },
      windowInsets = WindowInsets.statusBars.union(
        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
      )
    )
  }
}
