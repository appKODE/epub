package ru.kode.epub.feature.reader.ui.reader

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.font.FontFamily
import ru.kode.epub.feature.reader.domain.entity.ColumnMode
import ru.kode.epub.feature.reader.domain.entity.PageScrollMode
import ru.kode.epub.feature.reader.domain.entity.TurnPageMode
import ru.kode.epub.lib.entity.Book

@Immutable
data class ViewState(
  val loading: Boolean = true,
  val book: Book? = null,
  val fontFamilyMap: Map<String, FontFamily> = emptyMap(),
  val scrollMode: PageScrollMode? = null,
  val columnMode: ColumnMode? = null,
  val turnPageMode: TurnPageMode? = null,
  val scrollToElementIndex: Int? = null,
  val currentElementIndex: Int? = null
)
