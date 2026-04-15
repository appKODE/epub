package ru.kode.epub.core.uikit.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection

object AppTheme {
  val colors: AppColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current

  val typography: AppTypography
    @Composable
    @ReadOnlyComposable
    get() = LocalAppTypography.current
}

@Composable
fun AppTheme(
  useDarkTheme: Boolean,
  layoutDirection: LayoutDirection = LocalLayoutDirection.current,
  typography: AppTypography = AppTheme.typography,
  content: @Composable () -> Unit
) {
  val colors = remember(useDarkTheme) {
    if (useDarkTheme) DarkAppColors else LightAppColors
  }

  val adaptedTypography = remember(layoutDirection, typography) {
    when (layoutDirection) {
      LayoutDirection.Ltr -> typography
      LayoutDirection.Rtl -> typography.copy(
        headline1 = typography.headline1.setRtlFontSizeScale(),
        headline2 = typography.headline2.setRtlFontSizeScale(),
        headline3 = typography.headline3.setRtlFontSizeScale(),
        headline4 = typography.headline4.setRtlFontSizeScale(),
        headline5 = typography.headline5.setRtlFontSizeScale(),
        subhead1 = typography.subhead1.setRtlFontSizeScale(),
        subhead2 = typography.subhead2.setRtlFontSizeScale()
      )
    }
  }

  val textSelectionColors = TextSelectionColors(
    handleColor = colors.textAccent,
    backgroundColor = colors.surfaceLayerAccentPale
  )
  val rippleIndication = ripple()
  val rippleConfiguration = remember(colors.textPrimary) { RippleConfiguration(colors.textPrimary) }
  CompositionLocalProvider(
    LocalAppColors provides colors,
    LocalAppTypography provides adaptedTypography,
    LocalContentColor provides colors.textPrimary,
    LocalTextSelectionColors provides textSelectionColors,
    LocalIndication provides rippleIndication,
    LocalRippleConfiguration provides rippleConfiguration,
    content = content
  )
}

private fun TextStyle.setRtlFontSizeScale(): TextStyle = copy(fontSize = fontSize * RTL_FONT_SIZE_SCALE)

internal val LocalAppColors = compositionLocalOf<AppColors> {
  error("No AppColors provided")
}

private const val RTL_FONT_SIZE_SCALE = 1.1f
