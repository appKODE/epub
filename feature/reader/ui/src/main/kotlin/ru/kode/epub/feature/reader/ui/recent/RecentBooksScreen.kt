package ru.kode.epub.feature.reader.ui.recent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.kode.epub.core.ui.compose.modifiers.systemBarsPadding
import ru.kode.epub.core.ui.screen.AppScreen
import ru.kode.epub.core.uikit.theme.AppTheme

@Composable
fun RecentBooksScreen(
  viewModel: RecentBooksViewModel
) = AppScreen(viewModel) { state ->
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .systemBarsPadding()
      .background(AppTheme.colors.surfaceLayer1),
    contentPadding = PaddingValues(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 24.dp)
  ) {
    items(state.books) { book ->
      Text(
        modifier = Modifier
          .clickable(onClick = { viewModel.openBook(book) }),
        style = AppTheme.typography.body1,
        text = book.id
      )
    }
  }
}
