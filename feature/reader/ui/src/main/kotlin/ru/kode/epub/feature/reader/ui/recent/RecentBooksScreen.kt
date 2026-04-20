package ru.kode.epub.feature.reader.ui.recent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ru.kode.epub.core.domain.entity.resRef
import ru.kode.epub.core.domain.randomUuid
import ru.kode.epub.core.ui.compose.modifiers.cutoutPadding
import ru.kode.epub.core.ui.content.UiMessage
import ru.kode.epub.core.ui.screen.AppScreen
import ru.kode.epub.core.uikit.component.ErrorMessage
import ru.kode.epub.core.uikit.modifier.shimmer
import ru.kode.epub.core.uikit.theme.AppTheme
import ru.kode.epub.feature.reader.domain.entity.Book
import ru.kode.epub.feature.reader.ui.R
import ru.kode.epub.feature.reader.ui.component.AddBookButton
import ru.kode.epub.feature.reader.ui.component.BookGridItem
import kotlin.time.Clock

@Composable
fun RecentBooksScreen(
  viewModel: RecentBooksViewModel
) = AppScreen(
  viewModel = viewModel
) { state ->
  Box(
    modifier = Modifier
      .cutoutPadding()
      .statusBarsPadding()
      .background(color = AppTheme.colors.surfaceBackground)
      .fillMaxSize()
  ) {
    if (!state.loading && state.books.isEmpty()) {
      BooksStub()
    } else {
      LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Adaptive(minSize = BookPreviewCardMinWidth),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        if (state.loading) {
          repeat(10) {
            item(key = it) {
              BookGridItem(
                modifier = Modifier
                  .shimmer(visible = true)
                  .animateItem(),
                book = placeholderBook
              )
            }
          }
        } else {
          items(items = state.books, key = { it.id }) { book ->
            BookGridItem(
              modifier = Modifier.animateItem(),
              book = book,
              onClick = { viewModel.openBook(book) },
              onRemoveClick = { viewModel.removeBook(book) }
            )
          }
        }
      }
    }
    AddBookButton(
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(end = 16.dp, bottom = 16.dp),
      onClick = viewModel::addBook
    )
  }
}

@Composable
private fun BooksStub(
  modifier: Modifier = Modifier
) {
  ErrorMessage(
    modifier = modifier
      .fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    message = UiMessage(
      illustration = UiMessage.Illustration.Default,
      title = resRef(R.string.books_stub_title),
      description = resRef(R.string.books_stub_description)
    )
  )
}

val BookPreviewCardMinWidth = 140.dp

private val placeholderBook = Book(
  id = randomUuid(),
  name = randomUuid(),
  author = randomUuid(),
  uri = android.net.Uri.parse(randomUuid()),
  updatedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
)
