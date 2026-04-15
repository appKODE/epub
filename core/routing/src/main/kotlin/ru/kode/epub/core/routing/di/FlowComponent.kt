package ru.kode.epub.core.routing.di

import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope
import ru.kode.epub.core.ui.screen.ViewModelProvider

@Stable
interface FlowComponent {
  fun viewModelProviders(): Set<ViewModelProvider>
  fun coroutineScope(): CoroutineScope
}
