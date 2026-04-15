package ru.kode.epub.core.uikit.touch

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode

fun Modifier.disableClickThrough() = this.clickable(false) {}

/**
 * differs from [disableClickThrough] in that it does not consume touch events
 * and allow it's children to receive them
 */
@Composable
fun Modifier.consumeClicks(): Modifier = composed {
  clickable(
    onClick = { },
    indication = NoIndication,
    interactionSource = remember { MutableInteractionSource() }
  )
}

private object NoIndication : IndicationNodeFactory {
  private object NoIndicationInstance : Modifier.Node(), DrawModifierNode {
    override fun ContentDrawScope.draw() = drawContent()
  }

  override fun create(interactionSource: InteractionSource): DelegatableNode = NoIndicationInstance

  override fun equals(other: Any?): Boolean = other is NoIndication

  override fun hashCode(): Int = NoIndicationInstance.hashCode()
}
