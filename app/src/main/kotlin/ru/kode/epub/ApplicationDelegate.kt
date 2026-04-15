package ru.kode.epub

import android.app.Application
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
    _appComponent.systemConfigurationModel.processConfigurationChange(resources.configuration.toSystemConfiguration())
  }

  private fun buildAppComponent() = AppComponent::class.create(contextDelegate = this)

  private fun initLogging() {
    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
      Timber.d("Timber initialized")
    }
  }
}
