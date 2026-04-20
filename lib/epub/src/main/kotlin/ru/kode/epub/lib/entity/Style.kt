package ru.kode.epub.lib.entity

// ─────────────────────────────────────────────────────────────────────────────
// Inline spans (used inside Node.Text)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * An inline formatting range over [Node.Text.raw].
 * [start] inclusive, [end] exclusive — matches AnnotatedString conventions.
 */
data class Span(
  val start: Int,
  val end: Int,
  val bold: Boolean = false,
  val italic: Boolean = false,
  val underline: Boolean = false,
  val strikethrough: Boolean = false,
  val superscript: Boolean = false,
  val subscript: Boolean = false,
  val smallCaps: Boolean = false,
  /** Overrides the text color for this run. ARGB packed long (same format as [Style.color]). */
  val color: Long? = null,
  /** Relative font-size override for this run (e.g. 0.75em for <small>). */
  val fontSize: Length? = null
)

// ─────────────────────────────────────────────────────────────────────────────
// Style — resolved CSS for a node (CSS cascade already applied by the parser)
// ─────────────────────────────────────────────────────────────────────────────

data class Style(
  // Typography
  val fontFamily: String? = null,
  val fontSize: Length? = null,
  val fontWeight: Int? = null,
  val italic: Boolean? = null,
  val fontVariant: FontVariant? = null,
  val letterSpacing: Length? = null,
  val color: Long? = null,
  // Text layout
  val textAlign: TextAlign? = null,
  val textIndent: Length? = null,
  val lineHeight: Length? = null,
  val textDecoration: TextDecoration? = null,
  val textTransformUppercase: Boolean = false,
  val hyphensNone: Boolean = false,
  val whiteSpace: WhiteSpace? = null,
  val verticalAlign: VerticalAlign? = null,
  // Box model
  val marginTop: Length? = null,
  val marginBottom: Length? = null,
  val marginStart: Length? = null,
  val marginEnd: Length? = null,
  val paddingTop: Length? = null,
  val paddingBottom: Length? = null,
  val paddingStart: Length? = null,
  val paddingEnd: Length? = null,
  val width: Length? = null,
  val maxWidth: Length? = null,
  val height: Length? = null,
  val minHeight: Length? = null,
  // Background & border
  val background: Background? = null,
  val border: Border? = null,
  val borderRadius: Length? = null,
  val opacity: Float? = null,
  // Flex layout
  val display: Display? = null,
  val flexDirection: FlexDirection? = null,
  val alignItems: AlignItems? = null,
  val justifyContent: JustifyContent? = null,
  // List
  val listStyleType: ListStyleType? = null,
  // Pagination hints (for the page calculator)
  val pageBreakBefore: PageBreak? = null,
  val pageBreakAfter: PageBreak? = null,
  val pageBreakInside: PageBreak? = null,
  /** Minimum lines to keep at bottom of page before a break (CSS `orphans`, default 2). */
  val orphans: Int? = null,
  /** Minimum lines to keep at top of page after a break (CSS `widows`, default 2). */
  val widows: Int? = null
)

// ─────────────────────────────────────────────────────────────────────────────
// Background
// ─────────────────────────────────────────────────────────────────────────────

sealed class Background {
  data class Color(val argb: Long) : Background()

  /**
   * A background image. [data] is raw image bytes (null if the image was not found in the ZIP).
   * Position/size fields map directly to CSS background-position and background-size.
   */
  data class Image(
    val data: ByteArray?,
    val mimeType: String,
    val size: Length? = null,
    val positionX: Length = Length.Percent(0f),
    val positionY: Length = Length.Percent(0f),
    val repeat: Boolean = false
  ) : Background() {
    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is Image) return false
      return mimeType == other.mimeType && size == other.size &&
        positionX == other.positionX && positionY == other.positionY &&
        repeat == other.repeat && data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
      var r = data.contentHashCode()
      r = 31 * r + mimeType.hashCode()
      r = 31 * r + (size?.hashCode() ?: 0)
      r = 31 * r + positionX.hashCode()
      r = 31 * r + positionY.hashCode()
      r = 31 * r + repeat.hashCode()
      return r
    }
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Border
// ─────────────────────────────────────────────────────────────────────────────

/** Per-element border. Individual sides are null when not set. */
data class Border(
  val top: BorderSide? = null,
  val bottom: BorderSide? = null,
  val start: BorderSide? = null,
  val end: BorderSide? = null
)

data class BorderSide(
  val width: Length,
  val style: BorderLineStyle,
  val color: Long? = null
)

enum class BorderLineStyle { Solid, Dashed, Dotted, Double }

// ─────────────────────────────────────────────────────────────────────────────
// Length
// ─────────────────────────────────────────────────────────────────────────────

sealed class Length {
  data class Em(val value: Float) : Length()
  data class Rem(val value: Float) : Length()
  data class Percent(val value: Float) : Length()
  data class Px(val value: Float) : Length()
  data class Pt(val value: Float) : Length()

  /** CSS `auto` */
  object Auto : Length()
}

// ─────────────────────────────────────────────────────────────────────────────
// Enums
// ─────────────────────────────────────────────────────────────────────────────

enum class TextAlign { Left, Center, Right, Justify }

enum class TextDecoration { Underline, LineThrough, Overline }

enum class WhiteSpace {
  /** Collapse whitespace, wrap normally — CSS `normal` (default). */
  Normal,

  /** Preserve newlines and spaces, wrap on explicit breaks — CSS `pre-wrap`. */
  PreWrap,

  /** Collapse whitespace, no wrapping — CSS `nowrap`. */
  NoWrap,

  /** Preserve all whitespace, no wrapping — CSS `pre`. */
  Pre,
}

enum class VerticalAlign { Baseline, Super, Sub, Top, Bottom, Middle }

enum class FontVariant { SmallCaps }

enum class Display { Block, Flex, InlineBlock, None }

enum class FlexDirection { Row, Column, RowReverse, ColumnReverse }

enum class AlignItems { Start, Center, End, Baseline, Stretch }

enum class JustifyContent { Start, Center, End, SpaceBetween, SpaceAround, SpaceEvenly }

enum class ListStyleType { None, Disc, Circle, Square, Decimal, LowerAlpha, UpperAlpha, LowerRoman, UpperRoman }

enum class PageBreak {
  /** `always` / `page` — force break before/after. */
  Always,

  /** `avoid` — prevent break inside. */
  Avoid,

  /** `auto` — browser decides. */
  Auto,
}
