package ru.kode.epub.core.uikit.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.kode.epub.core.domain.entity.strRef
import ru.kode.epub.core.ui.compose.resolveRef
import ru.kode.epub.core.ui.content.UiMessage
import ru.kode.epub.core.uikit.R
import ru.kode.epub.core.uikit.component.preview.PreviewColumn
import ru.kode.epub.core.uikit.theme.AppTheme

@Composable
fun ErrorMessage(
  message: UiMessage,
  modifier: Modifier = Modifier,
  verticalArrangement: Arrangement.Vertical = Arrangement.Center
) {
  Column(
    modifier = modifier.padding(horizontal = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = verticalArrangement
  ) {
    if (message.illustration != null) {
      Icon(
        modifier = Modifier.fillMaxSize(0.5f),
        tint = AppTheme.colors.surfaceLayerAccentPale,
        painter = painterResource(id = message.illustration!!.toDrawableResource()),
        contentDescription = null
      )
    }
    Text(
      text = resolveRef(source = message.title),
      color = AppTheme.colors.textPrimary,
      style = AppTheme.typography.headline5,
      textAlign = TextAlign.Center
    )
    message.description?.let {
      VSpacer(size = 16.dp)
      Text(
        text = resolveRef(source = it),
        color = AppTheme.colors.textSecondary,
        style = AppTheme.typography.body2,
        textAlign = TextAlign.Center
      )
    }
  }
  val primaryAction = message.primaryAction
  if (primaryAction != null) {
    VSpacer(size = 24.dp)
    PrimaryButton(
      modifier = Modifier.fillMaxWidth(),
      onClick = { message.primaryAction?.listener() }
    ) {
      Text(text = resolveRef(primaryAction.name))
    }
  }
  val secondaryAction = message.secondaryAction
  if (secondaryAction != null) {
    if (primaryAction != null) {
      VSpacer(size = 8.dp)
    } else {
      VSpacer(size = 24.dp)
    }
    SecondaryButton(
      modifier = Modifier.fillMaxWidth(),
      onClick = { message.secondaryAction?.listener() }
    ) {
      Text(text = resolveRef(secondaryAction.name))
    }
  }
  VSpacer(size = 16.dp)
}

@Preview
@Composable
private fun ErrorMessagePreview() {
  PreviewColumn {
    ErrorMessage(
      message = UiMessage(
        title = strRef("Ошибка"),
        description = strRef("Вы не можете мочь"),
        primaryAction = UiMessage.Action(
          name = strRef("Техподдержка"),
          listener = {},
          description = strRef("Памагити")
        ),
        secondaryAction = null,
        illustration = UiMessage.Illustration.Default
      )
    )
  }
}

@Composable
private fun UiMessage.Illustration.toDrawableResource(): Int {
  return remember(this) {
    when (this) {
      UiMessage.Illustration.Default -> R.drawable.ic_book_24
    }
  }
}
