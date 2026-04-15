package ru.kode.epub.feature.reader.data.di

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import me.tatarka.inject.annotations.Provides
import ru.kode.epub.core.data.storage.timestampAdapter
import ru.kode.epub.core.domain.di.SingleIn
import ru.kode.epub.feature.reader.data.Book
import ru.kode.epub.feature.reader.data.BooksDatabase
import ru.kode.epub.feature.reader.domain.di.ReaderScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

@ContributesTo(ReaderScope::class)
interface ReaderDataModule {

  @Provides
  @SingleIn(ReaderScope::class)
  fun booksDatabase(context: Context): BooksDatabase {
    return BooksDatabase(
      driver = AndroidSqliteDriver(BooksDatabase.Schema, context, "books"),
      BookAdapter = Book.Adapter(
        updatedAtAdapter = timestampAdapter()
      )
    )
  }
}
