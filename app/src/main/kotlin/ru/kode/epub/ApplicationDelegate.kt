package ru.kode.epub

import android.app.Application
import ru.kode.epub.core.domain.entity.ScreenOrientation
import ru.kode.epub.di.AppComponent
import ru.kode.epub.di.AppComponentHolder
import ru.kode.epub.di.create
import timber.log.Timber

class ApplicationDelegate : Application(), AppComponentHolder {
  private lateinit var _appComponent: AppComponent

  override val appComponent: AppComponent get() = _appComponent

  override fun onCreate() {
    _appComponent = buildAppComponent()
    super.onCreate()
    initLogging()
    _appComponent.systemConfigurationModel
      .processConfigurationChange(
        configuration = resources.configuration.toSystemConfiguration(ScreenOrientation.Portrait)
      )
  }

  private fun buildAppComponent() = AppComponent::class.create(contextDelegate = this)

  private fun initLogging() {
    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
      Timber.d("Timber initialized")
    }
  }
}
