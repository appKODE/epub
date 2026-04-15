package ru.kode.epub.lib.entity

data class StyledText(
  val text: String,
  val spans: List<TextSpan> = emptyList()
)

data class TextSpan(
  val start: Int,
  val end: Int,
  val bold: Boolean = false,
  val italic: Boolean = false
)

fun buildStyledText(block: StyledTextBuilder.() -> Unit): StyledText =
  StyledTextBuilder().apply(block).build()

class StyledTextBuilder {
  private val sb = StringBuilder()
  private val spans = mutableListOf<TextSpan>()

  fun append(text: String) { sb.append(text) }
  fun append(char: Char) { sb.append(char) }

  fun withBold(block: StyledTextBuilder.() -> Unit) {
    val start = sb.length
    block()
    spans.add(TextSpan(start, sb.length, bold = true))
  }

  fun withItalic(block: StyledTextBuilder.() -> Unit) {
    val start = sb.length
    block()
    spans.add(TextSpan(start, sb.length, italic = true))
  }

  fun build() = StyledText(sb.toString(), spans.toList())
}
