package ru.kode.epub

import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.view.Display
import android.view.Surface
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import ru.kode.epub.core.domain.configuration.LocaleConfiguration
import ru.kode.epub.core.domain.configuration.SystemConfiguration
import ru.kode.epub.core.domain.entity.AppLocale
import ru.kode.epub.core.domain.entity.ScreenOrientation

fun Configuration.toSystemConfiguration(
  orientation: ScreenOrientation
): SystemConfiguration {
  return SystemConfiguration(
    isNightModeEnabled = uiMode and UI_MODE_NIGHT_MASK == UI_MODE_NIGHT_YES,
    localeConfiguration = buildLocalesConfiguration(),
    screenOrientation = orientation
  )
}

fun Display.getScreenRotation(): ScreenOrientation {
  return when (rotation) {
    Surface.ROTATION_90 -> ScreenOrientation.Landscape
    Surface.ROTATION_180 -> ScreenOrientation.Portrait
    Surface.ROTATION_270 -> ScreenOrientation.Landscape
    else -> ScreenOrientation.Portrait
  }
}

private fun Configuration.buildLocalesConfiguration() = LocaleConfiguration(
  current = mapLocale(),
  available = ConfigurationCompat.getLocales(this).toLocalesList()
)

private fun Configuration.mapLocale(): AppLocale {
  val locale = locales.get(0)
  return when (locale.country) {
    "ru" -> AppLocale.Ru
    "en" -> AppLocale.En
    else -> AppLocale.System
  }
}

private fun LocaleListCompat.toLocalesList(): List<AppLocale> =
  this.toLanguageTags().split(LOCALE_LIST_SEPARATOR)
    .map { locale ->
      when (locale) {
        "ru" -> AppLocale.Ru
        "en" -> AppLocale.En
        else -> AppLocale.System
      }
    }

private const val LOCALE_LIST_SEPARATOR = ","
