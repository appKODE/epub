package ru.kode.epub.core.uikit.component

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import ru.kode.epub.core.ui.compose.modifiers.surface
import ru.kode.epub.core.uikit.theme.AppTheme
import kotlin.math.max
import kotlin.math.min

/**
 * A copy of [androidx.compose.material3.DropdownMenu] and all necessary components
 * without dependencies on [MaterialTheme] colors
 */
@Composable
fun DropdownMenu(
  expanded: Boolean,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
  offset: DpOffset = DpOffset(0.dp, 0.dp),
  properties: PopupProperties = PopupProperties(focusable = true),
  content: @Composable ColumnScope.() -> Unit
) {
  val expandedStates = remember { MutableTransitionState(false) }
  expandedStates.targetState = expanded

  if (expandedStates.currentState || expandedStates.targetState) {
    val transformOriginState = remember { mutableStateOf(TransformOrigin.Center) }
    val density = LocalDensity.current
    val popupPositionProvider = DropdownMenuPositionProvider(
      offset,
      density
    ) { parentBounds, menuBounds ->
      transformOriginState.value = calculateTransformOrigin(parentBounds, menuBounds)
    }

    Popup(
      onDismissRequest = onDismissRequest,
      popupPositionProvider = popupPositionProvider,
      properties = properties
    ) {
      DropdownMenuContent(
        expandedStates = expandedStates,
        transformOriginState = transformOriginState,
        modifier = modifier,
        content = content
      )
    }
  }
}

@Suppress("ModifierParameter")
@Composable
private fun DropdownMenuContent(
  expandedStates: MutableTransitionState<Boolean>,
  transformOriginState: MutableState<TransformOrigin>,
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit
) {
  // Menu open/close animation.
  val transition = updateTransition(expandedStates, "DropDownMenu")

  val scale by transition.animateFloat(
    transitionSpec = {
      if (false isTransitioningTo true) {
        // Dismissed to expanded
        tween(
          durationMillis = IN_TRANSITION_DURATION,
          easing = LinearOutSlowInEasing
        )
      } else {
        // Expanded to dismissed.
        tween(
          durationMillis = 1,
          delayMillis = OUT_TRANSITION_DURATION - 1
        )
      }
    },
    label = "menu_in_transition"
  ) {
    if (it) {
      // Menu is expanded.
      1f
    } else {
      // Menu is dismissed.
      0.8f
    }
  }

  val alpha by transition.animateFloat(
    transitionSpec = {
      if (false isTransitioningTo true) {
        // Dismissed to expanded
        tween(durationMillis = 30)
      } else {
        // Expanded to dismissed.
        tween(durationMillis = OUT_TRANSITION_DURATION)
      }
    },
    label = "menu_out_transition"
  ) {
    if (it) {
      // Menu is expanded.
      1f
    } else {
      // Menu is dismissed.
      0f
    }
  }
  Column(
    modifier = modifier
      .surface(
        backgroundColor = AppTheme.colors.surfaceLayer1,
        shape = RoundedCornerShape(DropdownMenuDefaultCornerRadius),
        elevation = MenuElevation
      )
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
        this.alpha = alpha
        transformOrigin = transformOriginState.value
      }
      .padding(vertical = DropdownMenuVerticalPadding)
      .width(IntrinsicSize.Max)
      .verticalScroll(rememberScrollState()),
    content = content
  )
}

@Composable
fun DropdownMenuItem(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
  content: @Composable RowScope.() -> Unit
) {
  DropdownMenuItemContent(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    interactionSource = interactionSource,
    content = content
  )
}

@Composable
internal fun DropdownMenuItemContent(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
  content: @Composable RowScope.() -> Unit
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      // Preferred min and max width used during the intrinsic measurement.
      .sizeIn(
        minWidth = DropdownMenuItemDefaultMinWidth,
        maxWidth = DropdownMenuItemDefaultMaxWidth,
        minHeight = DropdownMenuItemDefaultMinHeight
      )
      .clickable(
        enabled = enabled,
        onClick = onClick,
        interactionSource = interactionSource,
        indication = ripple(true)
      )
      .padding(horizontal = DropdownMenuItemDefaultHorizontalPadding),
    verticalAlignment = Alignment.CenterVertically
  ) {
    content()
  }
}

/**
 * Calculates the position of a Material [DropdownMenu].
 */
