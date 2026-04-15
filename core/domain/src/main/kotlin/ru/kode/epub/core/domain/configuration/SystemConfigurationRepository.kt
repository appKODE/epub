package ru.kode.epub.core.domain.configuration

import kotlinx.coroutines.flow.Flow
import ru.kode.epub.core.domain.entity.ScreenOrientation

interface SystemConfigurationRepository {
  fun updateSystemConfiguration(transform: (SystemConfiguration?) -> SystemConfiguration)
  val isDarkModeEnabled: Flow<Boolean>
  val screenOrientation: Flow<ScreenOrientation>
}
