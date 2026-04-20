package ru.kode.epub.feature.reader.ui.reader.component

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.kode.epub.core.uikit.theme.AppTheme
import ru.kode.epub.feature.reader.ui.reader.applyStyle
import ru.kode.epub.feature.reader.ui.reader.toAnnotatedString
import ru.kode.epub.feature.reader.ui.reader.toTextStyle
import ru.kode.epub.lib.entity.AlignItems
import ru.kode.epub.lib.entity.Display
import ru.kode.epub.lib.entity.ListStyleType
import ru.kode.epub.lib.entity.Node
import ru.kode.epub.lib.entity.TextAlign
import ru.kode.epub.lib.entity.WhiteSpace

@Composable
internal fun NodeContent(
  node: Node,
  fontFamilyMap: Map<String, FontFamily>,
  modifier: Modifier = Modifier,
  inheritedTextStyle: TextStyle? = null,
  uppercase: Boolean = false
) {
  val density = LocalDensity.current
  when (node) {
    is Node.Text -> {
      val text = if (uppercase) node.copy(raw = node.raw.uppercase()) else node
      val annotated = text.toAnnotatedString()
      val style = inheritedTextStyle ?: TextStyle.Default.copy(hyphens = Hyphens.Auto)
      Text(text = annotated, modifier = modifier, style = style)
    }

    is Node.Image -> {
      val imageModifier = modifier.applyStyle(node.style, density)
      val bitmap = remember(node.data) {
        BitmapFactory.decodeByteArray(node.data, 0, node.data.size)?.asImageBitmap()
      }
      if (bitmap != null) {
        Image(
          bitmap = bitmap,
          contentDescription = node.alt.takeIf { it.isNotEmpty() },
          contentScale = ContentScale.Fit,
          modifier = imageModifier.fillMaxWidth()
        )
      }
    }

    is Node.HorizontalRule -> {
      HorizontalDivider(
        modifier = modifier
          .applyStyle(node.style, density)
          .padding(vertical = 8.dp),
        color = AppTheme.colors.borderRegular
      )
    }

    is Node.BulletList -> {
      BulletListContent(node, fontFamilyMap, modifier)
    }

    is Node.Table -> {
      TableContent(node, fontFamilyMap, modifier)
    }

    is Node.Element -> {
      ElementContent(node, fontFamilyMap, modifier, inheritedTextStyle, uppercase)
    }
  }
}

@Composable
private fun ElementContent(
  element: Node.Element,
  fontFamilyMap: Map<String, FontFamily>,
  modifier: Modifier = Modifier,
  parentTextStyle: TextStyle? = null,
  parentUppercase: Boolean = false
) {
  val density = LocalDensity.current
  val styledModifier = modifier.applyStyle(element.style, density)
  val textStyle = element.style?.toTextStyle(fontFamilyMap) ?: parentTextStyle

  val uppercase = element.style?.textTransformUppercase == true || parentUppercase
  val whiteSpace = element.style?.whiteSpace
  val softWrap = whiteSpace != WhiteSpace.NoWrap && whiteSpace != WhiteSpace.Pre
  val isCentered = element.style?.textAlign == TextAlign.Center
  val isFlex = element.style?.display == Display.Flex
  val flexCenter = isFlex && element.style?.alignItems == AlignItems.Center

  when (element.tag) {
    "h1", "h2", "h3", "h4", "h5", "h6" -> {
      val level = element.tag[1].digitToInt()
      val headingStyle = textStyle ?: TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = (24 - (level - 1) * 2).sp,
        hyphens = Hyphens.Auto
      )
      Text(
        text = collectAnnotated(element, uppercase),
        style = headingStyle,
        softWrap = softWrap,
        modifier = styledModifier
          .padding(vertical = 4.dp)
          .fillMaxWidth()
      )
    }

    "pre" -> {
      // Preserve whitespace verbatim
      Text(
        text = collectAnnotated(element, uppercase = false),
        style = textStyle ?: TextStyle(
          fontFamily = FontFamily.Monospace,
          fontSize = 14.sp,
          hyphens = Hyphens.None
        ),
        softWrap = false,
        modifier = styledModifier
      )
    }

    "blockquote" -> {
      // Apply default indent only when the CSS doesn't provide an explicit margin-start
      val hasExplicitMargin = element.style?.marginStart != null
      Column(
        modifier = styledModifier
          .let { if (!hasExplicitMargin) it.padding(start = 16.dp) else it }
          .fillMaxWidth()
      ) {
        element.children.forEach { child ->
          NodeContent(child, fontFamilyMap, inheritedTextStyle = textStyle, uppercase = uppercase)
        }
      }
    }

    "p" -> {
      val childModifier = if (isCentered) Modifier.fillMaxWidth() else Modifier
      if (flexCenter) {
        Box(modifier = styledModifier, contentAlignment = Alignment.CenterStart) {
          element.children.forEach { child ->
            NodeContent(
              child,
              fontFamilyMap,
              modifier = childModifier,
              inheritedTextStyle = textStyle,
              uppercase = uppercase
            )
          }
        }
      } else {
        Column(modifier = styledModifier) {
          element.children.forEach { child ->
            NodeContent(
              child,
              fontFamilyMap,
              modifier = childModifier,
              inheritedTextStyle = textStyle,
              uppercase = uppercase
            )
          }
        }
      }
    }

    else -> {
      // Generic block container
      Column(
        modifier = styledModifier.fillMaxWidth(),
        horizontalAlignment = if (isCentered) Alignment.CenterHorizontally else Alignment.Start
      ) {
        element.children.forEach { child ->
          NodeContent(child, fontFamilyMap, inheritedTextStyle = textStyle, uppercase = uppercase)
        }
      }
    }
  }
}

