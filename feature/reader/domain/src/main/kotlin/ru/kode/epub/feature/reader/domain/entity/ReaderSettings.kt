package ru.kode.epub.feature.reader.domain.entity

import androidx.compose.runtime.Immutable
import ru.kode.epub.core.domain.entity.TextRef
import ru.kode.epub.core.domain.entity.emptyTextRef
import ru.kode.epub.core.domain.entity.strRef

@Immutable
data class ReaderSettings(
  val key: Key,
  val name: TextRef,
  val description: TextRef?,
  val selected: Entry?,
  val available: List<Entry>,
  val defaultSelected: () -> Entry
) {

  @JvmInline
  value class Key(val value: String)

  @Immutable
  sealed interface Entry {
    val displayName: TextRef
    val description: TextRef
    val value: String
  }
}

enum class SelectorSettings(
  val key: String,
  val displayName: TextRef,
  val description: TextRef? = null,
  val entries: List<ReaderSettings.Entry>
) {
  AppThemeMode(
    key = "NightMode",
    displayName = strRef("Night mode"),
    entries = NightMode.entries
  ),
  PageScroll(
    key = "PageScroll",
    displayName = strRef("Page scroll mode"),
    entries = PageScrollMode.entries
  ),
  Columns(
    key = "Columns",
    displayName = strRef("Text columns"),
    entries = ColumnMode.entries
  ),
}

enum class NightMode(
  override val displayName: TextRef,
  override val value: String,
  override val description: TextRef = emptyTextRef()
) : ReaderSettings.Entry {
  Day(strRef("Light"), "day"),
  Night(strRef("Dark"), "night"),
  Auto(strRef("System"), "system"),
}

enum class PageScrollMode(
  override val displayName: TextRef,
  override val value: String,
  override val description: TextRef = emptyTextRef()
) : ReaderSettings.Entry {
  Vertical(strRef("Vertical"), "day"),
  Horizontal(strRef("Horizontal"), "night"),
}

enum class ColumnMode(
  override val displayName: TextRef,
  override val value: String,
  override val description: TextRef = emptyTextRef()
) : ReaderSettings.Entry {
  Single(strRef("Single"), "single"),
  Double(strRef("Double"), "double"),
}

fun readerSettings(): List<ReaderSettings> {
  return SelectorSettings.entries.map { setting ->
    ReaderSettings(
      key = ReaderSettings.Key(setting.key),
      name = setting.displayName,
      description = setting.description,
      selected = null,
      available = setting.entries,
      defaultSelected = setting::getDefaultValue
    )
  }
}

internal fun SelectorSettings.getDefaultValue(): ReaderSettings.Entry {
  return when (this) {
    SelectorSettings.AppThemeMode -> NightMode.Auto
    SelectorSettings.PageScroll -> PageScrollMode.Vertical
    SelectorSettings.Columns -> ColumnMode.Single
  }
}
