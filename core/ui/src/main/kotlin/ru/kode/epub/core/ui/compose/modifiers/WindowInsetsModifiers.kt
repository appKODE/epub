package ru.kode.epub.core.ui.compose.modifiers

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

fun Modifier.systemBarsPadding(
  start: Boolean = true,
  top: Boolean = true,
  end: Boolean = true,
  bottom: Boolean = true
): Modifier = composed {
  val insetsSides = ArrayList<WindowInsetsSides>(4)
  if (start) insetsSides.add(WindowInsetsSides.Start)
  if (top) insetsSides.add(WindowInsetsSides.Top)
  if (end) insetsSides.add(WindowInsetsSides.End)
  if (bottom) insetsSides.add(WindowInsetsSides.Bottom)
  if (insetsSides.isNotEmpty()) {
    this.then(
      Modifier.windowInsetsPadding(
        WindowInsets.systemBars.only(
          insetsSides.reduce { x, y -> x + y }
        )
      )
    )
  } else {
    this
  }
}

fun Modifier.cutoutPadding(
  start: Boolean = true,
  top: Boolean = true,
  end: Boolean = true,
  bottom: Boolean = true
): Modifier = composed {
  val insetsSides = ArrayList<WindowInsetsSides>(4)
  if (start) insetsSides.add(WindowInsetsSides.Start)
  if (top) insetsSides.add(WindowInsetsSides.Top)
  if (end) insetsSides.add(WindowInsetsSides.End)
  if (bottom) insetsSides.add(WindowInsetsSides.Bottom)
  if (insetsSides.isNotEmpty()) {
    this.then(
      Modifier.windowInsetsPadding(
        WindowInsets.displayCutout.only(
          insetsSides.reduce { x, y -> x + y }
        )
      )
    )
  } else {
    this
  }
}

fun Modifier.navigationBarsPadding(start: Boolean = true, end: Boolean = true, bottom: Boolean = true): Modifier =
  composed {
    val insetsSides = ArrayList<WindowInsetsSides>(3)
    if (start) insetsSides.add(WindowInsetsSides.Start)
    if (end) insetsSides.add(WindowInsetsSides.End)
    if (bottom) insetsSides.add(WindowInsetsSides.Bottom)
    if (insetsSides.isNotEmpty()) {
      this.then(
        Modifier.windowInsetsPadding(
          WindowInsets.systemBars.only(
            insetsSides.reduce { x, y -> x + y }
          )
        )
      )
    } else {
      this
    }
  }