@Composable
private fun TableContent(
  table: Node.Table,
  fontFamilyMap: Map<String, FontFamily>,
  modifier: Modifier = Modifier
) {
  val density = LocalDensity.current
  Column(modifier = modifier.applyStyle(table.style, density).fillMaxWidth()) {
    table.rows.forEach { row ->
      Row(modifier = Modifier.fillMaxWidth()) {
        val cellCount = row.cells.size.coerceAtLeast(1)
        row.cells.forEach { cell ->
          Column(
            modifier = Modifier
              .weight(cell.colspan.toFloat())
              .applyStyle(cell.style, density)
          ) {
            cell.children.forEach { child ->
              NodeContent(child, fontFamilyMap)
            }
          }
          // Fill remaining weight if colspan > 1 would exceed cellCount
          if (cell.colspan > 1 && cellCount > row.cells.size) {
            Spacer(Modifier.weight((cell.colspan - 1).toFloat()))
          }
        }
      }
      HorizontalDivider(color = AppTheme.colors.borderRegular.copy(alpha = 0.3f))
    }
  }
}

@Composable
private fun BulletListContent(
  list: Node.BulletList,
  fontFamilyMap: Map<String, FontFamily>,
  modifier: Modifier = Modifier
) {
  val density = LocalDensity.current
  val listStyleType = list.style?.listStyleType
  Column(modifier = modifier.applyStyle(list.style, density)) {
    list.items.forEachIndexed { _, item ->
      Row(modifier = Modifier.applyStyle(item.style, density)) {
        // Marker
        val marker = when {
          !list.ordered -> when (listStyleType) {
            ListStyleType.Circle -> "○"
            ListStyleType.Square -> "■"
            ListStyleType.None -> ""
            else -> "•"
          }
          else -> when (listStyleType) {
            ListStyleType.LowerAlpha -> "${('a' + item.index - 1)}."
            ListStyleType.UpperAlpha -> "${('A' + item.index - 1)}."
            ListStyleType.LowerRoman -> "${item.index.toRoman().lowercase()}."
            ListStyleType.UpperRoman -> "${item.index.toRoman()}."
            ListStyleType.None -> ""
            else -> "${item.index}."
          }
        }
        if (marker.isNotEmpty()) {
          Text(
            text = marker,
            modifier = Modifier.width(28.dp),
            style = TextStyle(fontSize = 14.sp, hyphens = Hyphens.None)
          )
        } else {
          Spacer(Modifier.width(28.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
          item.children.forEach { child ->
            NodeContent(child, fontFamilyMap)
          }
        }
      }
    }
  }
}

private fun Int.toRoman(): String {
  val vals = intArrayOf(1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1)
  val syms = arrayOf("M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I")
  var num = this
  return buildString {
    for (i in vals.indices) {
      while (num >= vals[i]) { append(syms[i]); num -= vals[i] }
    }
  }
}

/**
 * Collects all inline text from [node]'s subtree into one [AnnotatedString].
 * Used for headings/inline-only elements where children were flattened to a single Node.Text.
 */
private fun collectAnnotated(
  node: Node,
  uppercase: Boolean
): AnnotatedString = buildAnnotatedString {
  when (node) {
    is Node.Text -> {
      val text = if (uppercase) node.copy(raw = node.raw.uppercase()) else node
      append(text.toAnnotatedString())
    }
    is Node.Image -> Unit
    is Node.HorizontalRule -> Unit
    is Node.BulletList -> Unit
    is Node.Table -> Unit
    is Node.Element -> node.children.forEach { append(collectAnnotated(it, uppercase)) }
  }
}
