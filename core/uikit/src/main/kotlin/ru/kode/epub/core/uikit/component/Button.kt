package ru.kode.epub.core.uikit.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.kode.epub.core.ui.compose.modifiers.surface
import ru.kode.epub.core.uikit.R
import ru.kode.epub.core.uikit.component.preview.ThemePreviews
import ru.kode.epub.core.uikit.theme.AppTheme

/**
 * Has default content fillers as [ButtonScope] extensions. Example usage:
 * PrimaryButton(...) { TextWithIcon() }
 * */
@Composable
fun PrimaryButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  loading: Boolean = false,
  size: ButtonSize = ButtonSize.Large,
  disableClickBehavior: DisableClickBehavior = DisableClickBehavior.ChangeStyleAndDisable,
  contentPadding: PaddingValues = ButtonDefaults.contentPadding(size, ButtonAppearance.Primary),
  colors: @Composable (ButtonColors) -> ButtonColors = { it },
  content: @Composable ButtonScope.() -> Unit
) {
  Button(
    appearance = ButtonAppearance.Primary,
    size = size,
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    loading = loading,
    shape = ButtonDefaults.shape(size),
    contentPadding = contentPadding,
    colors = { colors(ButtonDefaults.buttonColors(ButtonAppearance.Primary)) },
    content = content,
    disableClickBehavior = disableClickBehavior
  )
}

/**
 * Has default content fillers as [ButtonScope] extensions. Example usage:
 * SecondaryButton(...) { TextWithIcon() }
 * */
@Composable
fun SecondaryButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  loading: Boolean = false,
  size: ButtonSize = ButtonSize.Large,
  disableClickBehavior: DisableClickBehavior = DisableClickBehavior.ChangeStyleAndDisable,
  contentPadding: PaddingValues = ButtonDefaults.contentPadding(size, ButtonAppearance.Secondary),
  colors: @Composable (ButtonColors) -> ButtonColors = { it },
  content: @Composable ButtonScope.() -> Unit
) {
  Button(
    appearance = ButtonAppearance.Secondary,
    size = size,
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    loading = loading,
    shape = ButtonDefaults.shape(size),
    contentPadding = contentPadding,
    colors = { colors(ButtonDefaults.buttonColors(ButtonAppearance.Secondary)) },
    content = content,
    disableClickBehavior = disableClickBehavior
  )
}

/**
 * Has default content fillers as [ButtonScope] extensions. Example usage:
 * TextButton(...) { TextWithIcon() }
 * */
@Composable
fun TextButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  loading: Boolean = false,
  size: ButtonSize = ButtonSize.Large,
  disableClickBehavior: DisableClickBehavior = DisableClickBehavior.ChangeStyleAndDisable,
  contentPadding: PaddingValues = ButtonDefaults.contentPadding(size, ButtonAppearance.Ghost),
  colors: @Composable (ButtonColors) -> ButtonColors = { it },
  content: @Composable ButtonScope.() -> Unit
) {
  Button(
    appearance = ButtonAppearance.Ghost,
    size = size,
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    loading = loading,
    shape = ButtonDefaults.shape(size),
    contentPadding = contentPadding,
    colors = { colors(ButtonDefaults.buttonColors(ButtonAppearance.Ghost)) },
    content = content,
    disableClickBehavior = disableClickBehavior
  )
}

/**
 * Has default content fillers as [ButtonScope] extensions. Example usage:
 * NeutralButton(...) { TextWithIcon() }
 * */
