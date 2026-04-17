package ru.kode.epub.lib.entity

sealed class EpubBackground {
  data class SolidColor(val argb: Long) : EpubBackground()

  class Image(
    val data: ByteArray,
    /** Width of the image (background-size first value). Null = auto. */
    val size: CssLength? = null,
    /** Horizontal origin (background-position-x). Default 0% = left. */
    val positionX: CssLength = CssLength.Percent(0f),
    /** Vertical origin (background-position-y). Default 0% = top. */
    val positionY: CssLength = CssLength.Percent(0f),
    val repeat: Boolean = false
  ) : EpubBackground() {
    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is Image) return false
      if (!data.contentEquals(other.data)) return false
      return size == other.size &&
        positionX == other.positionX &&
        positionY == other.positionY &&
        repeat == other.repeat
    }

    override fun hashCode(): Int {
      var result = data.contentHashCode()
      result = 31 * result + (size?.hashCode() ?: 0)
      result = 31 * result + positionX.hashCode()
      result = 31 * result + positionY.hashCode()
      result = 31 * result + repeat.hashCode()
      return result
    }
  }
}
