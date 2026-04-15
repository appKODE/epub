package ru.kode.epub.feature.reader.ui.reader

import android.content.Context
import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.update
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import ru.kode.epub.core.ui.screen.ViewModel
import ru.kode.epub.feature.reader.domain.entity.Book
import ru.kode.epub.lib.EpubParser
import ru.kode.epub.lib.entity.EpubBook
import ru.kode.epub.lib.entity.TocEntry

@Stable
class ReaderViewModel @Inject constructor(
  private val context: Context,
  @Assisted val book: Book
) : ViewModel<ViewState, Unit>() {
  val epub = EpubParser.parse(context, book.uri)

  override fun initialState() = ViewState()

  override fun onStart() {
    stateFlow.value = ViewState(
      elements = epub.flattenElements(),
      toc = epub.toc,
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
    val baseIndex = epub.chapters.take(entry.chapterIndex).sumOf { it.elements.size }
    val anchorOffset = entry.anchorId
      ?.let { epub.chapters.getOrNull(entry.chapterIndex)?.anchorIndex?.get(it) }
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