@Composable
fun NeutralButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  loading: Boolean = false,
  size: ButtonSize = ButtonSize.Large,
  disableClickBehavior: DisableClickBehavior = DisableClickBehavior.ChangeStyleAndDisable,
  contentPadding: PaddingValues = ButtonDefaults.contentPadding(size, ButtonAppearance.Neutral),
  colors: @Composable (ButtonColors) -> ButtonColors = { it },
  content: @Composable ButtonScope.() -> Unit
) {
  Button(
    appearance = ButtonAppearance.Neutral,
    size = size,
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    loading = loading,
    shape = ButtonDefaults.shape(size),
    contentPadding = contentPadding,
    colors = { colors(ButtonDefaults.buttonColors(ButtonAppearance.Neutral)) },
    content = content,
    disableClickBehavior = disableClickBehavior
  )
}

/**
 * Has default content fillers as [ButtonScope] extensions. Example usage:
 * PositiveButton(...) { TextWithIcon() }
 * */
@Composable
fun PositiveButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  loading: Boolean = false,
  size: ButtonSize = ButtonSize.Large,
  disableClickBehavior: DisableClickBehavior = DisableClickBehavior.ChangeStyleAndDisable,
  contentPadding: PaddingValues = ButtonDefaults.contentPadding(size, ButtonAppearance.Positive),
  colors: @Composable (ButtonColors) -> ButtonColors = { it },
  content: @Composable ButtonScope.() -> Unit
) {
  Button(
    appearance = ButtonAppearance.Positive,
    size = size,
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    loading = loading,
    shape = ButtonDefaults.shape(size),
    contentPadding = contentPadding,
    colors = { colors(ButtonDefaults.buttonColors(ButtonAppearance.Positive)) },
    content = content,
    disableClickBehavior = disableClickBehavior
  )
}

/**
 * Has default content fillers as [ButtonScope] extensions. Example usage:
 * NegativeButton(...) { TextWithIcon() }
 * */
@Composable
fun NegativeButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  loading: Boolean = false,
  size: ButtonSize = ButtonSize.Large,
  disableClickBehavior: DisableClickBehavior = DisableClickBehavior.ChangeStyleAndDisable,
  contentPadding: PaddingValues = ButtonDefaults.contentPadding(size, ButtonAppearance.Negative),
  colors: @Composable (ButtonColors) -> ButtonColors = { it },
  content: @Composable ButtonScope.() -> Unit
) {
  Button(
    appearance = ButtonAppearance.Negative,
    size = size,
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    loading = loading,
    shape = ButtonDefaults.shape(size),
    contentPadding = contentPadding,
    colors = { colors(ButtonDefaults.buttonColors(ButtonAppearance.Negative)) },
    content = content,
    disableClickBehavior = disableClickBehavior
  )
}

/**
 * Has default content fillers as [ButtonScope] extensions. Example usage:
 * PrimaryIconButton(...) { Icon() }
 * */
@Composable
fun PrimaryIconButton(
  @DrawableRes iconResId: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  loading: Boolean = false,
  size: ButtonSize = ButtonSize.Large,
  disableClickBehavior: DisableClickBehavior = DisableClickBehavior.ChangeStyleAndDisable,
  contentPadding: PaddingValues = ButtonDefaults.iconContentPadding(size),
  colors: @Composable (ButtonColors) -> ButtonColors = { it }
) {
  Button(
    appearance = ButtonAppearance.Primary,
    size = size,
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    loading = loading,
    shape = ButtonDefaults.shape(size),
    contentPadding = contentPadding,
    colors = { colors(ButtonDefaults.buttonColors(ButtonAppearance.Primary)) },
    content = { Icon(iconResId) },
    disableClickBehavior = disableClickBehavior
  )
}

/**
 * Has default content fillers as [ButtonScope] extensions. Example usage:
 * SecondaryIconButton(...) { Icon() }
 * */
@Composable
fun SecondaryIconButton(
  @DrawableRes iconResId: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  loading: Boolean = false,
  size: ButtonSize = ButtonSize.Large,
  disableClickBehavior: DisableClickBehavior = DisableClickBehavior.ChangeStyleAndDisable,
  contentPadding: PaddingValues = ButtonDefaults.iconContentPadding(size),
  colors: @Composable (ButtonColors) -> ButtonColors = { it }
) {
  Button(
    appearance = ButtonAppearance.Secondary,
    size = size,
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    loading = loading,
    shape = ButtonDefaults.shape(size),
    contentPadding = contentPadding,
    colors = { colors(ButtonDefaults.buttonColors(ButtonAppearance.Secondary)) },
    content = { Icon(iconResId) },
    disableClickBehavior = disableClickBehavior
  )
}

