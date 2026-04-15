package ru.kode.epub.feature.reader.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.byUnicodePattern
import ru.kode.epub.core.uikit.R
import ru.kode.epub.core.uikit.theme.AppTheme
import ru.kode.epub.feature.reader.domain.entity.Book

@Composable
internal fun Book(
  book: Book,
  modifier: Modifier = Modifier,
  backgroundColor: Color = AppTheme.colors.surfaceLayer1,
  progressColor: Color = AppTheme.colors.surfaceLayerAccentPale
) = Row(
  modifier = modifier
    .drawWithContent {
      drawRect(color = backgroundColor)
      drawRect(
        color = progressColor,
        size = Size(height = size.height, width = size.width * book.readProgress)
      )
      drawContent()
    }
    .padding(vertical = 10.dp, horizontal = 16.dp),
  verticalAlignment = Alignment.CenterVertically,
  horizontalArrangement = Arrangement.spacedBy(16.dp)
) {
  AsyncImage(
    model = book.cover,
    error = painterResource(R.drawable.ic_placeholder_24),
    contentDescription = "Book cover",
    modifier = Modifier
      .size(64.dp)
      .clip(RoundedCornerShape(8.dp))
      .border(
        border = BorderStroke(0.5.dp, AppTheme.colors.surfaceLayerOverlay),
        shape = RoundedCornerShape(8.dp)
      )
  )
  Column(
    modifier = Modifier.weight(1f),
    verticalArrangement = Arrangement.spacedBy(2.dp)
  ) {
    Text(
      text = book.name,
      style = AppTheme.typography.body2,
      color = AppTheme.colors.textPrimary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    Text(
      text = book.updatedAt.format(dateTimeFormat),
      style = AppTheme.typography.caption2,
      color = AppTheme.colors.textSecondary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
  Icon(
    painter = painterResource(id = R.drawable.ic_chevron_forward_24),
    contentDescription = "chevron icon",
    tint = AppTheme.colors.iconSecondary
  )
}

private val dateTimeFormat = LocalDateTime.Format {
  byUnicodePattern("yyyy-MM-dd HH:mm")
}
