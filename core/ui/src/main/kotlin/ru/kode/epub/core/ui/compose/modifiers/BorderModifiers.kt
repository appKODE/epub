package ru.kode.epub.core.ui.compose.modifiers

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.borderTop(
  strokeWidth: Dp,
  color: Color,
  alpha: Float = 1f,
  startIndent: Dp = 0.dp
): Modifier {
  return drawWithContent {
    drawContent()
    drawLine(
      color = color,
      start = Offset(x = startIndent.toPx(), y = 0f),
      end = Offset(x = size.width, y = 0f),
      strokeWidth = strokeWidth.toPx(),
      alpha = alpha
    )
  }
}

fun Modifier.borderBottom(
  strokeWidth: Dp,
  color: Color,
  alpha: Float = 1f,
  startIndent: Dp = 0.dp
): Modifier {
  return drawWithContent {
    drawContent()
    drawLine(
      color = color,
      start = Offset(x = startIndent.toPx(), y = size.height),
      end = Offset(x = size.width, y = size.height),
      strokeWidth = strokeWidth.toPx(),
      alpha = alpha
    )
  }
}
