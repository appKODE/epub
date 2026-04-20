package ru.kode.epub

import android.app.Application
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
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
        configuration = resources.configuration.toSystemConfiguration(resources.configuration.toScreenOrientation())
      )
  }

  private fun buildAppComponent() = AppComponent::class.create(contextDelegate = this)

  private fun initLogging() {
    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
      Timber.d("Timber initialized")
    } else {
      Timber.plant(
        object : Timber.Tree() {
          override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            val crashlytics = FirebaseCrashlytics.getInstance()
            if (priority >= Log.WARN) crashlytics.log("priority=$priority, tag=$tag, message=$message")
            t?.let(crashlytics::recordException)
          }
        }
      )
    }
  }
}
