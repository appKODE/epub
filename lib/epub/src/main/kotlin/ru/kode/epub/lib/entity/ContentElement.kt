package ru.kode.epub.lib.entity

sealed class ContentElement {
  data class Heading(
    val text: StyledText,
    val level: Int,
    val styles: EpubStyle = EpubStyle()
  ) : ContentElement()

  data class Paragraph(
    val text: StyledText,
    val styles: EpubStyle = EpubStyle()
  ) : ContentElement()

  /** Content of a &lt;blockquote&gt; tag. */
  data class Quote(
    val text: StyledText,
    val styles: EpubStyle = EpubStyle()
  ) : ContentElement()

  data class EpubImage(
    val data: ByteArray,
    val alt: String,
    val styles: EpubStyle = EpubStyle()
  ) : ContentElement() {
    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is EpubImage) return false
      return data.contentEquals(other.data) && alt == other.alt
    }
    override fun hashCode(): Int = 31 * data.contentHashCode() + alt.hashCode()
  }
}
