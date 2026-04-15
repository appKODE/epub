package ru.kode.epub.feature.reader.ui.reader

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.update
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import ru.kode.epub.core.ui.screen.ViewModel
import ru.kode.epub.lib.entity.EpubBook
import ru.kode.epub.lib.entity.TocEntry

@Stable
class ReaderViewModel @Inject constructor(
  @Assisted val book: EpubBook
) : ViewModel<ViewState, Unit>() {

  override fun initialState() = ViewState()

  override fun onStart() {
    stateFlow.value = ViewState(
      elements = book.flattenElements(),
      toc = book.toc,
      bookInfo = BookInfo(
        title = book.title,
        author = book.author,
        coverImage = book.coverImage,
        categories = book.categories,
        language = book.language,
        description = book.description
      ),
      fontFiles = book.fontFiles
    )
  }

  fun toggleTopBar() {
    stateFlow.update { it.copy(isTopBarVisible = !it.isTopBarVisible) }
  }

  fun showToc() {
    sendViewEvent(
      TocSheet(
        toc = stateFlow.value.toc,
        onChapterSelected = { entry -> selectTocEntry(entry) }
      )
    )
  }

  fun showBookInfo() {
    sendViewEvent(BookInfoSheet(info = stateFlow.value.bookInfo))
  }

  fun onScrollHandled() {
    stateFlow.update { it.copy(scrollToElementIndex = null) }
  }

  private fun selectTocEntry(entry: TocEntry) {
    val baseIndex = book.chapters.take(entry.chapterIndex).sumOf { it.elements.size }
    val anchorOffset = entry.anchorId
      ?.let { book.chapters.getOrNull(entry.chapterIndex)?.anchorIndex?.get(it) }
      ?: 0
    stateFlow.update { it.copy(scrollToElementIndex = baseIndex + anchorOffset) }
  }

  private fun EpubBook.flattenElements(): List<IndexedElement> {
    val result = mutableListOf<IndexedElement>()
    chapters.forEachIndexed { chapterIdx, chapter ->
      chapter.elements.forEachIndexed { elemIdx, element ->
        result.add(IndexedElement(key = "$chapterIdx-$elemIdx", element = element))
      }
    }
    return result
  }
}
