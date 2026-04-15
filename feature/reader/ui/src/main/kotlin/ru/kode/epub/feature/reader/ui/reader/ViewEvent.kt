package ru.kode.epub.feature.reader.ui.reader

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.kode.epub.core.ui.screen.event.ViewEvent
import ru.kode.epub.core.ui.screen.event.ViewEventHostScope
import ru.kode.epub.core.uikit.R
import ru.kode.epub.core.uikit.component.ModalBottomSheet
import ru.kode.epub.core.uikit.theme.AppTheme
import ru.kode.epub.lib.entity.TocEntry
import java.util.Locale

@Immutable
data class TocSheet(
  val toc: List<TocEntry>,
  val onChapterSelected: (TocEntry) -> Unit
) : ViewEvent.BottomSheet {
  @Composable
  override fun ViewEventHostScope.Content() {
    ModalBottomSheet(onDismissRequest = ::dismissEventPresentation) {
      Column(Modifier.navigationBarsPadding()) {
        TocSheetContent(
          toc = toc,
          onEntryClick = { entry ->
            onChapterSelected(entry)
            dismissEventPresentation()
          }
        )
      }
    }
  }
}

@Immutable
data class BookInfoSheet(
  val info: BookInfo
) : ViewEvent.BottomSheet {
  @Composable
  override fun ViewEventHostScope.Content() {
    ModalBottomSheet(onDismissRequest = ::dismissEventPresentation) {
      Column(Modifier.navigationBarsPadding()) {
        BookInfoSheetContent(info = info)
      }
    }
  }
}

// ─────────────────────────── TOC sheet content ──────────────────────────────

@Composable
private fun TocSheetContent(
  toc: List<TocEntry>,
  onEntryClick: (TocEntry) -> Unit
) {
  val expanded = remember { mutableStateMapOf<Int, Boolean>() }

  Text(
    text = "Содержание",
    style = AppTheme.typography.headline5,
    color = AppTheme.colors.textPrimary,
    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
  )
  HorizontalDivider()
  LazyColumn {
    toc.forEachIndexed { index, entry ->
      if (entry.children.isEmpty()) {
        item(key = "leaf-$index") {
          TocLeafRow(
            title = entry.title,
            indent = false,
            onClick = { onEntryClick(entry) }
          )
        }
      } else {
        item(key = "section-$index") {
          val isExpanded = expanded[index] ?: true
          TocSectionRow(
            title = entry.title,
            isExpanded = isExpanded,
            onToggle = { expanded[index] = !isExpanded }
          )
        }
        if (expanded[index] ?: true) {
          items(entry.children, key = { child -> "child-$index-${child.title}" }) { child ->
            TocLeafRow(
              title = child.title,
              indent = true,
              onClick = { onEntryClick(child) }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun TocSectionRow(
  title: String,
  isExpanded: Boolean,
  onToggle: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(start = 16.dp, end = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = title,
      style = AppTheme.typography.body1,
      fontWeight = FontWeight.Medium,
      color = AppTheme.colors.textPrimary,
      modifier = Modifier
        .weight(1f)
        .padding(vertical = 14.dp)
    )
    IconButton(onClick = onToggle) {
      Icon(
        painter = if (isExpanded) {
          painterResource(R.drawable.ic_chevron_up_24)
        } else {
          painterResource(R.drawable.ic_chevron_down_24)
        },
        contentDescription = null,
        tint = AppTheme.colors.iconSecondary
      )
    }
  }
}

@Composable
private fun TocLeafRow(
  title: String,
  indent: Boolean,
  onClick: () -> Unit
) {
  Text(
    text = title,
    style = AppTheme.typography.body2,
    color = AppTheme.colors.textPrimary,
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(
        start = if (indent) 32.dp else 16.dp,
        end = 16.dp,
        top = 14.dp,
        bottom = 14.dp
      )
  )
}

// ─────────────────────── Book info sheet content ────────────────────────────

@Composable
private fun BookInfoSheetContent(info: BookInfo) {
  LazyColumn(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    info.coverImage?.let { cover ->
      item {
        val bitmap = remember(cover.data) {
          BitmapFactory.decodeByteArray(cover.data, 0, cover.data.size)?.asImageBitmap()
        }
        if (bitmap != null) {
          Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 320.dp)
              .clip(RoundedCornerShape(8.dp))
          )
        }
      }
    }
    item {
      Spacer(Modifier.height(4.dp))
      Text(
        text = info.title,
        style = AppTheme.typography.headline4,
        color = AppTheme.colors.textPrimary
      )
    }
    if (info.author.isNotBlank()) {
      item { InfoRow(label = "Автор", value = info.author) }
    }
    if (info.categories.isNotEmpty()) {
      item { InfoRow(label = "Жанр", value = info.categories.joinToString(", ")) }
    }
    if (info.language.isNotBlank()) {
      item { InfoRow(label = "Язык", value = localizedLanguageName(info.language)) }
    }
    if (info.description.isNotBlank()) {
      item {
        Text(
          text = info.description,
          style = AppTheme.typography.body2,
          color = AppTheme.colors.textSecondary
        )
      }
    }
    item { Spacer(Modifier.height(8.dp)) }
  }
}

@Composable
private fun InfoRow(label: String, value: String) {
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = label,
      style = AppTheme.typography.body2,
      fontWeight = FontWeight.SemiBold,
      color = AppTheme.colors.textPrimary
    )
    Text(
      text = value,
      style = AppTheme.typography.body2,
      color = AppTheme.colors.textSecondary
    )
  }
}

private fun localizedLanguageName(languageTag: String): String {
  val locale = Locale.forLanguageTag(languageTag)
  return locale.getDisplayLanguage(locale)
    .replaceFirstChar { it.uppercase() }
    .takeIf { it.isNotBlank() }
    ?: languageTag
}
