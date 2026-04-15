package ru.kode.epub.core.uikit.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ru.kode.epub.core.uikit.R
import ru.kode.epub.core.uikit.theme.AppTheme

@Composable
fun CloseIcon(onClick: () -> Unit, modifier: Modifier = Modifier) {
  IconButton(
    onClick = onClick,
    modifier = modifier
  ) {
    Icon(
      painter = painterResource(id = R.drawable.ic_close_24),
      contentDescription = "close",
      tint = AppTheme.colors.iconPrimary,
      modifier = Modifier
        .background(AppTheme.colors.surfaceLayerTranslucent, CircleShape)
        .padding(4.dp)
    )
  }
}
