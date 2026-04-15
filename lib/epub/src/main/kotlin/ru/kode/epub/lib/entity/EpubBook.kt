package ru.kode.epub.lib.entity

data class EpubBook(
  val title: String,
  val author: String,
  val coverImage: ContentElement.EpubImage?,
  val categories: List<String>,
  val language: String,
  val description: String,
  val dateYear: Int?,
  val chapters: List<EpubChapter>,
  val toc: List<TocEntry>,
  val fontFiles: List<EpubFontFile> = emptyList()
)
