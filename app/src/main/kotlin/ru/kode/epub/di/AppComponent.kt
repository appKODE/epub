package ru.kode.epub.di

import android.content.Context
import me.tatarka.inject.annotations.Provides
import ru.kode.epub.core.domain.configuration.SystemConfigurationModel
import ru.kode.epub.core.domain.di.AppScope
import ru.kode.epub.core.domain.di.SingleIn
import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent

@SingleIn(AppScope::class)
@MergeComponent(AppScope::class)
abstract class AppComponent(
  @get:Provides val context: Context
) : CommonAppComponent {
  abstract fun foregroundComponentFactory(): ForegroundComponent.Factory
}

interface CommonAppComponent {
  val systemConfigurationModel: SystemConfigurationModel
}

interface AppComponentHolder {
  val appComponent: AppComponent
}
