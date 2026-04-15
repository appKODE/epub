package ru.kode.epub.feature.reader.ui.recent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.kode.epub.core.ui.compose.modifiers.cutoutPadding
import ru.kode.epub.core.ui.screen.AppScreen
import ru.kode.epub.core.uikit.theme.AppTheme
import ru.kode.epub.feature.reader.ui.component.AddBookButton
import ru.kode.epub.feature.reader.ui.component.Book

@Composable
fun RecentBooksScreen(
  viewModel: RecentBooksViewModel
) = AppScreen(viewModel) { state ->
  Box(
    modifier = Modifier
      .cutoutPadding()
      .statusBarsPadding()
      .background(color = AppTheme.colors.surfaceBackground)
      .fillMaxSize()
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(vertical = 24.dp, horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      items(items = state.books, key = { it.id }) { book ->
        Book(
          book = book,
          modifier = Modifier
            .animateItem()
            .clickable(onClick = { viewModel.openBook(book) })
        )
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
