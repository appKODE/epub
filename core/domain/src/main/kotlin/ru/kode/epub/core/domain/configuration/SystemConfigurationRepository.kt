package ru.kode.epub.core.domain.configuration

import kotlinx.coroutines.flow.Flow

interface SystemConfigurationRepository {
  fun updateSystemConfiguration(transform: (SystemConfiguration?) -> SystemConfiguration)
  val isDarkModeEnabled: Flow<Boolean>
}
