package ru.kode.epub.feature.reader.ui.reader

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.kode.epub.lib.entity.Background
import ru.kode.epub.lib.entity.BorderSide
import ru.kode.epub.lib.entity.FontFace
import ru.kode.epub.lib.entity.FontVariant
import ru.kode.epub.lib.entity.Length
import ru.kode.epub.lib.entity.Node
import ru.kode.epub.lib.entity.Span
import ru.kode.epub.lib.entity.Style
import ru.kode.epub.lib.entity.TextAlign
import androidx.compose.ui.text.style.TextAlign as ComposeTextAlign

internal const val BASE_FONT_SIZE_SP = 16f

/** Provides the current available layout width so percentage-based CSS lengths can be resolved. */
val LocalAvailableWidth = compositionLocalOf { Dp.Unspecified }

// ─────────────────────────────────────────────────────────────────────────────
// Modifier.applyStyle — layout properties → Compose modifiers
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Applies CSS box-model properties as Compose modifiers in correct order:
 * outer margin → background (covers padding-box) → inner padding → min/max height.
 */
@Composable
fun Modifier.applyStyle(style: Style?, density: Density): Modifier {
  style ?: return this
  var result = this

  val availableWidth = LocalAvailableWidth.current

  val marginStart = style.marginStart.toDpOrZero(density, availableWidth)
  val marginEnd = style.marginEnd.toDpOrZero(density, availableWidth)
  val marginTop = style.marginTop.toDpOrZero(density)
  val marginBottom = style.marginBottom.toDpOrZero(density)
  if (marginStart > 0.dp || marginEnd > 0.dp || marginTop > 0.dp || marginBottom > 0.dp) {
    result = result.padding(start = marginStart, top = marginTop, end = marginEnd, bottom = marginBottom)
  }

  // Explicit width (e.g. width: 60%) — applied before background/border so they cover the right area
  style.width?.let { w ->
    val widthDp = w.toDp(density, availableWidth)
    when {
      widthDp != null && widthDp > 0.dp -> result = result.widthIn(max = widthDp)
      w is Length.Percent -> result = result.fillMaxWidth(w.value / 100f)
    }
  }

  // Background drawn after margin, before inner padding → covers CSS padding-box
  when (val bg = style.background) {
    is Background.Color -> result = result.background(Color(bg.argb.toInt()))
    is Background.Image -> {
      val bytes = bg.data
      val imageBitmap = remember(bytes) {
        bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
      }
      if (imageBitmap != null) {
        result = result.drawBehind {
          val sizePx = bg.size?.toDp(this)?.toPx() ?: (2f * BASE_FONT_SIZE_SP).sp.toPx()
          drawImage(
            image = imageBitmap,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(imageBitmap.width, imageBitmap.height),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(sizePx.toInt(), sizePx.toInt())
          )
        }
      }
    }
    null -> Unit
  }

  // Border drawn after background (so it appears on top of the fill) and before inner padding
  style.border?.let { b ->
    val top = b.top; val bottom = b.bottom; val start = b.start; val end = b.end
    val allSame = top != null && top == bottom && bottom == start && start == end
    if (allSame) {
      // Uniform border — use Modifier.border() which draws cleanly at the composable edge
      result = result.border(
        width = top!!.width.toDpOrZero(density),
        color = top.color?.let { Color(it.toInt()) } ?: Color.Black
      )
    } else {
      // Non-uniform — draw individual sides on top of content via drawWithContent
      result = result.drawWithContent {
        drawContent()
        top?.drawSide(density) { c, w -> drawLine(c, Offset(0f, w / 2f), Offset(size.width, w / 2f), w) }
        bottom?.drawSide(density) { c, w ->
          drawLine(
            c,
            Offset(0f, size.height - w / 2f),
            Offset(size.width, size.height - w / 2f),
            w
          )
        }
        start?.drawSide(density) { c, w -> drawLine(c, Offset(w / 2f, 0f), Offset(w / 2f, size.height), w) }
        end?.drawSide(density) { c, w ->
          drawLine(
            c,
            Offset(size.width - w / 2f, 0f),
            Offset(size.width - w / 2f, size.height),
            w
          )
        }
      }
    }
  }

  val paddingStart = style.paddingStart.toDpOrZero(density, availableWidth)
  val paddingEnd = style.paddingEnd.toDpOrZero(density, availableWidth)
  val paddingTop = style.paddingTop.toDpOrZero(density)
  val paddingBottom = style.paddingBottom.toDpOrZero(density)
  if (paddingStart > 0.dp || paddingEnd > 0.dp || paddingTop > 0.dp || paddingBottom > 0.dp) {
    result = result.padding(start = paddingStart, top = paddingTop, end = paddingEnd, bottom = paddingBottom)
  }

  style.minHeight?.toDp(density)?.let { if (it > 0.dp) result = result.heightIn(min = it) }
  style.maxWidth?.let { w ->
    val maxDp = w.toDp(density, availableWidth)
    when {
      maxDp != null && maxDp > 0.dp -> result = result.widthIn(max = maxDp)
      w is Length.Percent -> result = result.fillMaxWidth(w.value / 100f)
    }
  }

  return result
}

// ─────────────────────────────────────────────────────────────────────────────
// Style → TextStyle
// ─────────────────────────────────────────────────────────────────────────────

