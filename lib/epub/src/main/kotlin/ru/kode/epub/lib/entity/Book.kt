package ru.kode.epub.lib.entity

import java.io.File

// ─────────────────────────────────────────────────────────────────────────────
// Top-level book model
// ─────────────────────────────────────────────────────────────────────────────

data class Book(
  val metadata: Metadata,
  val chapters: List<Chapter> = emptyList(),
  val toc: List<TocItem> = emptyList(),
  val fontFaces: List<FontFace> = emptyList()
)

/** One entry in the table of contents, possibly with nested children. */
data class TocItem(
  val title: String,
  /** Index into [Book.chapters] for navigation. */
  val chapterIndex: Int,
  val children: List<TocItem> = emptyList()
)

data class Metadata(
  val title: String,
  val author: String,
  val language: String,
  val coverImage: CoverImage? = null,
  val dateYear: Int? = null,
  val description: String = "",
  val categories: List<String> = emptyList()
)

/** Raw image bytes + MIME type (e.g. "image/jpeg"). */
class CoverImage(val data: ByteArray, val mimeType: String) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is CoverImage) return false
    return mimeType == other.mimeType && data.contentEquals(other.data)
  }

  override fun hashCode(): Int = 31 * mimeType.hashCode() + data.contentHashCode()
}

data class Chapter(
  val title: String,
  val nodes: List<Node>
)

/** A @font-face declaration with a reference to the cached font file on disk. */
data class FontFace(
  val family: String,
  val weight: Int = 400,
  val italic: Boolean = false,
  val file: File
)

// ─────────────────────────────────────────────────────────────────────────────
// Node tree — one sealed hierarchy covers all content types
// ─────────────────────────────────────────────────────────────────────────────

sealed class Node {

  /**
   * A generic HTML element (div, p, h1-h6, blockquote, section, figure, etc.).
   * Inline-only elements (strong, em, span, …) are NOT emitted as Element nodes;
   * they are folded into [Text.spans] by the parser.
   */
  data class Element(
    val tag: String,
    val id: String? = null,
    val epubType: String? = null,
    val style: Style? = null,
    val children: List<Node> = emptyList()
  ) : Node()

  /**
   * A run of text with optional inline formatting spans.
   * [raw] is the plain string (no encoding tricks).
   * [spans] carry character-range formatting; ranges are over [raw] indices.
   */
  data class Text(
    val raw: String,
    val spans: List<Span> = emptyList()
  ) : Node()

  /**
   * An <img> / <image> node.
   * [data] is raw image bytes; [mimeType] is e.g. "image/jpeg".
   * [alt] is the alt-text attribute.
   */
  data class Image(
    val data: ByteArray,
    val mimeType: String,
    val alt: String = "",
    val style: Style? = null
  ) : Node() {
    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is Image) return false
      return mimeType == other.mimeType && alt == other.alt &&
        style == other.style && data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
      var r = data.contentHashCode()
      r = 31 * r + mimeType.hashCode()
      r = 31 * r + alt.hashCode()
      r = 31 * r + (style?.hashCode() ?: 0)
      return r
    }
  }

  /**
   * An ordered or unordered list (<ol> / <ul>).
   * Each [items] entry carries its 1-based display index (meaningful for ordered lists)
   * and its own child nodes (which may themselves contain nested lists, images, etc.).
   */
  data class BulletList(
    val ordered: Boolean,
    val items: List<ListItem>,
    val style: Style? = null
  ) : Node()

  /**
   * An HTML table. Cells may span multiple columns/rows via [TableCell.colspan] /
   * [TableCell.rowspan]. Content is arbitrary nodes.
   */
  data class Table(
    val rows: List<TableRow>,
    val style: Style? = null
  ) : Node()

  /** A <hr> / thematic-break node. */
  data class HorizontalRule(val style: Style? = null) : Node()
}

/** One item inside [Node.BulletList]. */
data class ListItem(
  /** 1-based counter value for ordered lists; always 1 for unordered lists. */
  val index: Int,
  val children: List<Node>,
  val style: Style? = null
)

data class TableRow(val cells: List<TableCell>)

data class TableCell(
  val children: List<Node>,
  val header: Boolean = false,
  val colspan: Int = 1,
  val rowspan: Int = 1,
  val style: Style? = null
)
