package ru.kode.epub.core.uikit.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp
import ru.kode.epub.core.uikit.R

private val Roboto = FontFamily(
  Font(R.font.roboto_regular, FontWeight.Normal),
  Font(R.font.roboto_medium, FontWeight.Medium),
  Font(R.font.roboto_bold, FontWeight.Bold)
)

@ConsistentCopyVisibility
@Immutable
data class AppTypography internal constructor(
  val headline1: TextStyle = TextStyle(
    fontFamily = Roboto,
    fontWeight = FontWeight.SemiBold,
    fontSize = 36.sp,
    lineHeight = 44.sp
  ),
  val headline2: TextStyle = TextStyle(
    fontFamily = Roboto,
    fontWeight = FontWeight.SemiBold,
    fontSize = 32.sp,
    lineHeight = 40.sp
  ),
  val headline3: TextStyle = TextStyle(
    fontFamily = Roboto,
    fontWeight = FontWeight.SemiBold,
    fontSize = 28.sp,
    lineHeight = 36.sp
  ),
  val headline4: TextStyle = TextStyle(
    fontFamily = Roboto,
    fontWeight = FontWeight.SemiBold,
    fontSize = 24.sp,
    lineHeight = 32.sp
  ),
  val headline5: TextStyle = TextStyle(
    fontFamily = Roboto,
    fontWeight = FontWeight.SemiBold,
    fontSize = 22.sp,
    lineHeight = 28.sp
  ),
  val subhead1: TextStyle = TextStyle(
    fontFamily = Roboto,
    fontWeight = FontWeight.Medium,
    fontSize = 16.sp,
    lineHeight = 24.sp,
    letterSpacing = TextUnit(0.08f, TextUnitType.Em)
  ),
  val subhead2: TextStyle = TextStyle(
    fontFamily = Roboto,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = TextUnit(0.01f, TextUnitType.Em)
  ),
  val body1: TextStyle = TextStyle(
    fontFamily = Roboto,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 24.sp,
    letterSpacing = TextUnit(0.08f, TextUnitType.Em)
  ),
  val body2: TextStyle = TextStyle(
    fontFamily = Roboto,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = TextUnit(0.04f, TextUnitType.Em)
  ),
  val caption1: TextStyle = TextStyle(
    fontFamily = Roboto,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = TextUnit(0.05f, TextUnitType.Em)
  ),
  val caption2: TextStyle = TextStyle(
    fontFamily = Roboto,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = TextUnit(0.05f, TextUnitType.Em)
  ),
  val caption3: TextStyle = TextStyle(
    fontFamily = Roboto,
    fontWeight = FontWeight.Medium,
    fontSize = 10.sp,
    lineHeight = 20.sp,
    letterSpacing = TextUnit(0.1f, TextUnitType.Em)
  )
)

internal val LocalAppTypography = staticCompositionLocalOf { AppTypography() }
