package ru.kode.epub.core.uikit.modifier

import androidx.annotation.FloatRange
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import ru.kode.epub.core.uikit.theme.AppTheme
import kotlin.math.max

fun Modifier.shimmer(
  visible: Boolean,
  shape: Shape = RoundedCornerShape(16.dp)
): Modifier = composed {
  placeholder(
    visible = visible,
    shape = shape,
    color = AppTheme.colors.gradientSkeleton.first(),
    highlight = PlaceholderHighlight.shimmer(highlightColor = AppTheme.colors.gradientSkeleton.last()),
    fadeTransitionSpec = { tween() },
    contentFadeTransitionSpec = { tween() }
  )
}

internal fun PlaceholderHighlight.Companion.shimmer(
  highlightColor: Color,
  animationSpec: InfiniteRepeatableSpec<Float> = PlaceholderDefaults.shimmerAnimationSpec,
  @FloatRange(from = 0.0, to = 1.0) progressForMaxAlpha: Float = 0.6f
): PlaceholderHighlight = Shimmer(
  highlightColor = highlightColor,
  animationSpec = animationSpec,
  progressForMaxAlpha = progressForMaxAlpha
)

private data class Shimmer(
  private val highlightColor: Color,
  override val animationSpec: InfiniteRepeatableSpec<Float>,
  private val progressForMaxAlpha: Float = 0.6f
) : PlaceholderHighlight {
  override fun brush(
    progress: Float,
    size: Size
  ): Brush = Brush.radialGradient(
    colors = listOf(
      highlightColor.copy(alpha = 0f),
      highlightColor,
      highlightColor.copy(alpha = 0f)
    ),
    center = Offset(x = 0f, y = 0f),
    radius = (max(size.width, size.height) * progress * 2).coerceAtLeast(0.01f)
  )

  override fun alpha(progress: Float): Float = when {
    // From 0f...ProgressForOpaqueAlpha we animate from 0..1
    progress <= progressForMaxAlpha -> {
      lerp(
        start = 0f,
        stop = 1f,
        fraction = progress / progressForMaxAlpha
      )
    }
    // From ProgressForOpaqueAlpha..1f we animate from 1..0
    else -> {
      lerp(
        start = 1f,
        stop = 0f,
        fraction = (progress - progressForMaxAlpha) / (1f - progressForMaxAlpha)
      )
    }
  }
}

internal interface PlaceholderHighlight {
  /**
   * The optional [AnimationSpec] to use when running the animation for this highlight.
   */
  val animationSpec: InfiniteRepeatableSpec<Float>?

  /**
   * Return a [Brush] to draw for the given [progress] and [size].
   *
   * @param progress the current animated progress in the range of 0f..1f.
   * @param size The size of the current layout to draw in.
   */
  fun brush(
    @FloatRange(from = 0.0, to = 1.0) progress: Float,
    size: Size
  ): Brush

  /**
   * Return the desired alpha value used for drawing the [Brush] returned from [brush].
   *
   * @param progress the current animated progress in the range of 0f..1f.
   */
  @FloatRange(from = 0.0, to = 1.0)
  fun alpha(progress: Float): Float

  companion object
}

