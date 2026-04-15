package ru.kode.epub.feature.reader.ui.bottombar

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import ru.kode.epub.core.domain.mapDistinctChanges
import ru.kode.epub.core.domain.randomUuid

@Stable
class BottomBarController {
  private enum class Visibility { Visible, Hidden }

  @JvmInline
  value class StateId(internal val value: String)

  private val bottomBarState = MutableStateFlow(BottomBarState())
  private val visibilityState = MutableStateFlow(mapOf(randomStateUuid() to Visibility.Hidden))
  private val sectionClicks = MutableSharedFlow<BottomBarSection>(extraBufferCapacity = 3)

  fun setActiveSection(section: BottomBarSection) {
    bottomBarState.update {
      it.copy(selectedSection = section)
    }
  }

  fun clearActiveSection() {
    bottomBarState.update {
      it.copy(selectedSection = null)
    }
  }

  fun sectionClicks(): Flow<BottomBarSection> {
    return sectionClicks
  }

  // implementation detail, intentionally not exposing in class' interface
  internal fun reportSectionClick(section: BottomBarSection) {
    sectionClicks.tryEmit(section)
  }

  /**
   * Pushes new visibility state and returns an id of the state which was active **before** the push.
   *
   * You can use [popVisibilityState] to revert back to the previous state.
   * This is useful if you do a series of state changes and want to undo all of them
   */
  fun show(): StateId {
    val id = randomStateUuid()
    pushState(stateId = id, visibility = Visibility.Visible)
    return id
  }

  /**
   * Pushes new visibility state and returns an id of the state which was active **before** the push.
   *
   * You can use [popVisibilityState] to revert back to the previous state.
   * This is useful if you do a series of state changes and want to undo all of them
   */
  fun hide(): StateId {
    val id = randomStateUuid()
    pushState(stateId = id, visibility = Visibility.Hidden)
    return id
  }

  private fun pushState(stateId: StateId, visibility: Visibility) {
    visibilityState.update { state -> state.plus(stateId to visibility) }
  }

  /**
   * Tryis to restores to state with [stateId]. It is obtained from [show] or [hide] calls
   * If new state has been pushed on since this stateId, it will take precedence
   */
  fun popVisibilityState(stateId: StateId) {
    visibilityState.update { state ->
      state.minus(stateId)
    }
  }

  fun isVisible(): Flow<Boolean> = visibilityState.mapDistinctChanges { state ->
    state.values.count { it == Visibility.Visible } >= state.values.count { it == Visibility.Hidden }
  }

  fun state(): Flow<BottomBarState> = bottomBarState
}

private fun randomStateUuid(): BottomBarController.StateId = BottomBarController.StateId(randomUuid())