fun Style.toTextStyle(fontFamilyMap: Map<String, FontFamily> = emptyMap()): TextStyle = TextStyle(
  fontFamily = fontFamily?.let { fontFamilyMap[it] },
  fontSize = fontSize?.toSp() ?: TextUnit.Unspecified,
  fontWeight = fontWeight?.let { FontWeight(it) },
  fontStyle = italic?.let { if (it) FontStyle.Italic else FontStyle.Normal },
  color = color?.let { Color(it.toInt()) } ?: Color.Unspecified,
  textAlign = textAlign?.toComposeTextAlign() ?: ComposeTextAlign.Unspecified,
  lineHeight = lineHeight?.toSp() ?: TextUnit.Unspecified,
  textIndent = textIndent?.toSp()?.let { TextIndent(firstLine = it) },
  textDecoration = textDecoration?.toComposeDecoration(),
  hyphens = if (hyphensNone) Hyphens.None else Hyphens.Auto,
  fontFeatureSettings = if (fontVariant == FontVariant.SmallCaps) "smcp" else null,
  letterSpacing = letterSpacing?.toSp() ?: TextUnit.Unspecified
)

// ─────────────────────────────────────────────────────────────────────────────
// Node.Text → AnnotatedString
// ─────────────────────────────────────────────────────────────────────────────

fun Node.Text.toAnnotatedString(): AnnotatedString = buildAnnotatedString {
  append(raw)
  for (span in spans) {
    val end = span.end.coerceAtMost(raw.length)
    if (span.start >= end) continue
    addStyle(span.toSpanStyle(), span.start, end)
  }
}

private fun Span.toSpanStyle(): SpanStyle = SpanStyle(
  fontWeight = if (bold) FontWeight.Bold else null,
  fontStyle = if (italic) FontStyle.Italic else null,
  textDecoration = when {
    underline && strikethrough -> TextDecoration.combine(
      listOf(TextDecoration.Underline, TextDecoration.LineThrough)
    )
    underline -> TextDecoration.Underline
    strikethrough -> TextDecoration.LineThrough
    else -> null
  },
  baselineShift = when {
    superscript -> BaselineShift.Superscript
    subscript -> BaselineShift.Subscript
    else -> null
  },
  fontFeatureSettings = if (smallCaps) "smcp" else null,
  color = color?.let { Color(it.toInt()) } ?: Color.Unspecified,
  fontSize = fontSize?.toSp() ?: TextUnit.Unspecified
)

// ─────────────────────────────────────────────────────────────────────────────
// FontFace → FontFamily map (writes bytes to cache dir once)
// ─────────────────────────────────────────────────────────────────────────────

fun List<FontFace>.toFontFamilyMap(): Map<String, FontFamily> =
  groupBy { it.family }
    .mapValues { (_, faces) ->
      FontFamily(
        faces.mapNotNull { face ->
          runCatching {
            Font(
              file = face.file,
              weight = FontWeight(face.weight),
              style = if (face.italic) FontStyle.Italic else FontStyle.Normal
            )
          }.getOrNull()
        }
      )
    }

// ─────────────────────────────────────────────────────────────────────────────
// Length helpers
// ─────────────────────────────────────────────────────────────────────────────

fun Length.toDp(density: Density): Dp? = when (this) {
  is Length.Em -> with(density) { (value * BASE_FONT_SIZE_SP).sp.toDp() }
  is Length.Rem -> with(density) { (value * BASE_FONT_SIZE_SP).sp.toDp() }
  is Length.Px -> value.dp // CSS px ≈ dp (both are density-independent units)
  is Length.Pt -> (value * 96f / 72f).dp // 1pt = 4/3 CSS px at 96dpi
  is Length.Percent -> null // requires parent width context
  Length.Auto -> null
}

/** Resolves the length to [Dp], using [parentWidthDp] to compute percentage values. */
fun Length.toDp(density: Density, parentWidthDp: Dp): Dp? = when (this) {
  is Length.Percent -> if (parentWidthDp != Dp.Unspecified) parentWidthDp * (value / 100f) else null
  else -> toDp(density)
}

fun Length.toSp(): TextUnit = when (this) {
  is Length.Em -> (value * BASE_FONT_SIZE_SP).sp
  is Length.Rem -> (value * BASE_FONT_SIZE_SP).sp
  is Length.Px -> value.sp // CSS px ≈ sp at default font scale
  is Length.Pt -> (value * 96f / 72f).sp
  is Length.Percent -> (value / 100f * BASE_FONT_SIZE_SP).sp
  Length.Auto -> TextUnit.Unspecified
}

fun Length?.toDpOrZero(density: Density): Dp = this?.toDp(density) ?: 0.dp

fun Length?.toDpOrZero(density: Density, parentWidthDp: Dp): Dp =
  this?.toDp(density, parentWidthDp) ?: 0.dp

fun Length.toPx(density: Density): Float = toDp(density)?.let { with(density) { it.toPx() } } ?: 0f

// ─────────────────────────────────────────────────────────────────────────────
// Enum conversions
// ─────────────────────────────────────────────────────────────────────────────

private fun TextAlign.toComposeTextAlign() = when (this) {
  TextAlign.Left -> ComposeTextAlign.Left
  TextAlign.Center -> ComposeTextAlign.Center
  TextAlign.Right -> ComposeTextAlign.Right
  TextAlign.Justify -> ComposeTextAlign.Justify
}

private fun ru.kode.epub.lib.entity.TextDecoration.toComposeDecoration() = when (this) {
  ru.kode.epub.lib.entity.TextDecoration.Underline -> TextDecoration.Underline
  ru.kode.epub.lib.entity.TextDecoration.LineThrough -> TextDecoration.LineThrough
  ru.kode.epub.lib.entity.TextDecoration.Overline -> null // Compose doesn't support overline
}

private fun BorderSide.drawSide(
  density: Density,
  block: (color: Color, strokeWidth: Float) -> Unit
) {
  val w = width.toPx(density)
  if (w <= 0f) return
  val color = this.color?.let { Color(it.toInt()) } ?: Color.Black
  block(color, w)
}
