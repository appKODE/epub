package ru.kode.epub.core.uikit.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import ru.kode.epub.core.uikit.component.preview.PreviewColumn
import ru.kode.epub.core.uikit.component.preview.ThemePreviews
import ru.kode.epub.core.uikit.theme.AppTheme

@Composable
fun Snackbar(
  snackbarData: SnackbarData,
  modifier: Modifier = Modifier,
  actionOnNewLine: Boolean = false,
  shape: Shape = RoundedCornerShape(12.dp),
  backgroundColor: Color = AppTheme.colors.surfaceLayerContrast,
  contentColor: Color = AppTheme.colors.textContrastPrimary,
  actionColor: Color = AppTheme.colors.textContrastPrimary,
  actionContentColor: Color = AppTheme.colors.textContrastPrimary,
  dismissActionContentColor: Color = AppTheme.colors.textContrastPrimary
) {
  Snackbar(
    snackbarData = snackbarData,
    modifier = modifier,
    actionOnNewLine = actionOnNewLine,
    shape = shape,
    containerColor = backgroundColor,
    contentColor = contentColor,
    actionColor = actionColor,
    actionContentColor = actionContentColor,
    dismissActionContentColor = dismissActionContentColor
  )
}

@Composable
fun ErrorSnackbar(
  snackbarData: SnackbarData,
  modifier: Modifier = Modifier,
  actionOnNewLine: Boolean = false,
  shape: Shape = RoundedCornerShape(12.dp),
  backgroundColor: Color = AppTheme.colors.surfaceLayerNegative,
  contentColor: Color = AppTheme.colors.textOnNegative,
  actionColor: Color = AppTheme.colors.textOnNegative
) {
  Snackbar(
    snackbarData,
    modifier,
    actionOnNewLine,
    shape,
    backgroundColor,
    contentColor,
    actionColor
  )
}

@ThemePreviews
@Composable
private fun SnackbarsPreview() {
  PreviewColumn {
    val data = object : SnackbarData {
      override val visuals: SnackbarVisuals
        get() = object : SnackbarVisuals {
          override val actionLabel: String? = "Action"
          override val duration: SnackbarDuration = SnackbarDuration.Indefinite
          override val message: String = "Aloha! I'm glad to see ya"
          override val withDismissAction: Boolean = false
        }

      override fun dismiss() = Unit
      override fun performAction() = Unit
    }
    Snackbar(snackbarData = data)
    ErrorSnackbar(snackbarData = data)
  }
}
