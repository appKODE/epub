package ru.kode.epub.di

import android.app.Activity
import ru.kode.epub.core.domain.di.AppScope
import ru.kode.epub.core.domain.di.ForegroundScope
import ru.kode.epub.core.domain.di.SingleIn
import ru.kode.epub.core.ui.screen.event.ViewEventsHostMediator
import ru.kode.epub.feature.reader.routing.ReaderFlowComponent
import software.amazon.lastmile.kotlin.inject.anvil.ContributesSubcomponent

@SingleIn(ForegroundScope::class)
@ContributesSubcomponent(scope = ForegroundScope::class)
interface ForegroundComponent {
  @ContributesSubcomponent.Factory(AppScope::class)
  interface Factory {
    fun create(
      activity: Activity,
      viewEventsHostMediator: ViewEventsHostMediator
    ): ForegroundComponent
  }

  fun readerFactory(): ReaderFlowComponent.Factory
}
