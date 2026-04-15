package ru.kode.epub.lib

import ru.kode.epub.lib.entity.CssLength
import ru.kode.epub.lib.entity.EpubStyle
import ru.kode.epub.lib.entity.EpubTextAlign

/**
 * Resolves an [EpubStyle] for a specific HTML element by cascading all matching CSS rules
 * and then applying inline styles on top (inline wins, as per CSS spec).
 *
 * Supported selectors:
 *  - Type:        `p`, `h1`, `blockquote`
 *  - Class:       `.b`, `.jright`
 *  - Compound:    `h1.body_head`, `p.b`
 *  - Multi-group: `body, div, p { … }`
 *  - Descendant:  `blockquote div`, `div img` (ancestor tags only, not classes)
 *
 * Properties handled: text-align, font-size (% only), font-style, font-weight,
 * text-indent (em/0), margin / margin-left / right / top / bottom (em/0), color (#hex).
 */
object CssResolver {

  /**
   * @param rules     Parsed CSS rules from all stylesheets in the EPUB chapter.
   * @param tag       Lowercase tag name of the element (e.g. "p", "h1").
   * @param classes   Class attribute tokens (e.g. setOf("b", "jright")).
   * @param ancestors Tag names from outermost ancestor down to the immediate parent.
   * @param inlineStyle Raw value of the element's `style` attribute, if any.
   */
  fun resolve(
    rules: List<CssRule>,
    tag: String,
    classes: Set<String>,
    ancestors: List<String> = emptyList(),
    inlineStyle: String? = null
  ): EpubStyle {
    val merged = mutableMapOf<String, String>()
    for (rule in rules) {
      if (rule.selectors.any { selectorMatches(it.trim(), tag, classes, ancestors) }) {
        merged.putAll(rule.declarations)
      }
    }
    if (!inlineStyle.isNullOrBlank()) {
      merged.putAll(CssParser.parseDeclarations(inlineStyle))
    }
    return toEpubStyle(merged)
  }

  // ─────────────────────────── Selector matching ─────────────────────────

  @Suppress("ReturnCount")
  private fun selectorMatches(
    selector: String,
    tag: String,
    classes: Set<String>,
    ancestors: List<String>
  ): Boolean {
    val parts = selector.split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (parts.isEmpty()) return false
    if (parts.size == 1) return simpleMatches(parts[0], tag, classes)

    // Last part must match this element
    if (!simpleMatches(parts.last(), tag, classes)) return false

    // Preceding parts must appear somewhere among ancestors (in order)
    var ai = ancestors.lastIndex
    for (i in parts.size - 2 downTo 0) {
      var found = false
      while (ai >= 0) {
        if (simpleMatches(parts[i], ancestors[ai], emptySet())) {
          found = true; ai--; break
        }
        ai--
      }
      if (!found) return false
    }
    return true
  }

  /**
   * Matches a simple selector (no whitespace) against a single element.
   * Examples: "p", ".b", "h1.body_head", "*"
   */
  private fun simpleMatches(selector: String, tag: String, classes: Set<String>): Boolean {
    if (selector == "*") return true
    val dotParts = selector.split(".")
    val tagPart = dotParts[0] // empty string if selector starts with "."
    val classParts = dotParts.drop(1) // class names without the dot
    if (tagPart.isNotEmpty() && tagPart != tag) return false
    return classParts.all { it.isEmpty() || it in classes }
  }

  // ──────────────────────── Declaration → EpubStyle ──────────────────────

  @Suppress("NestedBlockDepth", "CyclomaticComplexMethod")
  private fun toEpubStyle(decls: Map<String, String>): EpubStyle {
    var textAlign: EpubTextAlign? = null
    var fontSizePercent: Int? = null
    var italic: Boolean? = null
    var bold: Boolean? = null
    var textIndentEm: Float? = null
    var marginStartEm: CssLength? = null
    var marginEndEm: CssLength? = null
    var marginTopEm: CssLength? = null
    var marginBottomEm: CssLength? = null
    var widthLen: CssLength? = null
    var color: Long? = null

    for ((prop, value) in decls) {
      when (prop) {
        "text-align" -> textAlign = when (value) {
          "left" -> EpubTextAlign.Left
          "right" -> EpubTextAlign.Right
          "center" -> EpubTextAlign.Center
          "justify" -> EpubTextAlign.Justify
          else -> null
        }
        "font-size" -> fontSizePercent = when {
          value.endsWith("%") -> value.removeSuffix("%").trim().toIntOrNull()
          else -> null
        }
        "font-style" -> italic = when (value) {
          "italic", "oblique" -> true
          "normal" -> false
          else -> null
        }
        "font-weight" -> bold = when (value) {
          "bold", "bolder" -> true
          "normal", "lighter" -> false
          else -> value.toIntOrNull()?.let { it >= 600 }
        }
        "text-indent" -> textIndentEm = parseEm(value)
        "margin" -> parseLength(value)?.let { len ->
          marginStartEm = len
          marginEndEm = len
          marginTopEm = len
          marginBottomEm = len
        }
        "margin-left" -> marginStartEm = parseLength(value)
        "margin-right" -> marginEndEm = parseLength(value)
        "margin-top" -> marginTopEm = parseLength(value)
        "margin-bottom" -> marginBottomEm = parseLength(value)
        "width" -> widthLen = parseLength(value)
        "color" -> color = parseColor(value)
      }
    }

    return EpubStyle(
      textAlign, fontSizePercent, italic, bold,
      textIndentEm, marginStartEm, marginEndEm, marginTopEm, marginBottomEm,
      widthLen, color
    )
  }

  // ─────────────────────────── Value parsers ─────────────────────────────

  /** Parses `0`, em, or % lengths. Returns null for px/rem/other. */
  private fun parseLength(value: String): CssLength? = when {
    value == "0" || value == "0px" || value == "0em" || value == "0%" ->
      CssLength.Em(0f)
    value.endsWith("em") ->
      value.removeSuffix("em").trim().toFloatOrNull()?.let { CssLength.Em(it) }
    value.endsWith("%") ->
      value.removeSuffix("%").trim().toFloatOrNull()?.let { CssLength.Percent(it) }
    else -> null
  }

  /** Parses em-only lengths (for text-indent, which is rarely percentage in EPUB). */
  private fun parseEm(value: String): Float? = when {
    value == "0" || value == "0px" || value == "0em" -> 0f
    value.endsWith("em") -> value.removeSuffix("em").trim().toFloatOrNull()
    else -> null
  }

  private fun parseColor(value: String): Long? {
    if (!value.startsWith("#")) return null
    val hex = value.removePrefix("#")
    return when (hex.length) {
      3 -> {
        val (r, g, b) = hex.toList()
        "ff$r$r$g$g$b$b".toLongOrNull(16)
      }
      6 -> "ff$hex".toLongOrNull(16)
      8 -> hex.toLongOrNull(16)
      else -> null
    }
  }
}
