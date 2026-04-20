package ru.kode.epub.lib

import ru.kode.epub.lib.entity.AlignItems
import ru.kode.epub.lib.entity.Background
import ru.kode.epub.lib.entity.Border
import ru.kode.epub.lib.entity.BorderLineStyle
import ru.kode.epub.lib.entity.BorderSide
import ru.kode.epub.lib.entity.Display
import ru.kode.epub.lib.entity.FlexDirection
import ru.kode.epub.lib.entity.FontVariant
import ru.kode.epub.lib.entity.JustifyContent
import ru.kode.epub.lib.entity.Length
import ru.kode.epub.lib.entity.ListStyleType
import ru.kode.epub.lib.entity.PageBreak
import ru.kode.epub.lib.entity.Style
import ru.kode.epub.lib.entity.TextAlign
import ru.kode.epub.lib.entity.TextDecoration
import ru.kode.epub.lib.entity.VerticalAlign
import ru.kode.epub.lib.entity.WhiteSpace

// ─────────────────────────────────────────────────────────────────────────────
// Path helpers (used in CSS and HTML parsing)
// ─────────────────────────────────────────────────────────────────────────────

internal fun resolvePath(base: String, relative: String): String {
  val rel = relative.substringBefore("#").trim()
  if (rel.isEmpty()) return base
  if (rel.startsWith("/")) return rel.removePrefix("/")
  val baseDir = base.substringBeforeLast("/", "")
  val stack: MutableList<String> =
    if (baseDir.isEmpty()) mutableListOf() else baseDir.split("/").toMutableList()
  rel.split("/").forEach { segment ->
    when (segment) {
      ".." -> if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
      ".", "" -> {}
      else -> stack.add(segment)
    }
  }
  return stack.joinToString("/")
}

