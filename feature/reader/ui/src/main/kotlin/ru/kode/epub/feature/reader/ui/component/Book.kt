package ru.kode.epub.feature.reader.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.error
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ru.kode.epub.core.domain.randomUuid
import ru.kode.epub.core.uikit.R
import ru.kode.epub.core.uikit.component.VSpacer
import ru.kode.epub.core.uikit.component.preview.PreviewColumn
import ru.kode.epub.core.uikit.modifier.shimmer
import ru.kode.epub.core.uikit.theme.AppTheme
import ru.kode.epub.feature.reader.domain.entity.Book
import kotlin.time.Clock

@Composable
fun BookGridItem(
  book: Book,
  modifier: Modifier = Modifier,
  backgroundColor: Color = AppTheme.colors.surfaceLayer1,
  progressColor: Color = AppTheme.colors.surfaceLayerAccentPale,
  onClick: () -> Unit = { },
  onRemoveClick: () -> Unit = { }
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(24.dp))
      .clickable(onClick = onClick)
  ) {
    Column {
      val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current)
          .data(book.cover)
          .error(R.drawable.ic_placeholder_24)
          .crossfade(true)
          .build(),
        imageLoader = LocalContext.current.imageLoader
      )
      val loaderState by painter.state.collectAsState()
      Image(
        modifier = Modifier
          .aspectRatio(0.625f)
          .shimmer(visible = loaderState is AsyncImagePainter.State.Loading),
        painter = painter,
        contentDescription = "book cover",
        contentScale = ContentScale.Crop
      )

      Column(
        modifier = Modifier
          .drawWithContent {
            drawRect(color = backgroundColor)
            drawRect(
              color = progressColor,
              size = Size(height = size.height, width = size.width * book.readProgress)
            )
            drawContent()
          }
          .fillMaxWidth()
          .padding(12.dp)
      ) {
        Text(
          text = book.name,
          style = AppTheme.typography.headline5,
          color = AppTheme.colors.textPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        VSpacer(size = 6.dp)
        Text(
          text = book.author,
          maxLines = 1,
          style = AppTheme.typography.caption1,
          color = AppTheme.colors.textPrimary
        )
      }
    }
    Box(
      modifier = Modifier
        .align(Alignment.TopEnd)
        .clickable(onClick = onRemoveClick)
        .padding(8.dp)
        .clip(CircleShape)
        .background(AppTheme.colors.surfaceBackground)
        .padding(horizontal = 3.dp, vertical = 3.dp),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        modifier = Modifier.size(24.dp),
        painter = painterResource(R.drawable.ic_trash_24),
        contentDescription = "remove book icon",
        tint = AppTheme.colors.iconNegative
      )
    }
  }
}

@Preview
@Composable
private fun BookGridItemPreview() {
  PreviewColumn(backgroundColor = Color.Black) {
    BookGridItem(
      modifier = Modifier
        .width(200.dp)
        .heightIn(min = 300.dp),
      book = Book(
        id = randomUuid(),
        name = "Long book title",
        author = "Some Authorson",
        uri = android.net.Uri.parse(randomUuid()),
        cover = null,
        updatedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
        progress = Book.Progress(
          chapter = 50L,
          tag = 1L
        ),
        totalChapters = 100L
      ),
      onClick = { },
      onRemoveClick = { }
    )
  }
}
