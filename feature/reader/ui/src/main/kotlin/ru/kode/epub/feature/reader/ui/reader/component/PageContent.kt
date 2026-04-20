package ru.kode.epub.feature.reader.ui.reader.component

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import ru.kode.epub.feature.reader.ui.reader.LocalAvailableWidth
import ru.kode.epub.feature.reader.ui.reader.PAGE_HORIZONTAL_PADDING
import ru.kode.epub.feature.reader.ui.reader.PAGE_VERTICAL_PADDING
import ru.kode.epub.feature.reader.ui.reader.Page

@Composable
internal fun PageContent(
  pages: List<Page>,
  columnCount: Int,
  screenPageIndex: Int,
  fontFamilyMap: Map<String, FontFamily>,
  modifier: Modifier = Modifier
) {
  Row(modifier) {
    repeat(columnCount) { colIdx ->
      BoxWithConstraints(
        modifier = Modifier
          .weight(1f)
          .fillMaxSize()
      ) {
        CompositionLocalProvider(LocalAvailableWidth provides maxWidth - PAGE_HORIZONTAL_PADDING * 2) {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = PAGE_HORIZONTAL_PADDING, vertical = PAGE_VERTICAL_PADDING)
          ) {
            val calcIdx = screenPageIndex * columnCount + colIdx
            pages.getOrElse(calcIdx) { Page(emptyList()) }.items.forEach { indexed ->
              NodeContent(node = indexed.node, fontFamilyMap = fontFamilyMap)
            }
          }
        }
      }
    }
  }
}
