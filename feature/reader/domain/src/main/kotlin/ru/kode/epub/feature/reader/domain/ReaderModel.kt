@file:OptIn(ExperimentalUuidApi::class)

package ru.kode.epub.feature.reader.domain

import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import ru.kode.epub.core.domain.Scheduler
import ru.kode.epub.core.domain.di.FlowCoroutineScope
import ru.kode.epub.core.domain.di.SingleIn
import ru.kode.epub.core.domain.observeStateChanges
import ru.kode.epub.core.domain.successResults
import ru.kode.epub.feature.reader.domain.di.ReaderScope
import ru.kode.epub.feature.reader.domain.entity.ReaderSettings
import kotlin.uuid.ExperimentalUuidApi

@SingleIn(ReaderScope::class)
class ReaderModel @Inject constructor(
  @FlowCoroutineScope(ReaderScope::class) private val coroutineScope: CoroutineScope,
  private val repository: ReaderRepository
) {

  private val scheduler = Scheduler(scope = coroutineScope)

  private val readEpub = scheduler.registerTask("fetch", body = { uri: Uri ->
    val book = repository.store(uri)
    book
  })

  fun readEpub(uri: Uri) {
    scheduler.startLatest(readEpub, uri)
  }

  val books = repository.books
  val readerSettings = repository.settings

  fun apply(settings: ReaderSettings) {
    coroutineScope.launch { repository.update(settings) }
  }

  val readEpubState = scheduler.observeStateChanges(readEpub)
  val epubReads = scheduler.successResults(readEpub)
}
