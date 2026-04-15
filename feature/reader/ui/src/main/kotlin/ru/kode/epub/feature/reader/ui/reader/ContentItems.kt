package ru.kode.epub.feature.reader.ui.reader

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import ru.kode.epub.core.uikit.theme.AppTheme
import ru.kode.epub.lib.entity.ContentElement
import ru.kode.epub.lib.entity.CssLength
import ru.kode.epub.lib.entity.EpubFontFile
import ru.kode.epub.lib.entity.EpubTextAlign
import ru.kode.epub.lib.entity.StyledText
import java.io.File

val LocalEpubFontFamily = compositionLocalOf<FontFamily> { FontFamily.Default }

@Composable
fun rememberEpubFontFamily(fontFiles: List<EpubFontFile>): FontFamily {
  val context = LocalContext.current
  return remember(fontFiles) {
    if (fontFiles.isEmpty()) return@remember FontFamily.Default
    val fonts = fontFiles.mapNotNull { fontFile ->
      val ext = fontFile.name.substringAfterLast('.').lowercase()
      if (ext !in setOf("ttf", "otf")) return@mapNotNull null
      val file = File(context.cacheDir, "epub_${fontFile.name.hashCode()}.$ext")
      file.writeBytes(fontFile.bytes)
      val weight = when {
        "Black" in fontFile.name -> FontWeight.Black
        "Bold" in fontFile.name -> FontWeight.Bold
        "Light" in fontFile.name -> FontWeight.Light
        "Medium" in fontFile.name -> FontWeight.Medium
        else -> FontWeight.Normal
      }
      val style = if ("Italic" in fontFile.name) FontStyle.Italic else FontStyle.Normal
      Font(file, weight, style)
    }
    if (fonts.isEmpty()) FontFamily.Default else FontFamily(fonts)
  }
}

@Composable
fun ContentItem(element: ContentElement) {
  when (element) {
    is ContentElement.Heading -> HeadingItem(element)
    is ContentElement.Paragraph -> ParagraphItem(element)
    is ContentElement.Quote -> QuoteItem(element)
    is ContentElement.EpubImage -> ImageItem(element)
  }
}

@Composable
private fun HeadingItem(heading: ContentElement.Heading) {
  val css = heading.styles
  val baseStyle: TextStyle = when (heading.level) {
    1 -> AppTheme.typography.headline2
    2 -> AppTheme.typography.headline3
    3 -> AppTheme.typography.headline4
    4 -> AppTheme.typography.headline5
    else -> AppTheme.typography.subhead1
  }
  Text(
    text = heading.text.toAnnotatedString(),
    style = baseStyle.copy(
      fontWeight = css.bold?.toFontWeight() ?: baseStyle.fontWeight,
      fontStyle = css.italic?.toFontStyle() ?: baseStyle.fontStyle,
      textAlign = css.textAlign?.toCompose() ?: TextAlign.Center,
      color = AppTheme.colors.textPrimary
    ),
    fontFamily = LocalEpubFontFamily.current,
    modifier = Modifier
      .fillMaxWidth()
      .padding(
        top = css.marginTop.toDp(default = 16.dp),
        bottom = css.marginBottom.toDp(default = 4.dp),
        start = 16.dp,
        end = 16.dp
      )
  )
}

@Composable
private fun ParagraphItem(paragraph: ContentElement.Paragraph) {
  val css = paragraph.styles
  Text(
    text = paragraph.text.toAnnotatedString(),
    style = AppTheme.typography.body1.copy(
      lineHeight = 26.sp,
      textIndent = TextIndent(firstLine = (css.textIndentEm ?: 1f) * 16.sp),
      textAlign = css.textAlign?.toCompose() ?: TextAlign.Justify,
      hyphens = Hyphens.Auto,
      lineBreak = LineBreak.Paragraph,
      fontStyle = css.italic?.toFontStyle() ?: FontStyle.Normal,
      fontWeight = css.bold?.toFontWeight() ?: FontWeight.Normal,
      color = AppTheme.colors.textPrimary
    ),
    fontFamily = LocalEpubFontFamily.current,
    modifier = Modifier
      .fillMaxWidth()
      .padding(
        top = css.marginTop.toDp(default = 0.dp),
        bottom = css.marginBottom.toDp(default = 2.dp),
        start = 16.dp,
        end = 16.dp
      )
  )
}

@Composable
private fun QuoteItem(quote: ContentElement.Quote) {
  val css = quote.styles
  BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
    val parentWidth = maxWidth
    val widthModifier = when (val w = css.width) {
      null -> Modifier.fillMaxWidth()
      is CssLength.Em -> Modifier.width((w.value * 16).dp)
      is CssLength.Percent -> Modifier.width(parentWidth * (w.value / 100f))
    }
    Text(
      text = quote.text.toAnnotatedString(),
      style = AppTheme.typography.body1.copy(
        lineHeight = 26.sp,
        textAlign = css.textAlign?.toCompose() ?: TextAlign.Start,
        fontStyle = css.italic?.toFontStyle() ?: FontStyle.Normal,
        color = AppTheme.colors.textPrimary
      ),
      fontFamily = LocalEpubFontFamily.current,
      modifier = Modifier
        .padding(
          start = css.marginStart.toDp(default = 32.dp, parentWidth = parentWidth),
          end = css.marginEnd.toDp(default = 0.dp, parentWidth = parentWidth),
          top = css.marginTop.toDp(default = 4.dp),
          bottom = css.marginBottom.toDp(default = 4.dp)
        )
        .then(widthModifier)
    )
  }
}

@Composable
private fun ImageItem(image: ContentElement.EpubImage) {
  val bitmap = remember(image.data) {
    BitmapFactory.decodeByteArray(image.data, 0, image.data.size)?.asImageBitmap()
  }
  if (bitmap != null) {
    Image(
      bitmap = bitmap,
      contentDescription = image.alt,
      contentScale = ContentScale.Fit,
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)
    )
  }
}

// ─────────────────────────── Extensions ─────────────────────────────────────

fun StyledText.toAnnotatedString(): AnnotatedString = buildAnnotatedString {
  append(text)
  for (span in spans) {
    addStyle(
      SpanStyle(
        fontWeight = if (span.bold) FontWeight.Bold else null,
        fontStyle = if (span.italic) FontStyle.Italic else null
      ),
      span.start,
      span.end
    )
  }
}

internal fun CssLength?.toDp(default: Dp, parentWidth: Dp? = null): Dp = when (this) {
  null -> default
  is CssLength.Em -> (value * 16).dp
  is CssLength.Percent -> parentWidth?.times(value / 100f) ?: default
}

private fun EpubTextAlign.toCompose(): TextAlign = when (this) {
  EpubTextAlign.Left -> TextAlign.Left
  EpubTextAlign.Right -> TextAlign.Right
  EpubTextAlign.Center -> TextAlign.Center
  EpubTextAlign.Justify -> TextAlign.Justify
}

private fun Boolean.toFontWeight() = if (this) FontWeight.Bold else FontWeight.Normal
private fun Boolean.toFontStyle() = if (this) FontStyle.Italic else FontStyle.Normal
