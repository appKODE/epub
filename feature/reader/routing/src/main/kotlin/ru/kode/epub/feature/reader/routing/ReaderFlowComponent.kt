package ru.kode.epub.feature.reader.routing

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides
import ru.kode.epub.core.domain.di.FlowCoroutineScope
import ru.kode.epub.core.domain.di.ForegroundScope
import ru.kode.epub.core.domain.di.SingleIn
import ru.kode.epub.core.routing.di.FlowComponent
import ru.kode.epub.core.ui.screen.ScopedViewModel
import ru.kode.epub.core.ui.screen.ViewModelProvider
import ru.kode.epub.feature.reader.domain.ReaderModel
import ru.kode.epub.feature.reader.domain.di.ReaderScope
import ru.kode.epub.feature.reader.domain.entity.Book
import ru.kode.epub.feature.reader.ui.reader.ReaderViewModel
import ru.kode.epub.feature.reader.ui.recent.RecentBooksViewModel
import ru.kode.epub.feature.reader.ui.settings.SettingsViewModel
import software.amazon.lastmile.kotlin.inject.anvil.ContributesSubcomponent

@SingleIn(ReaderScope::class)
@ContributesSubcomponent(ReaderScope::class)
interface ReaderFlowComponent : FlowComponent {

  val model: ReaderModel

  @ScopedViewModel(ReaderScope::class)
  override fun viewModelProviders(): Set<ViewModelProvider>

  @Provides
  @IntoSet
  fun bindRecentBooksViewModel(
    factory: () -> RecentBooksViewModel
  ):
    @ScopedViewModel(ReaderScope::class)
    ViewModelProvider = ViewModelProvider(
    key = RecentBooksViewModel::class.qualifiedName.orEmpty(),
    factory = { args -> factory() }
  )

  @Provides
  @IntoSet
  fun bindSettingsViewModel(
    factory: () -> SettingsViewModel
  ):
    @ScopedViewModel(ReaderScope::class)
    ViewModelProvider = ViewModelProvider(
    key = SettingsViewModel::class.qualifiedName.orEmpty(),
    factory = { args -> factory() }
  )

  @Provides
  @IntoSet
  fun bindBookViewModel(
    factory: (Book) -> ReaderViewModel
  ):
    @ScopedViewModel(ReaderScope::class)
    ViewModelProvider = ViewModelProvider(
    key = ReaderViewModel::class.qualifiedName.orEmpty(),
    factory = { args -> factory(args[0] as Book) }
  )

  @FlowCoroutineScope(ReaderScope::class)
  override fun coroutineScope(): CoroutineScope

  @Provides
  @SingleIn(ReaderScope::class)
  @FlowCoroutineScope(ReaderScope::class)
  fun providesReaderScope(): CoroutineScope = CoroutineScope(Default + CoroutineName("ReaderScope"))

  @ContributesSubcomponent.Factory(ForegroundScope::class)
  interface Factory {
    fun create(): ReaderFlowComponent
  }
}
