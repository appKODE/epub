package ru.kode.epub.core.routing

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.arkivanov.essenty.lifecycle.doOnCreate
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.essenty.lifecycle.doOnPause
import com.arkivanov.essenty.lifecycle.doOnStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import ru.kode.epub.core.domain.randomUuid

interface FlowComponentContext : ComponentContext {
  val context: ComponentContext
  fun configure(navigator: FlowNavigationComponent<*, *>, stack: Value<ChildStack<*, *>>)

  fun childFlow(context: ComponentContext): FlowComponentContext
}

@Stable
abstract class FlowNavigationComponent<Config : Any, Child : Node>(
  protected val context: FlowComponentContext
) : BackHandlerOwner by context {
  abstract val scope: CoroutineScope

  abstract fun initialConfig(): List<Config>

  protected val nav = StackNavigation<Config>()

  fun navigate(
    transform: StackNavigation<Config>.() -> Unit
  ) = runBlocking(Dispatchers.Main.immediate) {
    nav.transform()
  }

  protected abstract val childFactory: (Config, ComponentContext) -> Child

  val stack: Value<ChildStack<*, Child>> by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
    context.childStack(
      source = nav,
      serializer = null,
      initialStack = { initialConfig() },
      childFactory = { config: Config, context: ComponentContext ->
        val child = childFactory(config, context)
        if (child is Screen) {
          context.lifecycle.doOnStart { child.viewModel.onStart() }
          context.lifecycle.doOnPause { child.viewModel.detach() }
          context.lifecycle.doOnDestroy { child.viewModel.destroy() }
        }
        if (child is Flow) {
          context.lifecycle.doOnCreate { child.component.onCreate() }
          context.lifecycle.doOnDestroy { child.component.scope.cancel() }
        }
        child
      },
      key = randomUuid(),
      handleBackButton = true
    )
  }

  open fun onCreate() {
    context.configure(this, stack)
    context.lifecycle.doOnDestroy { scope.cancel() }
  }

  abstract fun onDismiss()
  open fun onScreenResult(result: Any?) = Unit

  open fun navigateBack() {
    runBlocking(Dispatchers.Main.immediate) {
      nav.pop { success -> if (!success) onDismiss() }
    }
  }

  open fun handleBack() {
    navigate { pop { success -> if (!success) onDismiss() } }
  }
}

/**
 * A class with dependencies needed to bind decompose navigation to a component lifecycle
 * and corresponding boilerplate code.
 * It is designed solely to keep the binding logic and dependencies in one place
 * instead of copying it in each [FlowComponentContext].
 */
class DefaultFlowComponentContext(
  override val context: ComponentContext
) : FlowComponentContext, ComponentContext by context {

  override fun configure(
    navigator: FlowNavigationComponent<*, *>,
    stack: Value<ChildStack<*, *>>
  ) {
    stack.screenResultFlow()
      .onEach { screenResult ->
        withContext(Dispatchers.Main.immediate) {
          navigator.onScreenResult(screenResult)
        }
      }
      .launchIn(navigator.scope)

    stack.navigateBackEvents()
      .onEach { navigator.navigateBack() }
      .launchIn(navigator.scope)
  }

  override fun childFlow(context: ComponentContext): FlowComponentContext {
    return DefaultFlowComponentContext(context = context)
  }
}
