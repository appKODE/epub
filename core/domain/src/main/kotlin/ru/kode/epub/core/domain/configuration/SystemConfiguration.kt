package ru.kode.epub.core.domain.configuration

import ru.kode.epub.core.domain.entity.ScreenOrientation

data class SystemConfiguration(
  val isNightModeEnabled: Boolean,
  val localeConfiguration: LocaleConfiguration,
  val screenOrientation: ScreenOrientation
)
