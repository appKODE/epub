package ru.kode.epub.core.uikit.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.kode.epub.core.uikit.theme.AppTheme
import ru.kode.epub.core.uikit.touch.disableClickThrough

@Composable
fun CircularLoaderWithOverlay(modifier: Modifier = Modifier, strokeWidth: Dp = 8.dp, loaderSize: Dp = 48.dp) {
  Box(
    modifier = modifier
      .disableClickThrough()
      .background(color = AppTheme.colors.surfaceLayerOverlay),
    contentAlignment = Alignment.Center
  ) {
    CircularLoader(strokeWidth = strokeWidth, minSize = loaderSize)
  }
}

@Composable
fun CircularLoader(
  modifier: Modifier = Modifier,
  strokeWidth: Dp = 8.dp,
  minSize: Dp = 48.dp,
  color: Color = AppTheme.colors.iconAccent
) {
  val startColor = Color.Transparent
  val infiniteTransition = rememberInfiniteTransition()
  val angle = infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 800, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "loading transition"
  )
  Box(
    modifier = modifier
      .defaultMinSize(minSize, minSize)
      .drawBehind {
        rotate(degrees = angle.value) {
          drawArc(
            brush = Brush.sweepGradient(listOf(startColor, color)),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(
              (size.width * .25f + strokeWidth.toPx()) / 2,
              (size.height * .25f + strokeWidth.toPx()) / 2
            ),
            size = Size(
              width = (size.width * .75f - strokeWidth.toPx()),
              height = (size.height * .75f - strokeWidth.toPx())
            ),
            style = Stroke(width = strokeWidth.toPx())
          )
        }
      }
  )
}
