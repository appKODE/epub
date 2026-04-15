package ru.kode.epub.core.ui.screen

import androidx.compose.runtime.Immutable

@Immutable
data class ViewModelProvider(
  val key: String,
  val factory: Factory
) {
  fun interface Factory {
    fun build(vararg params: Any?): ViewModel<*, *>
  }
}
