package ru.kode.epub.lib

/**
 * A single CSS rule: one or more selectors sharing the same declaration block.
 * Selectors are already split by comma and trimmed.
 * Declaration values are lowercased and stripped of `!important`.
 */
data class CssRule(
  val selectors: List<String>,
  val declarations: Map<String, String>
)

/**
 * Minimal CSS parser: turns a CSS text string into a list of [CssRule]s.
 * Handles standard rule blocks; skips @-rules (e.g. @font-face, @media).
 */
object CssParser {

  fun parse(css: String): List<CssRule> {
    val rules = mutableListOf<CssRule>()
    // Strip comments
    val cleaned = css.replace(Regex("/\\*[\\s\\S]*?\\*/"), "")
    // Match selector { declarations }
    val blockRegex = Regex("([^{@][^{]*?)\\{([^}]*)\\}")
    for (match in blockRegex.findAll(cleaned)) {
      val selectorText = match.groupValues[1].trim()
      val declarationBlock = match.groupValues[2]
      val selectors = selectorText.split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
      val declarations = parseDeclarations(declarationBlock)
      if (selectors.isNotEmpty() && declarations.isNotEmpty()) {
        rules.add(CssRule(selectors, declarations))
      }
    }
    return rules
  }

  /**
   * Parses a CSS declaration block (the content between `{` and `}`)
   * or an inline `style` attribute value into a property → value map.
   * Values are lowercased; `!important` is stripped.
   */
  fun parseDeclarations(block: String): Map<String, String> {
    val result = mutableMapOf<String, String>()
    for (decl in block.split(";")) {
      val idx = decl.indexOf(':')
      if (idx < 0) continue
      val prop = decl.substring(0, idx).trim().lowercase()
      val value = decl.substring(idx + 1)
        .replace(Regex("!important", RegexOption.IGNORE_CASE), "")
        .trim()
        .lowercase()
      if (prop.isNotEmpty() && value.isNotEmpty()) {
        result[prop] = value
      }
    }
    return result
  }
}
