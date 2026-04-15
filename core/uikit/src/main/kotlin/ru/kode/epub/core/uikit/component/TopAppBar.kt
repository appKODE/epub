package ru.kode.epub.core.uikit.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.kode.epub.core.uikit.R
import ru.kode.epub.core.uikit.component.preview.PreviewColumn
import ru.kode.epub.core.uikit.component.preview.ThemePreviews
import ru.kode.epub.core.uikit.theme.AppTheme

@Composable
fun TopAppBar(
  title: String,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  leadingContent: @Composable (BoxScope.() -> Unit)? = null,
  trailingContent: @Composable (BoxScope.() -> Unit)? = null
) {
  TopAppBar(
    modifier = modifier,
    centerContent = {
      TopAppBar.Title(
        modifier = Modifier.padding(12.dp),
        text = title,
        subtitle = subtitle
      )
    },
    leadingContent = leadingContent,
    trailingContent = trailingContent
  )
}

@Composable
fun TopAppBar(
  modifier: Modifier = Modifier,
  centerContent: @Composable (() -> Unit) = {},
  leadingContent: @Composable (BoxScope.() -> Unit)? = null,
  trailingContent: @Composable (BoxScope.() -> Unit)? = null
) {
  Layout(
    modifier = modifier
      .heightIn(min = appBarMinHeight)
      .fillMaxWidth(),
    content = {
      Box { leadingContent?.invoke(this) }
      Box { centerContent() }
      Box { trailingContent?.invoke(this) }
    }
  ) { measurables, constraints ->
    val maxHeight = constraints.maxHeight
    val leading = measurables[0].measure(
      Constraints(maxWidth = constraints.maxWidth, maxHeight = maxHeight)
    )
    val trailing = measurables[2].measure(
      Constraints(maxWidth = constraints.maxWidth - leading.width, maxHeight = maxHeight)
    )
    val center = measurables[1].measure(
      Constraints(maxWidth = constraints.maxWidth - maxOf(leading.width, trailing.width) * 2, maxHeight = maxHeight)
    )
    val width = constraints.maxWidth
    val height = maxOf(leading.height, trailing.height, center.height)
    layout(width, height) {
      leading.place(
        x = 0,
        y = Alignment.CenterVertically.align(leading.height, height)
      )
      center.place(
        x = if (leading.width == 0 && trailing.width == 0) {
          0
        } else {
          Alignment.CenterHorizontally.align(center.width, width, layoutDirection)
        },
        y = Alignment.CenterVertically.align(center.height, height)
      )
      trailing.place(
        x = Alignment.End.align(trailing.width, width, layoutDirection),
        y = Alignment.CenterVertically.align(trailing.height, height)
      )
    }
  }
}

object TopAppBar {
  @Composable
  fun BackAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = AppTheme.colors.iconAccent,
    startIndent: Dp = 8.dp
  ) {
    IconButton(
      modifier = modifier
        .size(appBarIconSize)
        .padding(start = startIndent),
      onClick = onClick
    ) {
      Icon(
        painter = painterResource(id = R.drawable.ic_arrow_backward_24),
        contentDescription = "back icon",
        tint = tint
      )
    }
  }

  @Composable
  fun CloseAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = AppTheme.colors.iconPrimary
  ) {
    IconButton(
      modifier = modifier.size(appBarIconSize),
      onClick = onClick
    ) {
      Icon(
        painter = painterResource(id = R.drawable.ic_close_24),
        contentDescription = "close icon",
        tint = tint
      )
    }
  }

  @Composable
  fun IconAction(
    @DrawableRes iconResId: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = AppTheme.colors.iconAccent
  ) {
    IconButton(
      modifier = modifier.size(appBarIconSize),
      onClick = onClick,
      enabled = enabled,
      colors = IconButtonDefaults.iconButtonColors(
        contentColor = tint
      )
    ) {
      Icon(
        painter = painterResource(id = iconResId),
        contentDescription = contentDescription
      )
    }
  }

  @Composable
  fun Title(
    text: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    titleStyle: TextStyle = AppTheme.typography.headline5,
    subtitleStyle: TextStyle = AppTheme.typography.caption1
  ) {
    Column(
      modifier = modifier,
      verticalArrangement = Arrangement.spacedBy(2.dp),
      horizontalAlignment = horizontalAlignment
    ) {
      Text(
        text = text,
        style = titleStyle,
        color = AppTheme.colors.textPrimary
      )
      if (subtitle != null) {
        Text(
          text = subtitle,
          style = subtitleStyle,
          color = AppTheme.colors.textSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

private val appBarMinHeight = 44.dp
private val appBarIconSize = 44.dp

@ThemePreviews
@Composable
private fun TopAppBarPreview() {
  PreviewColumn {
    TopAppBar(
      title = "Screen title",
      subtitle = "Subtitle",
      leadingContent = { TopAppBar.BackAction(onClick = {}) },
      trailingContent = {
        TextButton(onClick = { }) {
          Text(text = "Label")
        }
      }
    )
    HorizontalDivider()
    TopAppBar(
      title = "Only leading",
      subtitle = "Subtitle",
      leadingContent = { TopAppBar.BackAction(onClick = {}) }
    )
    HorizontalDivider()
    TopAppBar(
      title = "Only trailing",
      subtitle = "Subtitle",
      trailingContent = { TopAppBar.CloseAction(onClick = {}) }
    )
    HorizontalDivider()
    TopAppBar(
      title = "Only title with very very long title which should be displayed on multiple lines",
      subtitle = "Subtitle"
    )
    HorizontalDivider()
  }
}
