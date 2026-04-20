package ru.kode.epub.feature.reader.ui.reader

import android.content.Context
import androidx.compose.runtime.Stable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import ru.kode.epub.core.ui.screen.ViewModel
import ru.kode.epub.feature.reader.domain.ReaderModel
import ru.kode.epub.feature.reader.domain.entity.Book
import ru.kode.epub.feature.reader.domain.entity.ColumnMode
import ru.kode.epub.feature.reader.domain.entity.PageScrollMode
import ru.kode.epub.feature.reader.domain.entity.TurnPageMode
import ru.kode.epub.lib.EpubParser

@Stable
class ReaderViewModel @Inject constructor(
  private val context: Context,
  @Assisted val book: Book,
  private val model: ReaderModel
) : ViewModel<ViewState, Unit>() {

  override fun initialState() = ViewState()

  init {
    viewModelScope.launch {
      val (parsedBook, fontFamilyMap) = withContext(Dispatchers.IO) {
        val parsedBook = EpubParser.parse(context, book.uri)
        val fontFamilyMap = parsedBook.fontFaces.toFontFamilyMap()
        parsedBook to fontFamilyMap
      }
      val savedElementIndex = book.progress.positionKey.elementIdx.toInt()
      stateFlow.update {
        it.copy(
          loading = false,
          book = parsedBook,
          fontFamilyMap = fontFamilyMap,
          scrollToElementIndex = savedElementIndex.takeIf { idx -> idx > 0 }
        )
      }

      val totalNodes = parsedBook.chapters.sumOf { it.nodes.flattenToBlocks().size }
      model.updateBookTotalElements(book.id, totalNodes)
    }

    model.readerSettings
      .mapNotNull { settings ->
        settings.find { it.selected is PageScrollMode }?.selected as? PageScrollMode
      }
      .onEach { mode -> stateFlow.update { it.copy(scrollMode = mode) } }
      .launchIn(viewModelScope)

    model.readerSettings
      .mapNotNull { settings ->
        settings.find { it.selected is ColumnMode }?.selected as? ColumnMode
      }
      .onEach { mode -> stateFlow.update { it.copy(columnMode = mode) } }
      .launchIn(viewModelScope)

    model.readerSettings
      .mapNotNull { settings ->
        settings.find { it.selected is TurnPageMode }?.selected as? TurnPageMode
      }
      .onEach { mode -> stateFlow.update { it.copy(turnPageMode = mode) } }
      .launchIn(viewModelScope)
  }

  fun showBookInfo() {
    val parsedBook = stateFlow.value.book ?: return
    sendViewEvent(
      bookInfoSheet(
        metadata = parsedBook.metadata,
        coverImage = parsedBook.metadata.coverImage
      )
    )
  }

  fun showToc() {
    val parsedBook = stateFlow.value.book ?: return
    sendViewEvent(
      tocSheet(
        toc = parsedBook.toc,
        onChapterSelected = { chapterIndex ->
          val startIndex = parsedBook.chapters.take(chapterIndex).sumOf { it.nodes.flattenToBlocks().size }
          stateFlow.update { it.copy(scrollToElementIndex = startIndex) }
        }
      )
    )
  }

  fun onScrollHandled() {
    stateFlow.update { it.copy(scrollToElementIndex = null) }
  }

  fun onPageChanged(chapterIndex: Int, relativeIndex: Int, globalIndex: Int) {
    val positionKey = "$chapterIndex-$relativeIndex-$globalIndex"
    model.updateBookPosition(book.id, positionKey)
    stateFlow.update { it.copy(currentElementIndex = globalIndex) }
  }
}
