package ru.kode.epub.lib.entity

/**
 * Resolved CSS properties relevant for Compose rendering.
 * Null means "not specified by CSS" — the caller uses its own default.
 */
data class EpubStyle(
  val textAlign: EpubTextAlign? = null,
  /** Percentage of the base font size; e.g. 160 for "160%". */
  val fontSizePercent: Int? = null,
  val italic: Boolean? = null,
  val bold: Boolean? = null,
  /** First-line text indent in em units (1em ≈ 16sp). */
  val textIndentEm: Float? = null,
  val marginStart: CssLength? = null,
  val marginEnd: CssLength? = null,
  val marginTop: CssLength? = null,
  val marginBottom: CssLength? = null,
  /** Explicit content width (e.g. width: 60%). */
  val width: CssLength? = null,
  /** Packed ARGB color (same format as android.graphics.Color). */
  val color: Long? = null
) {
  /**
   * Returns a new style with [override]'s non-null fields taking precedence.
   * All fields are eligible for override.
   */
  fun mergeWith(override: EpubStyle) = EpubStyle(
    textAlign = override.textAlign ?: textAlign,
    fontSizePercent = override.fontSizePercent ?: fontSizePercent,
    italic = override.italic ?: italic,
    bold = override.bold ?: bold,
    textIndentEm = override.textIndentEm ?: textIndentEm,
    marginStart = override.marginStart ?: marginStart,
    marginEnd = override.marginEnd ?: marginEnd,
    marginTop = override.marginTop ?: marginTop,
    marginBottom = override.marginBottom ?: marginBottom,
    width = override.width ?: width,
    color = override.color ?: color
  )

  /**
   * Merges text-related properties from [child] (textAlign, font*, textIndent, color)
   * but keeps this style's layout properties (margins, width) unchanged.
   *
   * Use for blockquote children: the child's `margin: 0` should not clear the
   * blockquote container's `margin-left: 35%`.
   */
  fun mergeTextWith(child: EpubStyle) = copy(
    textAlign = child.textAlign ?: textAlign,
    fontSizePercent = child.fontSizePercent ?: fontSizePercent,
    italic = child.italic ?: italic,
    bold = child.bold ?: bold,
    textIndentEm = child.textIndentEm ?: textIndentEm,
    color = child.color ?: color
  )
}

/** A CSS length value — either relative (em) or percentage of the parent container. */
sealed class CssLength {
  /** Length in em units (1em ≈ 16sp / 16dp). */
  data class Em(val value: Float) : CssLength()

  /** Length as percentage of the parent container's width (e.g. 35f = 35%). */
  data class Percent(val value: Float) : CssLength()
}

enum class EpubTextAlign { Left, Right, Center, Justify }
