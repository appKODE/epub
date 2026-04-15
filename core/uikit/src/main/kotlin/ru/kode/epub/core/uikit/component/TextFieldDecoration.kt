package ru.kode.epub.core.uikit.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.kode.epub.core.uikit.theme.AppTheme

@Composable
fun FieldDecoration(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
  leadingContent: @Composable ((defaultMinSize: Dp) -> Unit)? = null,
  trailingContent: @Composable ((defaultMinSize: Dp) -> Unit)? = null
) {
  TextFieldDecoration(
    defaultContentHeight = DefaultContentHeight,
    textVerticalPadding = DefaultVerticalPadding,
    leadingContent = leadingContent,
    trailingContent = trailingContent,
    innerTextField = content,
    placeholderTextStyle = AppTheme.typography.body1,
    modifier = modifier
  )
}

@Composable
internal fun FormFieldHint(
  isError: Boolean,
  hint: String
) {
  if (isError) {
    Text(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
      text = hint,
      style = AppTheme.typography.caption1,
      color = AppTheme.colors.textNegative
    )
  } else {
    Text(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
      text = hint,
      style = AppTheme.typography.caption1,
      color = AppTheme.colors.textSecondary
    )
  }
}

@Composable
private fun TextFieldDecoration(
  defaultContentHeight: Dp,
  textVerticalPadding: Dp,
  placeholderTextStyle: TextStyle,
  modifier: Modifier = Modifier,
  placeholder: String? = null,
  maxLines: Int = Int.MAX_VALUE,
  textAlignment: Alignment.Vertical = Alignment.CenterVertically,
  innerTextField: @Composable () -> Unit,
  leadingContent: @Composable ((defaultMinSize: Dp) -> Unit)? = null,
  trailingContent: @Composable ((defaultMinSize: Dp) -> Unit)? = null
) {
  Row(
    modifier = modifier
      .background(
        color = AppTheme.colors.surfaceLayer1,
        shape = TextFieldShape
      )
      .border(
        width = 1.dp,
        color = Color.Transparent,
        shape = TextFieldShape
      )
      .padding(horizontal = DefaultHorizontalPadding),
    verticalAlignment = textAlignment
  ) {
    if (leadingContent != null) {
      SideContent(
        defaultContentHeight = defaultContentHeight,
        content = leadingContent
      )
    }
    Box(
      modifier = Modifier
        .weight(1f)
        .padding(
          top = textVerticalPadding,
          bottom = textVerticalPadding,
          start = DefaultHorizontalPadding,
          end = DefaultHorizontalPadding
        )
    ) {
      if (placeholder != null) {
        Text(
          text = placeholder,
          style = placeholderTextStyle,
          color = AppTheme.colors.textTertiary,
          maxLines = maxLines,
          overflow = TextOverflow.Ellipsis
        )
      }
      innerTextField()
    }
    if (trailingContent != null) {
      SideContent(
        defaultContentHeight = defaultContentHeight,
        content = trailingContent
      )
    }
  }
}

@Composable
private fun SideContent(
  defaultContentHeight: Dp,
  content: @Composable (Dp) -> Unit
) {
  Box(
    modifier = Modifier
      .sizeIn(minHeight = defaultContentHeight)
      .fillMaxHeight(),
    contentAlignment = Alignment.CenterStart
  ) {
    content(defaultContentHeight)
  }
}

internal val DefaultContentHeight = 56.dp
internal val DefaultHorizontalPadding = 12.dp
internal val DefaultVerticalPadding = 10.dp
internal val TextFieldShape = RoundedCornerShape(16.dp)
