package ru.kode.epub

import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import com.arkivanov.decompose.defaultComponentContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import ru.kode.epub.core.domain.mapDistinctChanges
import ru.kode.epub.core.routing.DefaultFlowComponentContext
import ru.kode.epub.core.ui.compose.LocalScreenOrientation
import ru.kode.epub.core.ui.screen.event.LocalViewEventsHostMediator
import ru.kode.epub.core.ui.screen.event.ViewEventsHostMediator
import ru.kode.epub.core.ui.screen.event.rememberViewEventsMediator
import ru.kode.epub.core.uikit.screen.ViewEventsHost
import ru.kode.epub.core.uikit.theme.AppTheme
import ru.kode.epub.di.AppComponent
import ru.kode.epub.di.AppComponentHolder
import ru.kode.epub.di.ForegroundComponent
import ru.kode.epub.feature.reader.domain.entity.NightMode
import ru.kode.epub.feature.reader.domain.entity.SelectorSettings
import ru.kode.epub.feature.reader.routing.ReaderFlow
import ru.kode.epub.feature.reader.routing.ReaderFlowParams
import ru.kode.epub.feature.reader.routing.ReaderNavigationComponent

@Stable
open class MainActivity : ComponentActivity() {
  protected val coroutineScope = CoroutineScope(Dispatchers.Default)
  protected lateinit var foregroundComponent: ForegroundComponent

  private val importUri = mutableStateOf<Uri?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    handleIntent(intent)
    configureEdgeToEdge()
    val appComponent = (applicationContext!! as AppComponentHolder).appComponent
    val context = defaultComponentContext(discardSavedState = true)
    appComponent.addBookLauncher.register(this)
    val initialOrientation = windowManager.defaultDisplay.getScreenRotation()

    setContent {
      val viewEventsMediator = rememberViewEventsMediator()
      val foregroundComponent = rememberForegroundComponent(appComponent, viewEventsMediator)
      val systemConfigModel = remember { appComponent.systemConfigurationModel }

      val initialIsDarkTheme = isSystemInDarkTheme()
      val isDark by isDarkTheme(appComponent).collectAsState(initialIsDarkTheme)

      val orientation by systemConfigModel.screenOrientation.collectAsState(initialOrientation)

      setSingletonImageLoaderFactory { context -> ImageLoader.Builder(context).crossfade(true).build() }

      AppTheme(useDarkTheme = isDark) {
        WindowBackgroundEffect(color = AppTheme.colors.surfaceBackground)
        SystemBarsColorEffect(isDark)
        CompositionLocalProvider(
          LocalScreenOrientation provides orientation,
          LocalViewEventsHostMediator provides viewEventsMediator
        ) {
          Box(modifier = Modifier.fillMaxSize()) {
            val uri = importUri.value
            ReaderFlow(
              component = remember(foregroundComponent, uri) {
                ReaderNavigationComponent(
                  params = uri?.let(ReaderFlowParams::Book) ?: ReaderFlowParams.Recent,
                  component = foregroundComponent.readerFactory().create(),
                  context = DefaultFlowComponentContext(context),
                  viewEventsHostMediator = viewEventsMediator,
                  onFinish = ::finish
                )
              }
            )
            ViewEventsHost(
              modifier = Modifier
                .fillMaxSize()
                .zIndex(2f),
              configurationsFlow = viewEventsMediator.events
            )
          }
        }
      }
    }
  }

  @Composable
  private fun SystemBarsColorEffect(isDarkTheme: Boolean) {
    val view = LocalView.current
    LaunchedEffect(isDarkTheme) {
      val windowInsetsController = window?.let {
        WindowCompat.getInsetsController(it, view)
      }
      windowInsetsController?.isAppearanceLightStatusBars = !isDarkTheme
      windowInsetsController?.isAppearanceLightNavigationBars = !isDarkTheme
    }
  }

  private fun isDarkTheme(appComponent: AppComponent): Flow<Boolean> {
    return combine(
      appComponent.systemConfigurationModel.isDarkTheme,
      appComponent.readerRepository.settings
        .mapDistinctChanges { settings ->
          settings.firstOrNull { it.key.value == SelectorSettings.AppThemeMode.key }
            ?.selected as NightMode
        }
    ) { isSystemDarkMode, config ->
      when (config) {
        NightMode.Day -> false
        NightMode.Night -> true
        NightMode.Auto -> isSystemDarkMode
      }
    }
  }

  @Composable
  internal fun rememberForegroundComponent(
    appComponent: AppComponent,
    viewEventsHostMediator: ViewEventsHostMediator
  ): ForegroundComponent {
    return remember {
      val component = appComponent.foregroundComponentFactory().create(
        activity = this,
        viewEventsHostMediator = viewEventsHostMediator
      )
      this@MainActivity.foregroundComponent = component
      component
    }
  }

  @Composable
  private fun WindowBackgroundEffect(color: Color) {
    LaunchedEffect(color) {
      window.setBackgroundDrawable(ColorDrawable(color.toArgb()))
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleIntent(intent)
  }

  override fun onDestroy() {
    super.onDestroy()
    coroutineScope.cancel()
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    processConfigurationChange(newConfig)
  }

  private fun processConfigurationChange(newConfig: Configuration) {
    (applicationContext!! as AppComponentHolder).appComponent
      .systemConfigurationModel
      .processConfigurationChange(
        configuration = newConfig.toSystemConfiguration(windowManager.defaultDisplay.getScreenRotation())
      )
  }

  private fun configureEdgeToEdge() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    enableEdgeToEdge()
    if (Build.VERSION.SDK_INT >= 28) {
      window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }
  }

  private fun handleIntent(intent: Intent) {
    intent.data?.let { uri -> importUri.value = uri }
  }
}
