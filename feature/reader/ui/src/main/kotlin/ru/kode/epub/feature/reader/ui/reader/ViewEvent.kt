package ru.kode.epub.feature.reader.ui.reader

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
import ru.kode.epub.lib.entity.CoverImage
import ru.kode.epub.lib.entity.Metadata
import ru.kode.epub.lib.entity.TocItem
import java.util.Locale

internal fun tocSheet(
  toc: List<TocItem>,
  onChapterSelected: (Int) -> Unit
) = object : ViewEvent.BottomSheet {
  @Composable
  override fun ViewEventHostScope.Content() {
    ModalBottomSheet(onDismissRequest = ::dismissEventPresentation, containerColor = AppTheme.colors.surfaceReader) {
      TocSheetContent(
        toc = toc,
        onEntryClick = { chapterIndex ->
          onChapterSelected(chapterIndex)
          dismissEventPresentation()
        },
        modifier = Modifier.navigationBarsPadding()
      )
    }
  }
}

internal fun bookInfoSheet(
  metadata: Metadata,
  coverImage: CoverImage?
) = object : ViewEvent.BottomSheet {
  @Composable
  override fun ViewEventHostScope.Content() {
    ModalBottomSheet(onDismissRequest = ::dismissEventPresentation, containerColor = AppTheme.colors.surfaceReader) {
      BookInfoSheetContent(
        metadata = metadata,
        coverImage = coverImage,
        modifier = Modifier.navigationBarsPadding()
      )
    }
  }
}

// ─────────────────────────── TOC sheet ──────────────────────────────────────

@Composable
private fun TocSheetContent(
  toc: List<TocItem>,
  onEntryClick: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  // key: index in the top-level toc list; value: expanded state
  val expanded = remember { mutableStateMapOf<Int, Boolean>() }

  Text(
    text = "Contents",
    style = AppTheme.typography.headline5,
    color = AppTheme.colors.textPrimary,
    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
  )
  HorizontalDivider()
  LazyColumn(modifier = modifier) {
    toc.forEachIndexed { index, item ->
      if (item.children.isEmpty()) {
        item(key = "leaf-$index") {
          TocLeafRow(
            title = item.title,
            indent = false,
            onClick = { onEntryClick(item.chapterIndex) }
          )
        }
      } else {
        item(key = "section-$index") {
          val isExpanded = expanded[index] ?: true
          Column {
            TocSectionRow(
              title = item.title,
              isExpanded = isExpanded,
              onToggle = { expanded[index] = !isExpanded }
            )
            AnimatedVisibility(
              visible = isExpanded,
              enter = expandVertically() + fadeIn(tween(200)),
              exit = shrinkVertically() + fadeOut(tween(150))
            ) {
              Column {
                item.children.forEach { child ->
                  TocLeafRow(
                    title = child.title,
                    indent = true,
                    onClick = { onEntryClick(child.chapterIndex) }
                  )
                }
              }
            }
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
    val chevronRotation by animateFloatAsState(
      targetValue = if (isExpanded) 0f else 180f,
      animationSpec = tween(durationMillis = 250)
    )
    IconButton(onClick = onToggle) {
      Icon(
        painter = painterResource(R.drawable.ic_chevron_up_24),
        contentDescription = null,
        tint = AppTheme.colors.iconSecondary,
        modifier = Modifier.rotate(chevronRotation)
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

// ─────────────────────── Book info sheet ────────────────────────────────────

@Composable
private fun BookInfoSheetContent(
  metadata: Metadata,
  coverImage: CoverImage?,
  modifier: Modifier = Modifier
) {
  LazyColumn(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp)
  ) {
    coverImage?.let { cover ->
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
        text = metadata.title,
        style = AppTheme.typography.headline4,
        color = AppTheme.colors.textPrimary
      )
    }
    if (metadata.author.isNotBlank()) {
      item { InfoRow(label = "Author", value = metadata.author) }
    }
    if (metadata.categories.isNotEmpty()) {
      item { InfoRow(label = "Genre", value = metadata.categories.joinToString(", ")) }
    }
    if (metadata.language.isNotBlank()) {
      item { InfoRow(label = "Language", value = localizedLanguageName(metadata.language)) }
    }
    if (metadata.description.isNotBlank()) {
      item {
        Text(
          text = metadata.description,
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
