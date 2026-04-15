package ru.kode.epub.core.domain.configuration

import me.tatarka.inject.annotations.Inject
import ru.kode.epub.core.domain.di.AppScope
import ru.kode.epub.core.domain.di.SingleIn

@SingleIn(AppScope::class)
class SystemConfigurationModel @Inject constructor(
  private val repository: SystemConfigurationRepository
) {
  fun processConfigurationChange(configuration: SystemConfiguration) {
    repository.updateSystemConfiguration { configuration }
  }

  val screenOrientation = repository.screenOrientation
  val isDarkTheme = repository.isDarkModeEnabled
}