internal fun Modifier.placeholder(
  visible: Boolean,
  color: Color,
  shape: Shape = RectangleShape,
  highlight: PlaceholderHighlight? = null,
  fadeTransitionSpec: @Composable Transition.Segment<Boolean>.() -> FiniteAnimationSpec<Float> = { spring() },
  contentFadeTransitionSpec: @Composable Transition.Segment<Boolean>.() -> FiniteAnimationSpec<Float> = { spring() }
): Modifier = composed(
  inspectorInfo = debugInspectorInfo {
    name = "placeholder"
    value = visible
    properties["visible"] = visible
    properties["color"] = color
    properties["highlight"] = highlight
    properties["shape"] = shape
  }
) {
  // Values used for caching purposes
  val lastSize = remember { Ref<Size>() }
  val lastLayoutDirection = remember { Ref<LayoutDirection>() }
  val lastOutline = remember { Ref<Outline>() }

  // The current highlight animation progress
  var highlightProgress: Float by remember { mutableStateOf(0f) }

  // This is our crossfade transition
  val transitionState = remember { MutableTransitionState(visible) }.apply {
    targetState = visible
  }
  val transition = updateTransition(transitionState, "placeholder_crossfade")

  val placeholderAlpha by transition.animateFloat(
    transitionSpec = fadeTransitionSpec,
    label = "placeholder_fade",
    targetValueByState = { placeholderVisible -> if (placeholderVisible) 1f else 0f }
  )
  val contentAlpha by transition.animateFloat(
    transitionSpec = contentFadeTransitionSpec,
    label = "content_fade",
    targetValueByState = { placeholderVisible -> if (placeholderVisible) 0f else 1f }
  )

  // Run the optional animation spec and update the progress if the placeholder is visible
  val animationSpec = highlight?.animationSpec
  if (animationSpec != null && (visible || placeholderAlpha >= 0.01f)) {
    val infiniteTransition = rememberInfiniteTransition()
    highlightProgress = infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec = animationSpec
    ).value
  }

  val paint = remember { Paint() }
  remember(color, shape, highlight) {
    drawWithContent {
      // Draw the composable content first
      if (contentAlpha in 0.01f..0.99f) {
        // If the content alpha is between 1% and 99%, draw it in a layer with
        // the alpha applied
        paint.alpha = contentAlpha
        withLayer(paint) {
          with(this@drawWithContent) {
            drawContent()
          }
        }
      } else if (contentAlpha >= 0.99f) {
        // If the content alpha is > 99%, draw it with no alpha
        drawContent()
      }

      if (placeholderAlpha in 0.01f..0.99f) {
        // If the placeholder alpha is between 1% and 99%, draw it in a layer with
        // the alpha applied
        paint.alpha = placeholderAlpha
        withLayer(paint) {
          lastOutline.value = drawPlaceholder(
            shape = shape,
            color = color,
            highlight = highlight,
            progress = highlightProgress,
            lastOutline = lastOutline.value,
            lastLayoutDirection = lastLayoutDirection.value,
            lastSize = lastSize.value
          )
        }
      } else if (placeholderAlpha >= 0.99f) {
        // If the placeholder alpha is > 99%, draw it with no alpha
        lastOutline.value = drawPlaceholder(
          shape = shape,
          color = color,
          highlight = highlight,
          progress = highlightProgress,
          lastOutline = lastOutline.value,
          lastLayoutDirection = lastLayoutDirection.value,
          lastSize = lastSize.value
        )
      }

      // Keep track of the last size & layout direction
      lastSize.value = size
      lastLayoutDirection.value = layoutDirection
    }
  }
}

private fun DrawScope.drawPlaceholder(
  shape: Shape,
  color: Color,
  highlight: PlaceholderHighlight?,
  progress: Float,
  lastOutline: Outline?,
  lastLayoutDirection: LayoutDirection?,
  lastSize: Size?
): Outline? {
  // shortcut to avoid Outline calculation and allocation
  if (shape === RectangleShape) {
    // Draw the initial background color
    drawRect(color = color)

    if (highlight != null) {
      drawRect(
        brush = highlight.brush(progress, size),
        alpha = highlight.alpha(progress)
      )
    }
    // We didn't create an outline so return null
    return null
  }

  // Otherwise we need to create an outline from the shape
  val outline = lastOutline.takeIf {
    size == lastSize && layoutDirection == lastLayoutDirection
  } ?: shape.createOutline(size, layoutDirection, this)

  // Draw the placeholder color
  drawOutline(outline = outline, color = color)

  if (highlight != null) {
    drawOutline(
      outline = outline,
      brush = highlight.brush(progress, size),
      alpha = highlight.alpha(progress)
    )
  }

  // Return the outline we used
  return outline
}

private inline fun DrawScope.withLayer(
  paint: Paint,
  drawBlock: DrawScope.() -> Unit
) = drawIntoCanvas { canvas ->
  canvas.saveLayer(size.toRect(), paint)
  drawBlock()
  canvas.restore()
}

internal object PlaceholderDefaults {
  /**
   * The default [InfiniteRepeatableSpec] to use for [fade].
   */
  internal val fadeAnimationSpec: InfiniteRepeatableSpec<Float> by lazy {
    infiniteRepeatable(
      animation = tween(delayMillis = 200, durationMillis = 600),
      repeatMode = RepeatMode.Reverse
    )
  }

  /**
   * The default [InfiniteRepeatableSpec] to use for [shimmer].
   */
  internal val shimmerAnimationSpec: InfiniteRepeatableSpec<Float> by lazy {
    infiniteRepeatable(
      animation = tween(durationMillis = 1700, delayMillis = 200),
      repeatMode = RepeatMode.Restart
    )
  }
}
