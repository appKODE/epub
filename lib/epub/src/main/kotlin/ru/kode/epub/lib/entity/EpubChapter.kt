package ru.kode.epub.lib.entity

data class EpubChapter(
  val title: String,
  val inToc: Boolean,
  val elements: List<ContentElement>,
  val anchorIndex: Map<String, Int>
)
