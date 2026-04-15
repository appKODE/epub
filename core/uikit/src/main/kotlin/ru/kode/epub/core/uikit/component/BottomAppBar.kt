package ru.kode.epub.core.uikit.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.kode.epub.core.domain.entity.TextRef
import ru.kode.epub.core.domain.entity.strRef
import ru.kode.epub.core.ui.compose.modifiers.surface
import ru.kode.epub.core.ui.compose.resolveRef
import ru.kode.epub.core.uikit.R
import ru.kode.epub.core.uikit.theme.AppTheme

@Composable
fun HorizontalBottomAppBar(
  tabs: List<Tab>,
  onTabClick: (Tab.Id) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .background(AppTheme.colors.surfaceLayer1),
    verticalAlignment = Alignment.CenterVertically
  ) {
    tabs.forEach { tab ->
      val interactionSource = remember { MutableInteractionSource() }
      BottomBarTab(
        modifier = Modifier
          .heightIn(min = 56.dp)
          .weight(1f)
          .clickable(
            interactionSource = interactionSource,
            indication = ripple(bounded = true, radius = 36.dp)
          ) { onTabClick(tab.id) },
        iconResId = tab.iconResId,
        titleRef = tab.titleRef,
        isActive = tab.isActive
      )
    }
  }
}

@Composable
fun VerticalBottomAppBar(
  tabs: List<Tab>,
  onTabClick: (Tab.Id) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .surface(
        backgroundColor = AppTheme.colors.surfaceLayer1,
        shape = RoundedCornerShape(20.dp)
      ),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    tabs.forEach { tab ->
      val interactionSource = remember { MutableInteractionSource() }
      BottomBarTab(
        modifier = Modifier
          .clickable(
            interactionSource = interactionSource,
            indication = ripple(bounded = true, radius = 36.dp)
          ) { onTabClick(tab.id) }
          .padding(8.dp)
          .weight(1f),
        iconResId = tab.iconResId,
        titleRef = tab.titleRef,
        isActive = tab.isActive
      )
    }
  }
}

@Composable
private fun BottomBarTab(
  @DrawableRes
  iconResId: Int,
  titleRef: TextRef,
  isActive: Boolean,
  modifier: Modifier = Modifier
) {
  val iconTint by animateColorAsState(
    targetValue = if (isActive) {
      AppTheme.colors.surfaceLayerAccent
    } else {
      AppTheme.colors.iconTertiary
    },
    animationSpec = tween(150)
  )
  val titleColor by animateColorAsState(
    targetValue = if (isActive) {
      AppTheme.colors.textPrimary
    } else {
      AppTheme.colors.iconTertiary
    },
    animationSpec = tween(150)
  )
  Column(
    modifier = modifier.defaultMinSize(56.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp, alignment = Alignment.CenterVertically),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Icon(
      painter = painterResource(id = iconResId),
      contentDescription = "bottom bar icon",
      tint = iconTint
    )

    Text(
      text = resolveRef(titleRef),
      style = AppTheme.typography.caption1,
      color = titleColor
    )
  }
}

@Immutable
data class Tab(
  val id: Id,
  @DrawableRes
  val iconResId: Int,
  val titleRef: TextRef,
  val isActive: Boolean
) {
  @JvmInline
  value class Id(val value: Any)
}

@Preview
@Composable
private fun HorizontalBottomAppBarPreview() {
  AppTheme(useDarkTheme = false) {
    Box(modifier = Modifier.width(320.dp)) {
      HorizontalBottomAppBar(
        listOf(
          Tab(
            id = Tab.Id(1),
            iconResId = R.drawable.ic_book_24,
            titleRef = strRef("Recent books"),
            isActive = true
          ),
          Tab(
            id = Tab.Id(2),
            iconResId = R.drawable.ic_settings_24,
            titleRef = strRef("Settings"),
            isActive = false
          )
        ),
        onTabClick = { }
      )
    }
  }
}

@Preview
@Composable
private fun VerticalBottomAppBarPreview() {
  AppTheme(useDarkTheme = false) {
    Box(modifier = Modifier.height(320.dp)) {
      VerticalBottomAppBar(
        listOf(
          Tab(
            id = Tab.Id(1),
            iconResId = R.drawable.ic_book_24,
            titleRef = strRef("Recent books"),
            isActive = true
          ),
          Tab(
            id = Tab.Id(2),
            iconResId = R.drawable.ic_settings_24,
            titleRef = strRef("Settings"),
            isActive = false
          )
        ),
        onTabClick = { }
      )
    }
  }
}
