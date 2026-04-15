package ru.kode.epub.core.domain.configuration

import androidx.compose.runtime.Immutable
import ru.kode.epub.core.domain.entity.AppLocale

@Immutable
data class LocaleConfiguration(
  val current: AppLocale,
  val available: List<AppLocale>
)
