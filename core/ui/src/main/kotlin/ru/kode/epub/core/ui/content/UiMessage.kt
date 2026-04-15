package ru.kode.epub.core.ui.content

import androidx.compose.runtime.Immutable
import ru.kode.epub.core.domain.entity.TextRef

@Immutable
data class UiMessage(
  val title: TextRef,
  val description: TextRef? = null,
  val primaryAction: Action? = null,
  val secondaryAction: Action? = null,
  val illustration: Illustration? = null
) {
  enum class Illustration {
    Default,
  }

  @Immutable
  data class Action(
    val name: TextRef,
    val listener: () -> Unit,
    val description: TextRef? = null
  ) {
    enum class Type {
      Generic
    }
  }
}