internal fun guessMimeType(path: String): String {
  val ext = path.substringAfterLast('.').lowercase()
  return when (ext) {
    "jpg", "jpeg" -> "image/jpeg"
    "png" -> "image/png"
    "gif" -> "image/gif"
    "svg" -> "image/svg+xml"
    "webp" -> "image/webp"
    "otf" -> "font/otf"
    "ttf" -> "font/ttf"
    "woff" -> "font/woff"
    "woff2" -> "font/woff2"
    else -> "application/octet-stream"
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// CSS rule model
// ─────────────────────────────────────────────────────────────────────────────

/** Selector + flat declarations map. */
internal data class CssRule(
  val selector: String,
  val declarations: Map<String, String>
)

// ─────────────────────────────────────────────────────────────────────────────
// CSS text → rules
// ─────────────────────────────────────────────────────────────────────────────

/** Parses stylesheet text into a list of [CssRule]s (ignores @-rules except @font-face). */
internal fun parseCssRules(css: String): List<CssRule> {
  val cleaned = css.replace(Regex("/\\*[\\s\\S]*?\\*/"), "")
  val rules = mutableListOf<CssRule>()
  val pattern = Regex("""([^{}]+)\{([^{}]*)\}""")
  for (match in pattern.findAll(cleaned)) {
    val selector = match.groupValues[1].trim()
    if (selector.startsWith("@") && !selector.startsWith("@font-face")) continue
    val decls = parseCssDeclarations(match.groupValues[2])
    if (decls.isNotEmpty()) rules.add(CssRule(selector, decls))
  }
  return rules
}

internal fun parseCssDeclarations(block: String): Map<String, String> {
  val result = mutableMapOf<String, String>()
  for (decl in block.split(";")) {
    val colon = decl.indexOf(':')
    if (colon < 0) continue
    val prop = decl.substring(0, colon).trim().lowercase()
    val value = decl.substring(colon + 1).trim()
    if (prop.isNotEmpty() && value.isNotEmpty()) result[prop] = value
  }
  return result
}

// ─────────────────────────────────────────────────────────────────────────────
// @font-face parsing
// ─────────────────────────────────────────────────────────────────────────────

internal data class FontFaceRule(
  val family: String,
  val srcPath: String,
  val mimeType: String,
  val weight: Int,
  val italic: Boolean
)

internal val FONT_FACE_REGEX = Regex("""@font-face\s*\{([^}]*)\}""", RegexOption.IGNORE_CASE)

internal fun parseFontFaces(css: String, cssPath: String): List<FontFaceRule> {
  val cleaned = css.replace(Regex("/\\*[\\s\\S]*?\\*/"), "")
  return FONT_FACE_REGEX.findAll(cleaned).mapNotNull { match ->
    val decls = parseCssDeclarations(match.groupValues[1])
    val family = decls["font-family"]?.trim('"', '\'') ?: return@mapNotNull null
    val srcRaw = decls["src"] ?: return@mapNotNull null
    val srcRel = extractUrlPath(srcRaw) ?: return@mapNotNull null
    val srcPath = resolvePath(cssPath, srcRel)
    val weight = when (val w = decls["font-weight"]?.trim()) {
      "bold", "bolder" -> 700; "normal", "lighter", null -> 400; else -> w.toIntOrNull() ?: 400
    }
    val italic = decls["font-style"]?.let { it == "italic" || it == "oblique" } ?: false
    FontFaceRule(family, srcPath, guessMimeType(srcPath), weight, italic)
  }.toList()
}

// ─────────────────────────────────────────────────────────────────────────────
// CSS cascade / selector resolution
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Resolves CSS declarations for [tag]/[classes]/[id] against [rules], then merges [inlineStyle].
 * Simple specificity ordering: tag → class → id → inline. No pseudo-class support.
 */
internal object CssResolver {
  fun resolveRaw(
    rules: List<CssRule>,
    tag: String,
    classes: Set<String>,
    ancestors: List<String>,
    id: String? = null,
    inlineStyle: String? = null
  ): Map<String, String> {
    val result = mutableMapOf<String, String>()
    for (rule in rules) {
      val selectors = rule.selector.split(",").map { it.trim() }
      if (selectors.any { matchesSelector(it, tag, classes, id, ancestors) }) {
        result.putAll(rule.declarations)
      }
    }
    if (!inlineStyle.isNullOrEmpty()) {
      result.putAll(parseCssDeclarations(inlineStyle))
    }
    return result
  }

  /** Checks if a simple CSS selector matches the element context. */
  @Suppress("CyclomaticComplexMethod")
  private fun matchesSelector(
    selector: String,
    tag: String,
    classes: Set<String>,
    id: String?,
    ancestors: List<String>
  ): Boolean {
    val sel = selector.trim().lowercase()
    if (sel.contains("::") || sel.contains(":hover") || sel.contains(":focus") ||
      sel.contains(":active") || sel.contains(":visited") || sel.contains(":before") ||
      sel.contains(":after")
    ) return false

    if (sel.contains(" ")) {
      val parts = sel.split(Regex("\\s+"))
      val last = parts.last()
      val parentParts = parts.dropLast(1)
      if (!matchesSingleSelector(last, tag, classes, id)) return false
      return parentParts.all { ps -> ancestors.any { anc -> anc == ps.trimStart('.', '#') || ps.startsWith(".") } }
    }

    return matchesSingleSelector(sel, tag, classes, id)
  }

  private fun matchesSingleSelector(sel: String, tag: String, classes: Set<String>, id: String?): Boolean {
    if (sel == "*") return true
    if (sel == tag) return true
    if (sel.startsWith("#") && sel.removePrefix("#") == id) return true
    if (sel.startsWith(".") && sel.removePrefix(".") in classes) return true
    if (sel.contains(".")) {
      val tagPart = sel.substringBefore(".")
      val classPart = sel.substringAfter(".")
      if ((tagPart.isEmpty() || tagPart == tag) && classPart in classes) return true
    }
    if (sel.contains("#")) {
      val tagPart = sel.substringBefore("#")
      val idPart = sel.substringAfter("#")
      if ((tagPart.isEmpty() || tagPart == tag) && idPart == id) return true
    }
    return false
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// CSS declarations → Style
// ─────────────────────────────────────────────────────────────────────────────

@Suppress("CyclomaticComplexMethod", "LongMethod")
internal fun toStyle(
  decls: Map<String, String>,
  chapterPath: String,
  loadImage: (String) -> ByteArray?
): Style? {
  if (decls.isEmpty()) return null

  var fontFamily: String? = null
  var fontSize: Length? = null
  var fontWeight: Int? = null
  var italic: Boolean? = null
  var fontVariant: FontVariant? = null
  var letterSpacing: Length? = null
  var color: Long? = null
  var textAlign: TextAlign? = null
  var textIndent: Length? = null
  var lineHeight: Length? = null
  var textDecoration: TextDecoration? = null
  var textTransformUppercase = false
  var hyphensNone = false
  var whiteSpace: WhiteSpace? = null
  var verticalAlign: VerticalAlign? = null
  var marginTop: Length? = null
  var marginBottom: Length? = null
  var marginStart: Length? = null
  var marginEnd: Length? = null
  var paddingTop: Length? = null
  var paddingBottom: Length? = null
  var paddingStart: Length? = null
  var paddingEnd: Length? = null
  var width: Length? = null
  var maxWidth: Length? = null
  var height: Length? = null
  var minHeight: Length? = null
  var display: Display? = null
  var flexDirection: FlexDirection? = null
  var alignItems: AlignItems? = null
  var justifyContent: JustifyContent? = null
  var listStyleType: ListStyleType? = null
  var borderRadius: Length? = null
  var opacity: Float? = null
  var pageBreakBefore: PageBreak? = null
  var pageBreakAfter: PageBreak? = null
  var pageBreakInside: PageBreak? = null
  var orphans: Int? = null
  var widows: Int? = null
  var bgColor: Long? = null
  var bgImagePath: String? = null
  var bgSize: Length? = null
  var bgPositionX: Length? = null
  var bgPositionY: Length? = null
  var bgRepeat: Boolean? = null
  var borderTop: BorderSide? = null
  var borderBottom: BorderSide? = null
  var borderStart: BorderSide? = null
  var borderEnd: BorderSide? = null

  for ((prop, value) in decls) {
    when (prop) {
      "font-family" -> fontFamily = value.trim('"', '\'').split(",").first().trim().trim('"', '\'')
      "font-size" -> fontSize = parseLength(value)
      "font-weight" -> fontWeight = when (value.trim()) {
        "bold", "bolder" -> 700; "normal", "lighter" -> 400; else -> value.trim().toIntOrNull()
      }

      "font-style" -> italic = when (value.trim()) {
        "italic", "oblique" -> true; "normal" -> false; else -> null
      }

      "font-variant" -> fontVariant = if (value.contains("small-caps")) FontVariant.SmallCaps else null
      "letter-spacing" -> letterSpacing = parseLength(value)
      "color" -> color = parseColor(value)
      "text-align" -> textAlign = when (value.trim()) {
        "left" -> TextAlign.Left; "right" -> TextAlign.Right
        "center" -> TextAlign.Center; "justify", "justify-all" -> TextAlign.Justify; else -> null
      }

      "text-indent" -> textIndent = parseLength(value) ?: parseEmFallback(value)
      "line-height" -> lineHeight = parseLength(value) ?: if (value.trim() == "normal") Length.Em(1.2f) else null
      "text-decoration", "text-decoration-line" -> textDecoration = when {
        value.contains("underline") -> TextDecoration.Underline
        value.contains("line-through") -> TextDecoration.LineThrough
        value.contains("overline") -> TextDecoration.Overline
        else -> null
      }

      "text-transform" -> textTransformUppercase = value.trim() == "uppercase"
      "hyphens", "-webkit-hyphens", "-moz-hyphens" -> hyphensNone = value.trim() == "none"
      "white-space" -> whiteSpace = when (value.trim()) {
        "pre-wrap" -> WhiteSpace.PreWrap; "nowrap" -> WhiteSpace.NoWrap
        "pre" -> WhiteSpace.Pre; "normal" -> WhiteSpace.Normal; else -> null
      }

      "vertical-align" -> verticalAlign = when (value.trim()) {
        "super" -> VerticalAlign.Super; "sub" -> VerticalAlign.Sub
        "top" -> VerticalAlign.Top; "bottom" -> VerticalAlign.Bottom
        "middle" -> VerticalAlign.Middle; else -> VerticalAlign.Baseline
      }

      "margin" -> parseMarginShorthand(value).let { (t, e, b, s) ->
        marginTop = t; marginEnd = e; marginBottom = b; marginStart = s
      }

      "margin-top" -> marginTop = parseLength(value)
      "margin-bottom" -> marginBottom = parseLength(value)
      "margin-left", "margin-inline-start" -> marginStart = parseLength(value)
      "margin-right", "margin-inline-end" -> marginEnd = parseLength(value)
      "padding" -> parseMarginShorthand(value).let { (t, e, b, s) ->
        paddingTop = t; paddingEnd = e; paddingBottom = b; paddingStart = s
      }

      "padding-top" -> paddingTop = parseLength(value)
      "padding-bottom" -> paddingBottom = parseLength(value)
      "padding-left", "padding-inline-start" -> paddingStart = parseLength(value)
      "padding-right", "padding-inline-end" -> paddingEnd = parseLength(value)
      "width" -> width = parseLength(value)
      "max-width" -> maxWidth = parseLength(value)
      "height" -> height = parseLength(value)
      "min-height" -> minHeight = parseLength(value)
      "display" -> display = when (value.trim()) {
        "flex", "inline-flex" -> Display.Flex
        "block" -> Display.Block
        "inline-block" -> Display.InlineBlock
        "none" -> Display.None
        else -> null
      }

      "flex-direction" -> flexDirection = when (value.trim()) {
        "row" -> FlexDirection.Row; "column" -> FlexDirection.Column
        "row-reverse" -> FlexDirection.RowReverse; "column-reverse" -> FlexDirection.ColumnReverse
        else -> null
      }

      "align-items" -> alignItems = when (value.trim()) {
        "center" -> AlignItems.Center; "flex-end", "end" -> AlignItems.End
        "baseline" -> AlignItems.Baseline; "stretch" -> AlignItems.Stretch
        else -> AlignItems.Start
      }

      "justify-content" -> justifyContent = when (value.trim()) {
        "center" -> JustifyContent.Center; "flex-end", "end" -> JustifyContent.End
        "space-between" -> JustifyContent.SpaceBetween; "space-around" -> JustifyContent.SpaceAround
        "space-evenly" -> JustifyContent.SpaceEvenly; else -> JustifyContent.Start
      }

      "list-style", "list-style-type" -> listStyleType = when (value.trim()) {
        "none" -> ListStyleType.None; "disc" -> ListStyleType.Disc
        "circle" -> ListStyleType.Circle; "square" -> ListStyleType.Square
        "decimal" -> ListStyleType.Decimal; "lower-alpha", "lower-latin" -> ListStyleType.LowerAlpha
        "upper-alpha", "upper-latin" -> ListStyleType.UpperAlpha
        "lower-roman" -> ListStyleType.LowerRoman; "upper-roman" -> ListStyleType.UpperRoman
        else -> null
      }

      "border-radius" -> borderRadius = parseLength(value)
      "opacity" -> opacity = value.trim().toFloatOrNull()
      "page-break-before", "break-before" -> pageBreakBefore = parsePageBreak(value)
      "page-break-after", "break-after" -> pageBreakAfter = parsePageBreak(value)
      "page-break-inside", "break-inside" -> pageBreakInside = parsePageBreak(value)
      "orphans" -> orphans = value.trim().toIntOrNull()?.coerceAtLeast(1)
      "widows" -> widows = value.trim().toIntOrNull()?.coerceAtLeast(1)
      "background-color" -> bgColor = parseColor(value)
      "background-image" -> bgImagePath = extractUrlPath(value)
      "background-size" -> bgSize = parseLength(value.split(Regex("\\s+")).first())
      "background-position" -> {
        val parts = value.split(Regex("\\s+"))
        bgPositionX = parseBackgroundPosition(parts.getOrElse(0) { "0%" }, horizontal = true)
        bgPositionY = parseBackgroundPosition(parts.getOrElse(1) { "0%" }, horizontal = false)
      }

      "background-repeat" -> bgRepeat = value.trim() != "no-repeat"
      "background" -> parseBackgroundShorthand(value).let { bg ->
        bg.color?.let { bgColor = it }
        bg.imagePath?.let { bgImagePath = it }
      }

      "border" -> parseBorderSide(value)?.let { side ->
        borderTop = side; borderBottom = side; borderStart = side; borderEnd = side
      }

      "border-top" -> borderTop = parseBorderSide(value)
      "border-bottom" -> borderBottom = parseBorderSide(value)
      "border-left" -> borderStart = parseBorderSide(value)
      "border-right" -> borderEnd = parseBorderSide(value)
      "border-color" -> {
        val c = parseColor(value) ?: continue
        fun withColor(s: BorderSide?) = s?.copy(color = c) ?: BorderSide(Length.Px(1f), BorderLineStyle.Solid, c)
        borderTop = withColor(borderTop); borderBottom = withColor(borderBottom)
        borderStart = withColor(borderStart); borderEnd = withColor(borderEnd)
      }

      "border-width" -> {
        val w = parseLength(value) ?: continue
        fun withWidth(s: BorderSide?) = s?.copy(width = w) ?: BorderSide(w, BorderLineStyle.Solid, null)
        borderTop = withWidth(borderTop); borderBottom = withWidth(borderBottom)
        borderStart = withWidth(borderStart); borderEnd = withWidth(borderEnd)
      }

      "border-style" -> {
        if (value.trim() == "none" || value.trim() == "hidden") {
          borderTop = null; borderBottom = null; borderStart = null; borderEnd = null
        } else {
          val bs = parseBorderLineStyle(value) ?: continue
          fun withStyle(s: BorderSide?) = s?.copy(style = bs) ?: BorderSide(Length.Px(1f), bs, null)
          borderTop = withStyle(borderTop); borderBottom = withStyle(borderBottom)
          borderStart = withStyle(borderStart); borderEnd = withStyle(borderEnd)
        }
      }
    }
  }

  val border = Border(borderTop, borderBottom, borderStart, borderEnd)
    .takeIf { it.top != null || it.bottom != null || it.start != null || it.end != null }

  val background: Background? = when {
    bgImagePath != null -> {
      val data = loadImage(bgImagePath) ?: loadImage(resolvePath(chapterPath, bgImagePath))
      Background.Image(
        data = data,
        mimeType = guessMimeType(bgImagePath),
        size = bgSize,
        positionX = bgPositionX ?: Length.Percent(0f),
        positionY = bgPositionY ?: Length.Percent(0f),
        repeat = bgRepeat ?: false
      )
    }

    bgColor != null -> Background.Color(bgColor)
    else -> null
  }

  val hasAny = fontFamily != null || fontSize != null || fontWeight != null || italic != null ||
    fontVariant != null || letterSpacing != null || color != null || textAlign != null ||
    textIndent != null || lineHeight != null || textDecoration != null || textTransformUppercase ||
    hyphensNone || whiteSpace != null || verticalAlign != null || marginTop != null || marginBottom != null ||
    marginStart != null || marginEnd != null || paddingTop != null || paddingBottom != null ||
    paddingStart != null || paddingEnd != null || width != null || maxWidth != null ||
    height != null || minHeight != null || display != null || flexDirection != null ||
    alignItems != null || justifyContent != null || listStyleType != null || background != null ||
    border != null || borderRadius != null || opacity != null || pageBreakBefore != null ||
    pageBreakAfter != null || pageBreakInside != null || orphans != null || widows != null
  if (!hasAny) return null

  return Style(
    fontFamily = fontFamily, fontSize = fontSize, fontWeight = fontWeight, italic = italic,
    fontVariant = fontVariant, letterSpacing = letterSpacing, color = color,
    textAlign = textAlign, textIndent = textIndent, lineHeight = lineHeight,
    textDecoration = textDecoration, textTransformUppercase = textTransformUppercase,
    hyphensNone = hyphensNone, whiteSpace = whiteSpace, verticalAlign = verticalAlign,
    marginTop = marginTop, marginBottom = marginBottom, marginStart = marginStart, marginEnd = marginEnd,
    paddingTop = paddingTop, paddingBottom = paddingBottom, paddingStart = paddingStart, paddingEnd = paddingEnd,
    width = width, maxWidth = maxWidth, height = height, minHeight = minHeight,
    background = background, border = border, borderRadius = borderRadius, opacity = opacity,
    display = display, flexDirection = flexDirection, alignItems = alignItems, justifyContent = justifyContent,
    listStyleType = listStyleType,
    pageBreakBefore = pageBreakBefore, pageBreakAfter = pageBreakAfter, pageBreakInside = pageBreakInside,
    orphans = orphans, widows = widows
  )
}

// ─────────────────────────────────────────────────────────────────────────────
// CSS value parsers
// ─────────────────────────────────────────────────────────────────────────────

internal fun parseLength(value: String): Length? {
  val v = value.trim()
  return when {
    v == "0" || v == "0px" || v == "0em" || v == "0%" || v == "0pt" || v == "0rem" -> Length.Em(0f)
    v.endsWith("rem") -> v.removeSuffix("rem").toFloatOrNull()?.let { Length.Rem(it) }
    v.endsWith("em") -> v.removeSuffix("em").toFloatOrNull()?.let { Length.Em(it) }
    v.endsWith("%") -> v.removeSuffix("%").toFloatOrNull()?.let { Length.Percent(it) }
    v.endsWith("px") -> v.removeSuffix("px").toFloatOrNull()?.let { Length.Px(it) }
    v.endsWith("pt") -> v.removeSuffix("pt").toFloatOrNull()?.let { Length.Pt(it) }
    v == "auto" -> Length.Auto
    else -> null
  }
}

/** Fallback: bare number treated as em (common in text-indent shorthand). */
internal fun parseEmFallback(value: String) = value.trim().toFloatOrNull()?.let { Length.Em(it) }

/**
 * Parses margin/padding shorthand into (top, end, bottom, start).
 * CSS order: top right bottom left → we map right→end, left→start.
 */
internal fun parseMarginShorthand(value: String): List<Length?> {
  val parts = value.trim().split(Regex("\\s+"))
  val parsed = parts.map { parseLength(it) }
  return when (parsed.size) {
    1 -> listOf(parsed[0], parsed[0], parsed[0], parsed[0])
    2 -> listOf(parsed[0], parsed[1], parsed[0], parsed[1])
    3 -> listOf(parsed[0], parsed[1], parsed[2], parsed[1])
    else -> listOf(parsed.getOrNull(0), parsed.getOrNull(1), parsed.getOrNull(2), parsed.getOrNull(3))
  }
}

internal fun parseColor(value: String?): Long? {
  if (value == null) return null
  val v = value.trim()
  return when {
    v == "transparent" -> 0x00000000L
    v == "white" -> 0xFFFFFFFFL
    v == "black" -> 0xFF000000L
    v == "red" -> 0xFFFF0000L
    v == "green" -> 0xFF008000L
    v == "blue" -> 0xFF0000FFL
    v.startsWith("#") -> {
      val hex = v.removePrefix("#")
      when (hex.length) {
        3 -> {
          val (r, g, b) = hex.toList(); "ff$r$r$g$g$b$b".toLongOrNull(16)
        }

        4 -> {
          val (a, r, g, b) = hex.toList(); "$a$a$r$r$g$g$b$b".toLongOrNull(16)
        }

        6 -> "ff$hex".toLongOrNull(16)
        8 -> hex.toLongOrNull(16)
        else -> null
      }
    }

    v.startsWith("rgb(") -> {
      val nums = v.removePrefix("rgb(").removeSuffix(")").split(",")
        .mapNotNull { it.trim().toIntOrNull() }
      if (nums.size >= 3) {
        (0xFF000000L or (nums[0].toLong() shl 16) or (nums[1].toLong() shl 8) or nums[2].toLong())
      } else null
    }

    v.startsWith("rgba(") -> {
      val nums = v.removePrefix("rgba(").removeSuffix(")").split(",")
        .mapNotNull { it.trim().toFloatOrNull() }
      if (nums.size >= 4) {
        val a = (nums[3] * 255).toInt().coerceIn(0, 255)
        (a.toLong() shl 24) or (nums[0].toInt().toLong() shl 16) or (nums[1].toInt().toLong() shl 8) or nums[2].toInt()
          .toLong()
      } else null
    }

    else -> null
  }
}

internal fun parsePageBreak(value: String): PageBreak? = when (value.trim()) {
  "always", "page", "left", "right" -> PageBreak.Always
  "avoid", "avoid-page" -> PageBreak.Avoid
  "auto" -> PageBreak.Auto
  else -> null
}

internal fun parseBorderSide(value: String): BorderSide? {
  if (value.trim() == "none" || value.trim() == "0") return null
  val parts = value.trim().split(Regex("\\s+"))
  var width: Length? = null
  var style: BorderLineStyle? = null
  var color: Long? = null
  for (part in parts) {
    when {
      parseBorderLineStyle(part) != null -> style = parseBorderLineStyle(part)
      parseColor(part) != null -> color = parseColor(part)
      else -> parseLength(part)?.let { width = it }
    }
  }
  if (width == null && style == null && color == null) return null
  return BorderSide(width ?: Length.Px(1f), style ?: BorderLineStyle.Solid, color)
}

internal fun parseBorderLineStyle(value: String): BorderLineStyle? = when (value.trim()) {
  "solid" -> BorderLineStyle.Solid; "dashed" -> BorderLineStyle.Dashed
  "dotted" -> BorderLineStyle.Dotted; "double" -> BorderLineStyle.Double
  else -> null
}

internal data class BgShorthand(val color: Long? = null, val imagePath: String? = null)

internal fun parseBackgroundShorthand(value: String): BgShorthand {
  val urlMatch = Regex("""url\(['"]?([^'")\s]+)['"]?\)""", RegexOption.IGNORE_CASE).find(value)
  val imagePath = urlMatch?.groupValues?.get(1)
  val withoutUrl = if (urlMatch != null) value.replace(urlMatch.value, "").trim() else value
  val color = withoutUrl.split(Regex("\\s+")).firstNotNullOfOrNull { parseColor(it) }
  return BgShorthand(color, imagePath)
}

internal fun parseBackgroundPosition(token: String, horizontal: Boolean): Length? = when (token.trim()) {
  "left" -> Length.Percent(0f); "center" -> Length.Percent(50f); "right" -> Length.Percent(100f)
  "top" -> if (!horizontal) Length.Percent(0f) else null
  "bottom" -> if (!horizontal) Length.Percent(100f) else null
  else -> parseLength(token)
}

internal fun extractUrlPath(value: String): String? =
  Regex("""url\(['"]?([^'")\s]+)['"]?\)""", RegexOption.IGNORE_CASE).find(value)
    ?.groupValues?.get(1)?.trim()
