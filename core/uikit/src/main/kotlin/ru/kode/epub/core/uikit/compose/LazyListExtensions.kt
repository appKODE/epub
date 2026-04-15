package ru.kode.epub.core.uikit.compose

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

inline fun LazyListScope.itemsWithShape(
  count: Int,
  shape: CornerBasedShape = RoundedCornerShape(16.dp),
  crossinline itemContent: @Composable LazyItemScope.(shape: Shape, index: Int) -> Unit
) {
  items(count) { index ->
    val recalculatedShape = remember(index, count) {
      when {
        count == 1 -> shape
        index == 0 -> shape.copy(
          bottomStart = CornerSize(0),
          bottomEnd = CornerSize(0)
        )

        index == count - 1 -> shape.copy(
          topStart = CornerSize(0),
          topEnd = CornerSize(0)
        )

        else -> RectangleShape
      }
    }
    itemContent(recalculatedShape, index)
  }
}

inline fun <T> LazyListScope.itemsIndexedWithShape(
  items: List<T>,
  shape: CornerBasedShape = RoundedCornerShape(16.dp),
  noinline key: ((index: Int, item: T) -> Any)? = null,
  crossinline contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
  crossinline itemContent: @Composable LazyItemScope.(shape: Shape, index: Int, item: T) -> Unit
) {
  itemsIndexed(items, key, contentType) { index, item ->
    val recalculatedShape = remember(index, items.size) {
      when {
        items.size == 1 -> shape
        index == 0 -> shape.copy(
          bottomStart = CornerSize(0),
          bottomEnd = CornerSize(0)
        )

        index == items.lastIndex -> shape.copy(
          topStart = CornerSize(0),
          topEnd = CornerSize(0)
        )

        else -> RectangleShape
      }
    }
    itemContent(recalculatedShape, index, item)
  }
}