/**
 * Has default content fillers as [ButtonScope] extensions. Example usage:
 * TextIconButton(...) { Icon() }
 * */
@Composable
fun TextIconButton(
  @DrawableRes iconResId: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  loading: Boolean = false,
  size: ButtonSize = ButtonSize.Large,
  disableClickBehavior: DisableClickBehavior = DisableClickBehavior.ChangeStyleAndDisable,
  contentPadding: PaddingValues = ButtonDefaults.iconContentPadding(size),
  colors: @Composable (ButtonColors) -> ButtonColors = { it }
) {
  Button(
    appearance = ButtonAppearance.Ghost,
    size = size,
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    loading = loading,
    shape = ButtonDefaults.shape(size),
    contentPadding = contentPadding,
    colors = { colors(ButtonDefaults.buttonColors(ButtonAppearance.Ghost)) },
    content = { Icon(iconResId) },
    disableClickBehavior = disableClickBehavior
  )
}

/**
 * Has default content fillers as [ButtonScope] extensions. Example usage:
 * TextIconButton(...) { Icon() }
 * */
@Composable
fun NeutralIconButton(
  @DrawableRes iconResId: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  loading: Boolean = false,
  size: ButtonSize = ButtonSize.Large,
  disableClickBehavior: DisableClickBehavior = DisableClickBehavior.ChangeStyleAndDisable,
  contentPadding: PaddingValues = ButtonDefaults.iconContentPadding(size),
  colors: @Composable (ButtonColors) -> ButtonColors = { it }
) {
  Button(
    appearance = ButtonAppearance.Neutral,
    size = size,
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    loading = loading,
    shape = ButtonDefaults.shape(size),
    contentPadding = contentPadding,
    colors = { colors(ButtonDefaults.buttonColors(ButtonAppearance.Neutral)) },
    content = { Icon(iconResId) },
    disableClickBehavior = disableClickBehavior
  )
}

/**
 * Has default content fillers as [ButtonScope] extensions. Example usage:
 * Button(...) { TextWithIcon() }
 * */
@Composable
fun Button(
  appearance: ButtonAppearance,
  size: ButtonSize,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  loading: Boolean = false,
  disableClickBehavior: DisableClickBehavior = DisableClickBehavior.ChangeStyleAndDisable,
  shape: CornerBasedShape = ButtonDefaults.shape(size),
  contentPadding: PaddingValues = ButtonDefaults.contentPadding(size, appearance),
  colors: @Composable () -> ButtonColors = { ButtonDefaults.buttonColors(appearance) },
  content: @Composable ButtonScope.() -> Unit
) {
  val colorsState by rememberUpdatedState(colors())

  val backgroundColor by animateColorAsState(
    targetValue = colorsState.backgroundColor(enabled, loading),
    label = "background color"
  )

  Row(
    modifier = modifier
      // set large button min height explicitly because sp + padding does not match raw dp values,
      // which causes visual inconsistency between buttons and supposedly same sized fields
      .heightIn(min = ButtonDefaults.minHeight(size))
      .surface(
        backgroundColor = backgroundColor,
        shape = shape
      )
      .clickable(
        enabled = when (disableClickBehavior) {
          DisableClickBehavior.ChangeStyleAndDisable -> enabled
          DisableClickBehavior.ChangeStyle -> true
        },
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(color = colorsState.textColor(enabled = true, loading = false)),
        onClick = onClick
      )
      .padding(contentPadding),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically
  ) {
    val state = rememberUpdatedState(ButtonState(colorsState, appearance, size, enabled, loading))
    val scope = remember {
      object : ButtonScope {
        override val buttonState: State<ButtonState> = state
      }
    }
    content(scope)
  }
}

