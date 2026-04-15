package ru.kode.epub.core.uikit.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.kode.epub.core.ui.compose.modifiers.surface
import ru.kode.epub.core.uikit.component.preview.PreviewColumn
import ru.kode.epub.core.uikit.component.preview.ThemePreviews
import ru.kode.epub.core.uikit.theme.AppTheme

/**
 * Alert dialog with one button
 */
@Composable
fun AlertDialog(
  onDismissRequest: () -> Unit,
  buttonText: String,
  onButtonClick: () -> Unit,
  text: String? = null,
  title: String? = null
) {
  AlertDialog(
    onDismissRequest = onDismissRequest,
    confirmButton = { Button(text = buttonText, onClick = onButtonClick) },
    title = title?.let { { Title(text = it) } },
    text = text?.let { { Description(text = it) } }
  )
}

/**
 * Alert dialog with confirm/dismiss buttons
 */
@Composable
fun AlertDialog(
  confirmButtonText: String?,
  dismissButtonText: String?,
  onConfirmClick: () -> Unit,
  onDismissRequest: () -> Unit,
  onDismissClick: () -> Unit = onDismissRequest,
  useConfirmNegative: Boolean = false,
  useDismissNegative: Boolean = false,
  text: String? = null,
  title: String? = null
) {
  AlertDialog(
    onDismissRequest = onDismissRequest,
    confirmButton = {
      if (!confirmButtonText.isNullOrEmpty()) {
        if (useConfirmNegative) {
          NegativeButton(text = confirmButtonText, onClick = onConfirmClick)
        } else {
          Button(text = confirmButtonText, onClick = onConfirmClick)
        }
      }
    },
    dismissButton = {
      if (!dismissButtonText.isNullOrEmpty()) {
        if (useDismissNegative) {
          NegativeButton(text = dismissButtonText, onClick = onDismissClick)
        } else {
          Button(text = dismissButtonText, onClick = onDismissClick)
        }
      }
    },
    title = title?.let { { Title(text = it) } },
    text = text?.let { { Description(text = it) } }
  )
}

/**
 * Slot based version of [AlertDialog].
 * Prefer to use slots defined in [AlertDialog] namespace for consistent look.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlertDialog(
  onDismissRequest: () -> Unit,
  confirmButton: @Composable AlertDialog.() -> Unit,
  dismissButton: (@Composable AlertDialog.() -> Unit)? = null,
  title: (@Composable AlertDialog.() -> Unit)? = null,
  text: (@Composable AlertDialog.() -> Unit)? = null
) {
  BasicAlertDialog(onDismissRequest = onDismissRequest) {
    Column(
      modifier = Modifier
        .surface(
          backgroundColor = AppTheme.colors.surfaceLayer1,
          shape = RoundedCornerShape(28.dp)
        )
    ) {
      if (title != null || text != null) {
        Column(
          modifier = Modifier.padding(
            start = 24.dp,
            top = 24.dp,
            end = 24.dp
          ),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          title?.let { AlertDialog.it() }
          text?.let { AlertDialog.it() }
        }
      }
      FlowRow(
        modifier = Modifier
          .align(Alignment.End)
          .padding(
            start = 4.dp,
            top = 20.dp,
            end = 20.dp,
            bottom = 20.dp
          ),
        horizontalArrangement = Arrangement.End
      ) {
        dismissButton?.let {
          Box(Modifier.padding(4.dp)) {
            AlertDialog.it()
          }
        }
        Box(Modifier.padding(4.dp)) {
          AlertDialog.confirmButton()
        }
      }
    }
  }
}

object AlertDialog {
  @Composable
  fun Button(text: String, onClick: () -> Unit) {
    TextButton(
      onClick = onClick,
      size = ButtonSize.Small,
      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
    ) {
      Text(
        text = text,
        style = AppTheme.typography.subhead2,
        color = AppTheme.colors.textAccent,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }

  @Composable
  fun NegativeButton(text: String, onClick: () -> Unit) {
    TextButton(
      onClick = onClick,
      size = ButtonSize.Small,
      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
    ) {
      Text(
        text = text,
        style = AppTheme.typography.subhead2,
        color = AppTheme.colors.textNegative,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }

  @Composable
  fun Title(text: String) {
    Text(
      text = text,
      style = AppTheme.typography.headline4,
      color = AppTheme.colors.textPrimary
    )
  }

  @Composable
  fun Description(text: String) {
    Text(
      text = text,
      style = AppTheme.typography.body2,
      color = AppTheme.colors.textSecondary
    )
  }
}

@ThemePreviews
@Composable
private fun ButtonsPreview() {
  PreviewColumn {
    AlertDialog(
      onDismissRequest = {},
      onConfirmClick = {},
      title = "Alert title",
      text = "Description",
      confirmButtonText = "Agree",
      dismissButtonText = "Cancel"
    )
  }
}
