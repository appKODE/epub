package ru.kode.epub.core.domain.configuration

data class SystemConfiguration(
  val isNightModeEnabled: Boolean,
  val localeConfiguration: LocaleConfiguration
)
