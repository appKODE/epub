package ru.kode.epub.core.ui.compose

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import ru.kode.epub.core.domain.entity.TextRef

@Suppress("SpreadOperator")
// is not called on a performance-critical path
@Composable
@ReadOnlyComposable
fun resolveRef(source: TextRef): String = when (source) {
  is TextRef.Res -> {
    if (source.formatArgs.isEmpty()) {
      stringResource(source.id)
    } else {
      stringResource(source.id, *source.formatArgs.toTypedArray())
    }
  }

  is TextRef.QtyRes -> {
    if (source.formatArgs.isEmpty()) {
      pluralStringResource(source.id, source.quantity)
    } else {
      pluralStringResource(source.id, source.quantity, *source.formatArgs.toTypedArray())
    }
  }

  is TextRef.Str -> source.value.toString()
  is TextRef.Compound -> {
    buildString {
      source.refs.forEach {
        append(resolveRef(source = it))
      }
    }
  }
}

@Suppress("SpreadOperator")
// We need to parse the args array into vararg parameters
@Composable
@ReadOnlyComposable
fun annotatedStringResource(
  @StringRes id: Int,
  vararg formatArgs: Pair<Any, SpanStyle>
): AnnotatedString {
  val args = Array(formatArgs.size) { formatArgs[it].first }
  val resources = LocalResources.current
  val resultText = resources.getString(id, *args)
  val spanStyles = args.mapIndexed { index, arg ->
    val startIndex = resultText.indexOf(arg.toString())
    val endIndex = startIndex + arg.toString().length
    AnnotatedString.Range(
      item = formatArgs[index].second,
      start = startIndex,
      end = endIndex
    )
  }
  return AnnotatedString(
    text = resultText,
    spanStyles = spanStyles
  )
}
