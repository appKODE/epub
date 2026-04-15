package ru.kode.epub.core.data.configuration

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import me.tatarka.inject.annotations.Inject
import ru.kode.epub.core.domain.configuration.SystemConfiguration
import ru.kode.epub.core.domain.configuration.SystemConfigurationRepository
import ru.kode.epub.core.domain.di.AppScope
import ru.kode.epub.core.domain.di.SingleIn
import ru.kode.epub.core.domain.entity.ScreenOrientation
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class SystemConfigurationDataRepository @Inject constructor() : SystemConfigurationRepository {
  private val _systemConfiguration = MutableStateFlow<SystemConfiguration?>(null)

  override fun updateSystemConfiguration(transform: (SystemConfiguration?) -> SystemConfiguration) {
    _systemConfiguration.update(transform)
  }

  override val isDarkModeEnabled: Flow<Boolean> = _systemConfiguration
    .mapNotNull { it?.isNightModeEnabled }
    .distinctUntilChanged()

  override val screenOrientation: Flow<ScreenOrientation> = _systemConfiguration
    .mapNotNull { it?.screenOrientation }
    .distinctUntilChanged()
}