@Immutable
internal data class DropdownMenuPositionProvider(
  val contentOffset: DpOffset,
  val density: Density,
  val onPositionCalculated: (IntRect, IntRect) -> Unit = { _, _ -> }
) : PopupPositionProvider {
  override fun calculatePosition(
    anchorBounds: IntRect,
    windowSize: IntSize,
    layoutDirection: LayoutDirection,
    popupContentSize: IntSize
  ): IntOffset {
    // The min margin above and below the menu, relative to the screen.
    val verticalMargin = with(density) { MenuVerticalMargin.roundToPx() }
    // The content offset specified using the dropdown offset parameter.
    val contentOffsetX = with(density) { contentOffset.x.roundToPx() }
    val contentOffsetY = with(density) { contentOffset.y.roundToPx() }

    // Compute horizontal position.
    val toRight = anchorBounds.left + contentOffsetX
    val toLeft = anchorBounds.right - contentOffsetX - popupContentSize.width
    val toDisplayRight = windowSize.width - popupContentSize.width
    val toDisplayLeft = 0
    val x = if (layoutDirection == LayoutDirection.Ltr) {
      sequenceOf(
        toRight,
        toLeft,
        // If the anchor gets outside of the window on the left, we want to position
        // toDisplayLeft for proximity to the anchor. Otherwise, toDisplayRight.
        if (anchorBounds.left >= 0) toDisplayRight else toDisplayLeft
      )
    } else {
      sequenceOf(
        toLeft,
        toRight,
        // If the anchor gets outside of the window on the right, we want to position
        // toDisplayRight for proximity to the anchor. Otherwise, toDisplayLeft.
        if (anchorBounds.right <= windowSize.width) toDisplayLeft else toDisplayRight
      )
    }.firstOrNull {
      it >= 0 && it + popupContentSize.width <= windowSize.width
    } ?: toLeft

    // Compute vertical position.
    val toBottom = maxOf(anchorBounds.bottom + contentOffsetY, verticalMargin)
    val toTop = anchorBounds.top - contentOffsetY - popupContentSize.height
    val toCenter = anchorBounds.top - popupContentSize.height / 2
    val toDisplayBottom = windowSize.height - popupContentSize.height - verticalMargin
    val y = sequenceOf(toBottom, toTop, toCenter, toDisplayBottom).firstOrNull {
      it >= verticalMargin &&
        it + popupContentSize.height <= windowSize.height - verticalMargin
    } ?: toTop

    onPositionCalculated(
      anchorBounds,
      IntRect(x, y, x + popupContentSize.width, y + popupContentSize.height)
    )
    return IntOffset(x, y)
  }
}

internal fun calculateTransformOrigin(
  parentBounds: IntRect,
  menuBounds: IntRect
): TransformOrigin {
  val pivotX = when {
    menuBounds.left >= parentBounds.right -> 0f
    menuBounds.right <= parentBounds.left -> 1f
    menuBounds.width == 0 -> 0f
    else -> {
      val intersectionCenter =
        (
          max(parentBounds.left, menuBounds.left) +
            min(parentBounds.right, menuBounds.right)
          ) / 2
      (intersectionCenter - menuBounds.left).toFloat() / menuBounds.width
    }
  }
  val pivotY = when {
    menuBounds.top >= parentBounds.bottom -> 0f
    menuBounds.bottom <= parentBounds.top -> 1f
    menuBounds.height == 0 -> 0f
    else -> {
      val intersectionCenter =
        (
          max(parentBounds.top, menuBounds.top) +
            min(parentBounds.bottom, menuBounds.bottom)
          ) / 2
      (intersectionCenter - menuBounds.top).toFloat() / menuBounds.height
    }
  }
  return TransformOrigin(pivotX, pivotY)
}

// Menu open/close animation.
internal const val IN_TRANSITION_DURATION = 120
internal const val OUT_TRANSITION_DURATION = 75

// Size defaults.
private val MenuElevation = 8.dp
internal val MenuVerticalMargin = 48.dp
internal val DropdownMenuVerticalPadding = 8.dp
private val DropdownMenuItemDefaultMinWidth = 180.dp
private val DropdownMenuItemDefaultMaxWidth = 280.dp
private val DropdownMenuItemDefaultMinHeight = 42.dp
private val DropdownMenuItemDefaultHorizontalPadding = 24.dp
private val DropdownMenuDefaultCornerRadius = 16.dp
