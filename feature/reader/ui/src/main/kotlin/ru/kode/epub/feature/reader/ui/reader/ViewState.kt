package ru.kode.epub.feature.reader.ui.reader

import androidx.compose.runtime.Immutable
import ru.kode.epub.feature.reader.domain.entity.PageScrollMode
import ru.kode.epub.lib.entity.ContentElement
import ru.kode.epub.lib.entity.EpubFontFile
import ru.kode.epub.lib.entity.TocEntry

@Immutable
data class ViewState(
  val elements: List<IndexedElement> = emptyList(),
  val toc: List<TocEntry> = emptyList(),
  val tocAnchors: List<TocAnchor> = emptyList(),
  val bookInfo: BookInfo = BookInfo(),
  val fontFiles: List<EpubFontFile> = emptyList(),
  val isTopBarVisible: Boolean = true,
  val currentElementIndex: Int? = null,
  val scrollToElementIndex: Int? = null,
  val scrollMode: PageScrollMode? = null
)

@Immutable
data class TocAnchor(
  val elementIndex: Int,
  val entry: TocEntry
)

@Immutable
data class IndexedElement(
  val key: String,
  val index: Int,
  val isChapterStart: Boolean = false,
  val element: ContentElement
)

@Immutable
data class BookInfo(
  val title: String = "",
  val author: String = "",
  val coverImage: ContentElement.EpubImage? = null,
  val categories: List<String> = emptyList(),
  val language: String = "",
  val description: String = ""
)
