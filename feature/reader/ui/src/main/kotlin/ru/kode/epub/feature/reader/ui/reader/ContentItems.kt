package ru.kode.epub.feature.reader.ui.reader

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import ru.kode.epub.lib.entity.EpubBackground
import ru.kode.epub.lib.entity.EpubFontFile
import ru.kode.epub.lib.entity.EpubTextAlign
import ru.kode.epub.lib.entity.StyledText
import java.io.File

val LocalEpubFontFamily = compositionLocalOf<FontFamily> { FontFamily.Default }

// Default CSS spacing values — shared with PageCalculator.kt to keep measurement and rendering in sync
internal val HEADING_MARGIN_TOP_DEFAULT = 16.dp
internal val HEADING_MARGIN_BOTTOM_DEFAULT = 4.dp
internal val PARAGRAPH_MARGIN_BOTTOM_DEFAULT = 2.dp
internal val QUOTE_MARGIN_TOP_DEFAULT = 4.dp
internal val QUOTE_MARGIN_BOTTOM_DEFAULT = 4.dp
internal val QUOTE_MARGIN_START_DEFAULT = 32.dp
internal val IMAGE_VERTICAL_PADDING_DEFAULT = 8.dp
internal val HEADING_HORIZONTAL_PADDING_DEFAULT = 16.dp // each side, fixed

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

// ─────────────────────────── Background box ──────────────────────────────────

@Composable
fun EpubBackgroundBox(
  background: EpubBackground?,
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit
) {
  Box(modifier = modifier) {
    if (background != null) {
      EpubBackgroundLayer(background)
    }
    content()
  }
}

@Composable
private fun BoxScope.EpubBackgroundLayer(background: EpubBackground) {
  when (background) {
    is EpubBackground.SolidColor -> {
      Box(
        Modifier
          .matchParentSize()
          .background(Color(background.argb.toInt()))
      )
    }
    is EpubBackground.Image -> {
      val bitmap = remember(background.data) {
        BitmapFactory.decodeByteArray(background.data, 0, background.data.size)?.asImageBitmap()
      }
      if (bitmap != null) {
        BoxWithConstraints(Modifier.matchParentSize()) {
          val density = LocalDensity.current

          val imgW: Dp = when (val s = background.size) {
            null -> with(density) { bitmap.width.toDp() }
            is CssLength.Em -> (s.value * 16).dp
            is CssLength.Percent -> maxWidth * (s.value / 100f)
          }
          val imgH: Dp = imgW * (bitmap.height.toFloat() / bitmap.width)

          val offsetX: Dp = when (val px = background.positionX) {
            is CssLength.Em -> (px.value * 16).dp
            is CssLength.Percent -> (maxWidth - imgW) * (px.value / 100f)
          }
          val offsetY: Dp = when (val py = background.positionY) {
            is CssLength.Em -> (py.value * 16).dp
            is CssLength.Percent -> (maxHeight - imgH) * (py.value / 100f)
          }

          if (background.repeat) {
            // Tile the image across the background
            var y = offsetY
            while (y < maxHeight) {
              var x = offsetX
              while (x < maxWidth) {
                Image(
                  bitmap = bitmap,
                  contentDescription = null,
                  contentScale = ContentScale.FillBounds,
                  modifier = Modifier.size(imgW, imgH).absoluteOffset(x, y)
                )
                x += imgW
              }
              y += imgH
            }
          } else {
            Image(
              bitmap = bitmap,
              contentDescription = null,
              contentScale = ContentScale.FillBounds,
              modifier = Modifier.size(imgW, imgH).absoluteOffset(offsetX, offsetY)
            )
          }
        }
      }
    }
  }
}

// ─────────────────────────── Content items ───────────────────────────────────

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
  EpubBackgroundBox(
    background = css.background,
    modifier = Modifier
      .fillMaxWidth()
      .padding(
        top = css.marginTop.toDp(default = HEADING_MARGIN_TOP_DEFAULT),
        bottom = css.marginBottom.toDp(default = HEADING_MARGIN_BOTTOM_DEFAULT)
      )
  ) {
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
        .padding(horizontal = HEADING_HORIZONTAL_PADDING_DEFAULT)
    )
  }
}

@Composable
private fun ParagraphItem(paragraph: ContentElement.Paragraph) {
  val css = paragraph.styles
  EpubBackgroundBox(
    background = css.background,
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = css.minHeight.toDp(default = 0.dp))
      .padding(
        top = css.marginTop.toDp(default = 0.dp),
        bottom = css.marginBottom.toDp(default = PARAGRAPH_MARGIN_BOTTOM_DEFAULT)
      )
  ) {
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
          start = css.paddingStart.toDp(default = 16.dp),
          end = css.paddingEnd.toDp(default = 16.dp),
          top = css.paddingTop.toDp(default = 0.dp),
          bottom = css.paddingBottom.toDp(default = 0.dp)
        )
    )
  }
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
    EpubBackgroundBox(
      background = css.background,
      modifier = Modifier
        .padding(
          start = css.marginStart.toDp(default = QUOTE_MARGIN_START_DEFAULT, parentWidth = parentWidth),
          end = css.marginEnd.toDp(default = 0.dp, parentWidth = parentWidth),
          top = css.marginTop.toDp(default = QUOTE_MARGIN_TOP_DEFAULT),
          bottom = css.marginBottom.toDp(default = QUOTE_MARGIN_BOTTOM_DEFAULT)
        )
        .then(widthModifier)
    ) {
      Text(
        text = quote.text.toAnnotatedString(),
        style = AppTheme.typography.body1.copy(
          lineHeight = 26.sp,
          textAlign = css.textAlign?.toCompose() ?: TextAlign.Start,
          fontStyle = css.italic?.toFontStyle() ?: FontStyle.Normal,
          color = AppTheme.colors.textPrimary
        ),
        fontFamily = LocalEpubFontFamily.current,
        modifier = Modifier.fillMaxWidth()
      )
    }
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
        .padding(vertical = IMAGE_VERTICAL_PADDING_DEFAULT)
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
