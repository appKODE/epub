package ru.kode.epub.core.uikit.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.kode.epub.core.uikit.R
import ru.kode.epub.core.uikit.theme.AppTheme

@Composable
fun <T : Any> DropdownField(
  options: List<T>,
  selected: T,
  onSelect: (T) -> Unit,
  label: String,
  modifier: Modifier = Modifier,
  hint: String? = null,
  hasError: Boolean = false,
  key: (T) -> Any = { it.hashCode() },
  title: @Composable (T) -> String = { it.toString() }
) {
  var expanded by remember(selected) { mutableStateOf(false) }
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Text(
      modifier = Modifier.padding(start = 16.dp),
      text = label,
      style = AppTheme.typography.subhead2,
      color = AppTheme.colors.textSecondary
    )
    FieldDecoration(
      content = {
        Text(
          text = title(selected),
          style = AppTheme.typography.subhead1,
          color = AppTheme.colors.textSecondary
        )
        Dropdown(
          expanded = expanded,
          options = options,
          selected = selected,
          key = key,
          title = title,
          onSelect = { option ->
            onSelect(option)
            expanded = !expanded
          },
          onDismiss = { expanded = !expanded }
        )
      },
      trailingContent = {
        Icon(
          painter = painterResource(id = R.drawable.ic_chevron_up_and_down_24),
          contentDescription = "show dropdown options",
          tint = AppTheme.colors.iconTertiary
        )
      },
      modifier = Modifier.clickable { expanded = !expanded }
    )
    FormFieldHint(hasError, hint.orEmpty())
  }
}

@Composable
private fun <T : Any> Dropdown(
  expanded: Boolean,
  options: List<T>,
  selected: T,
  onSelect: (T) -> Unit,
  key: (T) -> Any = { it.hashCode() },
  title: @Composable (T) -> String = { it.toString() },
  onDismiss: () -> Unit
) {
  DropdownMenu(
    expanded = expanded,
    onDismissRequest = onDismiss
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      options.forEach { option ->
        key(key(option)) {
          DropdownMenuItem(onClick = { onSelect(option) }) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                textAlign = TextAlign.Start,
                text = title(option),
                color = AppTheme.colors.textPrimary,
                style = AppTheme.typography.body1
              )
              if (option == selected) {
                Icon(
                  painter = painterResource(id = R.drawable.ic_checkmark_24),
                  tint = AppTheme.colors.iconPrimary,
                  contentDescription = "option icon"
                )
              }
            }
          }
        }
      }
    }
  }
}
