package ru.kode.epub.core.ui.compose.modifiers

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.borderBottom(strokeWidth: Dp, color: Color, alpha: Float = 1f): Modifier {
  return drawBehind {
    val width = size.width
    val height = size.height - strokeWidth.toPx()

    drawLine(
      color = color,
      start = Offset(x = 0f, y = height),
      end = Offset(x = width, y = height),
      strokeWidth = strokeWidth.toPx(),
      alpha = alpha
    )
  }
}

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

fun Modifier.dashedBorder(
  color: Color,
  strokeWidth: Dp = 1.dp,
  cornerRadius: Dp = 0.dp,
  dashPattern: Array<Dp> = arrayOf(6.dp, 6.dp)
) = composed {
  val density = LocalDensity.current
  val strokeWidthPx = density.run { strokeWidth.toPx() }
  val radius = CornerRadius(density.run { cornerRadius.toPx() })
  val dashesPx = dashPattern.map { density.run { it.toPx() } }.toFloatArray()
  Modifier.drawBehind {
    val stroke = Stroke(
      width = strokeWidthPx,
      pathEffect = PathEffect.dashPathEffect(dashesPx)
    )
    drawRoundRect(
      color = color,
      style = stroke,
      cornerRadius = radius
    )
  }
}

fun Modifier.borderScrollIndicator(
  scrollableState: ScrollableState,
  direction: ScrollDirection,
  strokeWidth: Dp,
  color: Color
) = composed {
  when (direction) {
    ScrollDirection.Backward -> {
      val alpha by animateFloatAsState(targetValue = if (scrollableState.canScrollBackward) 1f else 0f)
      borderBottom(strokeWidth, color, alpha)
    }
    ScrollDirection.Forward -> {
      val alpha by animateFloatAsState(targetValue = if (scrollableState.canScrollForward) 1f else 0f)
      borderTop(strokeWidth, color, alpha)
    }
  }
}

enum class ScrollDirection {
  Backward,
  Forward
}
