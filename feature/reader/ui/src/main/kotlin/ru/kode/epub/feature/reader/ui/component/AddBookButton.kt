package ru.kode.epub.feature.reader.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ru.kode.epub.core.ui.compose.modifiers.surface
import ru.kode.epub.core.uikit.R
import ru.kode.epub.core.uikit.theme.AppTheme

@Composable
internal fun AddBookButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .surface(
        backgroundColor = AppTheme.colors.surfaceLayerAccent,
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
      )
      .padding(16.dp),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      painter = painterResource(R.drawable.ic_plus_32),
      tint = AppTheme.colors.iconContrastPrimary,
      contentDescription = "plus icon"
    )
  }
}
