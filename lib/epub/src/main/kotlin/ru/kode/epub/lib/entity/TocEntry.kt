package ru.kode.epub.lib.entity

data class TocEntry(
  val title: String,
  val chapterIndex: Int,
  val anchorId: String?,
  val children: List<TocEntry> = emptyList()
)