@Composable
fun ButtonScope.Text(
  text: String,
  modifier: Modifier = Modifier,
  style: TextStyle = ButtonDefaults.textStyle(buttonState.value.size, buttonState.value.appearance)
) {
  Box(
    modifier = modifier,
    contentAlignment = Alignment.Center
  ) {
    val iconColor by animateColorAsState(
      targetValue = buttonState.value.colors.iconColor(
        buttonState.value.enabled,
        buttonState.value.loading
      ),
      label = "icon color"
    )
    CircularLoader(
      modifier = Modifier
        .size(ButtonDefaults.loaderSize(buttonSize = buttonState.value.size))
        .buttonContentVisible(buttonState.value.loading),
      color = iconColor,
      strokeWidth = ButtonDefaults.loaderStrokeWidth(buttonSize = buttonState.value.size)
    )

    val textColor by animateColorAsState(
      buttonState.value.colors.textColor(
        buttonState.value.enabled,
        buttonState.value.loading
      ),
      label = "text color"
    )
    Text(
      text = text,
      style = style,
      modifier = Modifier.buttonContentVisible(!buttonState.value.loading),
      color = textColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
fun ButtonScope.Icon(@DrawableRes iconResId: Int, modifier: Modifier = Modifier) {
  Box(
    modifier = modifier,
    contentAlignment = Alignment.Center
  ) {
    val iconColor by animateColorAsState(
      targetValue = buttonState.value.colors.iconColor(
        buttonState.value.enabled,
        buttonState.value.loading
      ),
      label = "icon color"
    )
    CircularLoader(
      modifier = Modifier
        .size(ButtonDefaults.loaderSize(buttonState.value.size))
        .buttonContentVisible(buttonState.value.loading),
      color = iconColor,
      strokeWidth = ButtonDefaults.loaderStrokeWidth(buttonSize = buttonState.value.size)
    )
    Icon(
      modifier = Modifier
        .size(ButtonDefaults.iconSize(buttonState.value.size))
        .buttonContentVisible(!buttonState.value.loading),
      painter = painterResource(iconResId),
      contentDescription = "button icon",
      tint = iconColor
    )
  }
}

@Composable
fun ButtonScope.TextWithIcon(
  text: String,
  @DrawableRes iconResId: Int,
  modifier: Modifier = Modifier,
  iconPosition: IconPosition = IconPosition.Leading,
  textStyle: TextStyle = ButtonDefaults.textStyle(buttonState.value.size, buttonState.value.appearance)
) {
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically
  ) {
    if (iconPosition == IconPosition.Leading) {
      Icon(iconResId = iconResId)
      HSpacer(ButtonDefaults.IconSpacing)
    }
    val textColor by animateColorAsState(
      buttonState.value.colors.textColor(
        buttonState.value.enabled,
        buttonState.value.loading
      ),
      label = "text color"
    )
    Text(
      text = text,
      style = textStyle,
      color = textColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    if (iconPosition == IconPosition.Trailing) {
      HSpacer(ButtonDefaults.IconSpacing)
      Icon(iconResId = iconResId)
    }
  }
}

@Composable
private fun Modifier.buttonContentVisible(visible: Boolean): Modifier {
  val alpha by animateFloatAsState(
    targetValue = if (visible) 1f else 0f,
    label = "alpha content animation"
  )
  val scale by animateFloatAsState(
    targetValue = if (visible) 1f else 0.5f,
    label = "scale content animation"
  )

  return this then Modifier
    .alpha(alpha)
    .scale(scale)
}

object ButtonDefaults {
  val IconSpacing = 8.dp

  fun shape(buttonSize: ButtonSize): CornerBasedShape = when (buttonSize) {
    ButtonSize.Large -> RoundedCornerShape(16.dp)
    ButtonSize.Medium -> RoundedCornerShape(12.dp)
    ButtonSize.Small -> RoundedCornerShape(12.dp)
  }

  fun minHeight(buttonSize: ButtonSize): Dp = when (buttonSize) {
    ButtonSize.Small -> 36.dp
    ButtonSize.Medium -> 44.dp
    ButtonSize.Large -> 56.dp
  }

  fun iconSize(buttonSize: ButtonSize): Dp = when (buttonSize) {
    ButtonSize.Small -> 20.dp
    ButtonSize.Medium -> 24.dp
    ButtonSize.Large -> 24.dp
  }

  fun contentPadding(buttonSize: ButtonSize, appearance: ButtonAppearance): PaddingValues = when (appearance) {
    ButtonAppearance.Primary,
    ButtonAppearance.Secondary,
    ButtonAppearance.Neutral,
    ButtonAppearance.Positive,
    ButtonAppearance.Negative
    -> when (buttonSize) {
      ButtonSize.Small -> PaddingValues(horizontal = 12.dp, vertical = 8.dp)
      ButtonSize.Medium -> PaddingValues(horizontal = 12.dp, vertical = 10.dp)
      ButtonSize.Large -> PaddingValues(16.dp)
    }

    ButtonAppearance.Ghost -> when (buttonSize) {
      ButtonSize.Small -> PaddingValues(vertical = 8.dp)
      ButtonSize.Medium -> PaddingValues(vertical = 10.dp)
      ButtonSize.Large -> PaddingValues(vertical = 16.dp)
    }
  }

  fun iconContentPadding(buttonSize: ButtonSize): PaddingValues = when (buttonSize) {
    ButtonSize.Small -> PaddingValues(8.dp)
    ButtonSize.Medium -> PaddingValues(10.dp)
    ButtonSize.Large -> PaddingValues(16.dp)
  }

  @Composable
  @ReadOnlyComposable
  fun textStyle(buttonSize: ButtonSize, appearance: ButtonAppearance): TextStyle = when (buttonSize) {
    ButtonSize.Small -> when (appearance) {
      ButtonAppearance.Primary, ButtonAppearance.Neutral,
      ButtonAppearance.Positive, ButtonAppearance.Negative
      -> AppTheme.typography.subhead2

      ButtonAppearance.Secondary, ButtonAppearance.Ghost -> AppTheme.typography.body2
    }

    ButtonSize.Medium -> when (appearance) {
      ButtonAppearance.Primary, ButtonAppearance.Secondary,
      ButtonAppearance.Ghost, ButtonAppearance.Neutral
      -> AppTheme.typography.subhead1

      ButtonAppearance.Positive,
      ButtonAppearance.Negative
      -> AppTheme.typography.subhead2
    }

    ButtonSize.Large -> AppTheme.typography.subhead1
  }

  fun loaderStrokeWidth(buttonSize: ButtonSize): Dp = when (buttonSize) {
    ButtonSize.Small -> 1.5.dp
    ButtonSize.Medium -> 2.dp
    ButtonSize.Large -> 2.dp
  }

  fun loaderSize(buttonSize: ButtonSize): Dp = when (buttonSize) {
    ButtonSize.Small -> 20.dp
    ButtonSize.Medium -> 24.dp
    ButtonSize.Large -> 24.dp
  }

  @Composable
  @ReadOnlyComposable
  fun buttonColors(appearance: ButtonAppearance): ButtonColors = when (appearance) {
    ButtonAppearance.Primary -> ButtonColors(
      backgroundColor = AppTheme.colors.surfaceLayerAccent,
      iconColor = AppTheme.colors.textOnAccent,
      textColor = AppTheme.colors.textOnAccent,
      disabledBackgroundColor = AppTheme.colors.interactiveLayerAccentDisabled,
      disabledIconColor = AppTheme.colors.textOnAccent.copy(alpha = 0.6f),
      disabledTextColor = AppTheme.colors.textOnAccent.copy(alpha = 0.6f),
      loadingBackgroundColor = AppTheme.colors.interactiveLayerAccentDisabled,
      loadingIconColor = AppTheme.colors.textOnAccent.copy(alpha = 0.6f),
      loadingTextColor = AppTheme.colors.textOnAccent.copy(alpha = 0.6f)
    )

    ButtonAppearance.Secondary -> ButtonColors(
      backgroundColor = AppTheme.colors.surfaceLayerTranslucent,
      iconColor = AppTheme.colors.textAccent,
      textColor = AppTheme.colors.textAccent,
      disabledBackgroundColor = AppTheme.colors.interactiveLayerTranslucentDisabled,
      disabledIconColor = AppTheme.colors.textAccent.copy(alpha = 0.4f),
      disabledTextColor = AppTheme.colors.textAccent.copy(alpha = 0.4f),
      loadingBackgroundColor = AppTheme.colors.interactiveLayerTranslucentDisabled,
      loadingIconColor = AppTheme.colors.textAccent.copy(alpha = 0.4f),
      loadingTextColor = AppTheme.colors.textAccent.copy(alpha = 0.4f)
    )

    ButtonAppearance.Ghost -> ButtonColors(
      backgroundColor = Color.Transparent,
      iconColor = AppTheme.colors.textAccent,
      textColor = AppTheme.colors.textAccent,
      disabledBackgroundColor = Color.Transparent,
      disabledIconColor = AppTheme.colors.textAccent.copy(alpha = 0.4f),
      disabledTextColor = AppTheme.colors.textAccent.copy(alpha = 0.4f),
      loadingBackgroundColor = Color.Transparent,
      loadingIconColor = AppTheme.colors.textAccent.copy(alpha = 0.4f),
      loadingTextColor = AppTheme.colors.textAccent.copy(alpha = 0.4f)
    )

    ButtonAppearance.Neutral -> ButtonColors(
      backgroundColor = AppTheme.colors.surfaceLayer1,
      iconColor = AppTheme.colors.textAccent,
      textColor = AppTheme.colors.textAccent,
      disabledBackgroundColor = AppTheme.colors.surfaceLayer1.copy(alpha = 0.4f),
      disabledIconColor = AppTheme.colors.textAccent.copy(alpha = 0.4f),
      disabledTextColor = AppTheme.colors.textAccent.copy(alpha = 0.4f),
      loadingBackgroundColor = AppTheme.colors.surfaceLayer1.copy(alpha = 0.4f),
      loadingIconColor = AppTheme.colors.textAccent.copy(alpha = 0.4f),
      loadingTextColor = AppTheme.colors.textAccent.copy(alpha = 0.4f)
    )

    ButtonAppearance.Positive -> ButtonColors(
      backgroundColor = AppTheme.colors.surfaceLayerPositive,
      iconColor = AppTheme.colors.textOnAccent,
      textColor = AppTheme.colors.textOnAccent,
      disabledBackgroundColor = AppTheme.colors.surfaceLayerPositivePale,
      disabledIconColor = AppTheme.colors.textOnAccent.copy(alpha = 0.6f),
      disabledTextColor = AppTheme.colors.textOnAccent.copy(alpha = 0.6f),
      loadingBackgroundColor = AppTheme.colors.surfaceLayerPositive,
      loadingIconColor = AppTheme.colors.textOnAccent,
      loadingTextColor = AppTheme.colors.textOnAccent
    )

    ButtonAppearance.Negative -> ButtonColors(
      backgroundColor = AppTheme.colors.surfaceLayerNegative,
      iconColor = AppTheme.colors.textOnAccent,
      textColor = AppTheme.colors.textOnAccent,
      disabledBackgroundColor = AppTheme.colors.surfaceLayerNegativePale,
      disabledIconColor = AppTheme.colors.textOnAccent.copy(alpha = 0.6f),
      disabledTextColor = AppTheme.colors.textOnAccent.copy(alpha = 0.6f),
      loadingBackgroundColor = AppTheme.colors.surfaceLayerNegative,
      loadingIconColor = AppTheme.colors.textOnAccent,
      loadingTextColor = AppTheme.colors.textOnAccent
    )
  }
}

@Immutable
data class ButtonColors(
  private val backgroundColor: Color,
  private val iconColor: Color,
  private val textColor: Color,
  private val disabledBackgroundColor: Color,
  private val disabledIconColor: Color,
  private val disabledTextColor: Color,
  private val loadingBackgroundColor: Color,
  private val loadingIconColor: Color,
  private val loadingTextColor: Color
) {
  fun backgroundColor(enabled: Boolean, loading: Boolean): Color = when {
    !enabled -> disabledBackgroundColor
    loading -> loadingBackgroundColor
    else -> backgroundColor
  }

  fun iconColor(enabled: Boolean, loading: Boolean): Color = when {
    !enabled -> disabledIconColor
    loading -> loadingIconColor
    else -> iconColor
  }

  fun textColor(enabled: Boolean, loading: Boolean): Color = when {
    !enabled -> disabledTextColor
    loading -> loadingTextColor
    else -> textColor
  }
}

@Stable
interface ButtonScope {
  val buttonState: State<ButtonState>
}

enum class IconPosition {
  Leading,
  Trailing,
}

enum class ButtonAppearance {
  Primary,
  Secondary,
  Ghost,
  Neutral,
  Positive,
  Negative,
}

enum class DisableClickBehavior {
  ChangeStyleAndDisable,
  ChangeStyle,
}

enum class ButtonSize {
  Large,
  Medium,
  Small,
}

@Immutable
data class ButtonState(
  val colors: ButtonColors,
  val appearance: ButtonAppearance,
  val size: ButtonSize,
  val enabled: Boolean,
  val loading: Boolean
)

@ThemePreviews
@Composable
private fun ButtonsPreview() {
  AppTheme(useDarkTheme = isSystemInDarkTheme()) {
    Column(
      modifier = Modifier
        .background(AppTheme.colors.surfaceBackground)
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      var loading by remember { mutableStateOf(false) }
      PrimaryButton(
        onClick = { loading = !loading },
        enabled = !loading
      ) {
        Text(
          text = "${buttonState.value.size.name} Primary"
        )
      }
      PositiveButton(
        onClick = { loading = !loading },
        enabled = !loading
      ) {
        Text(
          text = "${buttonState.value.size.name} Positive"
        )
      }
      NegativeButton(
        onClick = { loading = !loading },
        enabled = !loading
      ) {
        Text(
          text = "${buttonState.value.size.name} Negative"
        )
      }
      SecondaryButton(
        onClick = { loading = !loading },
        enabled = !loading
      ) {
        TextWithIcon(
          text = "${buttonState.value.size.name} Secondary",
          iconResId = R.drawable.ic_placeholder_24
        )
      }
      TextButton(
        onClick = { loading = !loading },
        enabled = !loading
      ) {
        TextWithIcon(
          text = "${buttonState.value.size.name} Ghost",
          iconResId = R.drawable.ic_placeholder_24
        )
      }
      TextButton(
        onClick = { loading = !loading },
        enabled = !loading
      ) {
        Text(
          text = "${buttonState.value.size.name} Ghost"
        )
      }
      SecondaryButton(
        onClick = { loading = !loading },
        enabled = !loading
      ) {
        Icon(
          iconResId = R.drawable.ic_placeholder_24
        )
      }

      VSpacer(16.dp)

      PrimaryButton(
        onClick = { loading = !loading }
      ) {
        Text(
          text = "Set loading"
        )
      }
    }
  }
}
