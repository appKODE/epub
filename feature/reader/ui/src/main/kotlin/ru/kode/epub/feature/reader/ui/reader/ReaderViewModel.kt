package ru.kode.epub.feature.reader.ui.reader

import android.content.Context
import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import ru.kode.epub.core.ui.screen.ViewModel
import ru.kode.epub.feature.reader.domain.ReaderModel
import ru.kode.epub.feature.reader.domain.entity.Book
import ru.kode.epub.feature.reader.domain.entity.PageScrollMode
import ru.kode.epub.lib.EpubParser
import ru.kode.epub.lib.entity.EpubBook
import ru.kode.epub.lib.entity.TocEntry

@Stable
class ReaderViewModel @Inject constructor(
  context: Context,
  private val model: ReaderModel,
  @Assisted val book: Book
) : ViewModel<ViewState, Unit>() {
  val epub = EpubParser.parse(context, book.uri)

  override fun initialState() = ViewState()

  override fun onStart() {
    stateFlow.update {
      it.copy(
        elements = epub.flattenElements(),
        toc = epub.toc,
        tocAnchors = epub.buildTocAnchors(),
        bookInfo = BookInfo(
          title = epub.title,
          author = epub.author,
          coverImage = epub.coverImage,
          categories = epub.categories,
          language = epub.language,
          description = epub.description
        ),
        fontFiles = epub.fontFiles
      )
    }

    model.readerSettings
      .mapNotNull { settings ->
        settings.find { it.selected is PageScrollMode }?.selected as? PageScrollMode
      }
      .onEach { scrollMode ->
        stateFlow.update { it.copy(scrollMode = scrollMode) }
      }
      .launchIn(viewModelScope)
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

  fun scrollToElement(elementIndex: Int) {
    stateFlow.update { it.copy(scrollToElementIndex = elementIndex) }
  }

  fun onCurrentPageChanged(elementIndex: Int) {
    stateFlow.update { it.copy(currentElementIndex = elementIndex) }
  }

  private fun selectTocEntry(entry: TocEntry) {
    val baseIndex = epub.chapters.take(entry.chapterIndex).sumOf { it.elements.size }
    val anchorOffset = entry.anchorId
      ?.let { epub.chapters.getOrNull(entry.chapterIndex)?.anchorIndex?.get(it) }
      ?: 0
    stateFlow.update { it.copy(scrollToElementIndex = baseIndex + anchorOffset) }
  }

  private fun EpubBook.buildTocAnchors(): List<TocAnchor> {
    val result = mutableListOf<TocAnchor>()
    fun collect(entries: List<TocEntry>) {
      for (entry in entries) {
        val baseIndex = chapters.take(entry.chapterIndex).sumOf { it.elements.size }
        val anchorOffset = entry.anchorId
          ?.let { chapters.getOrNull(entry.chapterIndex)?.anchorIndex?.get(it) }
          ?: 0
        result.add(TocAnchor(elementIndex = baseIndex + anchorOffset, entry = entry))
        collect(entry.children)
      }
    }
    collect(toc)
    return result.sortedBy { it.elementIndex }
  }

  private fun EpubBook.flattenElements(): List<IndexedElement> {
    // Collect global element indices of all TOC anchors (at any nesting level)
    val tocAnchorIndices = mutableSetOf<Int>()
    fun collectTocIndices(entries: List<TocEntry>) {
      for (entry in entries) {
        val baseIndex = chapters.take(entry.chapterIndex).sumOf { it.elements.size }
        val anchorOffset = entry.anchorId
          ?.let { chapters.getOrNull(entry.chapterIndex)?.anchorIndex?.get(it) }
          ?: 0
        tocAnchorIndices.add(baseIndex + anchorOffset)
        collectTocIndices(entry.children)
      }
    }
    collectTocIndices(toc)

    val result = mutableListOf<IndexedElement>()
    chapters.forEachIndexed { chapterIdx, chapter ->
      chapter.elements.forEachIndexed { elemIdx, element ->
        val globalIndex = result.size
        result.add(
          IndexedElement(
            key = "$chapterIdx-$elemIdx",
            index = globalIndex,
            isChapterStart = elemIdx == 0 || globalIndex in tocAnchorIndices,
            element = element
          )
        )
      }
    }
    return result
  }
}
