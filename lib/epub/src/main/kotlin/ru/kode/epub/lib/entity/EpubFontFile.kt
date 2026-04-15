package ru.kode.epub.lib.entity

/** TTF/OTF-файл шрифта, извлечённый из epub. */
data class EpubFontFile(val name: String, val bytes: ByteArray) {
  override fun equals(other: Any?) =
    other is EpubFontFile && name == other.name && bytes.contentEquals(other.bytes)
  override fun hashCode() = 31 * name.hashCode() + bytes.contentHashCode()
}
