@file:OptIn(ExperimentalUuidApi::class)

package ru.kode.epub.core.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * A reference handle representing a task registered with the [Scheduler].
 * Can be used to start, cancel, check status of the task.
 */
@Suppress("UseDataClass")
class TaskHandle<A, R>(
  val name: String,
  val id: Uuid,
  internal val scope: CoroutineScope,
  internal val body: suspend (A) -> R
) {
  companion object {
    val NIL = TaskHandle<Any?, Any?>("", Uuid.NIL, CoroutineScope(EmptyCoroutineContext)) {}
  }

  override fun toString(): String {
    return if (id == Uuid.NIL) "TaskHandle.NIL" else "TaskHandle[name=$name,id=$id]"
  }
}

@Suppress("UseDataClass")
class JobHandle(
  val taskHandle: TaskHandle<Any?, Any?>,
  val id: Uuid,
  internal val job: Job
) {
  companion object {
    val NIL = JobHandle(TaskHandle.NIL, Uuid.NIL, Job())
  }

  override fun toString(): String {
    return if (id == Uuid.NIL) "JobHandle.NIL" else "JobHandle[id=$id]"
  }
}

enum class RunState {
  NotStarted,
  Running,
  FinishedSuccess,
  FinishedError
}

/**
 * A state of a job
 */
@OptIn(ExperimentalUuidApi::class)
data class JobState(
  val jobId: Uuid,
  val taskId: Uuid,
  val argument: Any?,
  val runState: RunState = RunState.NotStarted,
  val error: Throwable? = null,
  val result: Any? = null,
  val tag: Any? = null
) {

  override fun toString(): String {
    return if (jobId == Uuid.NIL) {
      "JobState.NIL"
    } else {
      "JobState[${runState.name}][jobId=$jobId,taskId=$taskId,argument=$argument,error=$error,result=$result,tag=$tag]"
    }
  }
}

/**
 * A listener which can be used to observe task's job state changes
 */
@OptIn(ExperimentalUuidApi::class)
interface TaskStateChangeListener {
  /**
   * Will be called whenever a job state changes
   */
  suspend fun onJobStateChanged(state: JobState)

  /**
   * Will be called if job is cancelled
   */
  fun onJobCancelled(taskId: Uuid, jobId: Uuid)
}

/**
 * Task scheduler is the main entry point of the task system.
 *
 * Register tasks with [registerTask] which returns a task handle.
 * This handle can be passed to [start] or [startLatest] to start the task's body.
 * An instance of started task's body is called a "job" and is represented by the [JobHandle] which
 * is returned by [start] or [startLatest].
 *
 * Tasks can also be started in a "blocking" manner by calling [startSuspended] which will execute
 * tasks body, suspend and return the result. Note that you still can observe this job's state with a
 * registered [TaskStateChangeListener].
 *
 * Tasks and Jobs can also be restarted with corresponding [restart] functions. They will reuse the arguments
 * passed to [start], see [restart] documentation for more information.
 */
class Scheduler(
  val scope: CoroutineScope,
  val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
  val onUncaughtException: (taskId: Uuid, taskName: String, error: Throwable) -> Unit = { name, id, e ->
    println("warning: uncaught exception from task \"$name[$id]\"")
    e.printStackTrace()
  }
) {
  private val argumentCache: MutableMap<Uuid, Any?> = ConcurrentHashMap()
  private val _taskJobState: MutableMap<Uuid, JobState> = ConcurrentHashMap()

  private val stateChangeListeners: MutableList<TaskStateChangeListener> = CopyOnWriteArrayList()

  private fun createExceptionHandler(taskId: Uuid, taskName: String): CoroutineExceptionHandler {
    return CoroutineExceptionHandler { _, e ->
      onUncaughtException(taskId, taskName, e)
    }
  }

  fun addTaskStateChangeListener(listener: TaskStateChangeListener) {
    stateChangeListeners.add(listener)
  }

  fun removeTaskStateChangeListener(listener: TaskStateChangeListener) {
    stateChangeListeners.remove(listener)
  }

  fun <A, R> registerTask(
    name: String,
    dispatcher: CoroutineDispatcher = defaultDispatcher,
    body: suspend (A) -> R
  ): TaskHandle<A, R> {
    val taskId = Uuid.random()
    val scope =
      CoroutineScope(SupervisorJob(scope.coroutineContext.job) + createExceptionHandler(taskId, name) + dispatcher)
    return TaskHandle(
      name = name,
      id = taskId,
      body = body,
      scope = scope
    )
  }

  fun <A> start(handle: TaskHandle<A, *>, argument: A, tag: Any? = null): JobHandle {
    return startInternal(handle, argument, tag, cancelPrevious = false)
  }

  fun <A> startLatest(handle: TaskHandle<A, *>, argument: A, tag: Any? = null): JobHandle {
    return startInternal(handle, argument, tag, cancelPrevious = true)
  }

  suspend fun <A, R> startSuspended(handle: TaskHandle<A, R>, argument: A, tag: Any? = null): JobState {
    val jobId = Uuid.random()
    argumentCache.put(jobId, argument as Any?)
    argumentCache.put(handle.id, argument as Any?)
    return executeWrappedBody(handle, jobId, argument, tag)
  }

  private fun <A> startInternal(handle: TaskHandle<A, *>, argument: A, tag: Any?, cancelPrevious: Boolean): JobHandle {
    val jobId = Uuid.random()
    argumentCache.put(jobId, argument as Any?)
    argumentCache.put(handle.id, argument as Any?)
    val previousJobs = handle.scope.coroutineContext.job.children.toList()
    val job = handle.scope.launch {
      if (cancelPrevious) {
        previousJobs.forEach {
          it.cancelAndJoin()
        }
      }
      executeWrappedBody(handle, jobId, argument, tag)
    }
    job.invokeOnCompletion { cause ->
      if (cause is CancellationException) {
        stateChangeListeners.forEach {
          it.onJobCancelled(handle.id, jobId)
        }
      }
    }
    return JobHandle(
      handle as TaskHandle<Any?, Any?>,
      jobId,
      job
    )
  }

  @Suppress("TooGenericExceptionCaught", "InstanceOfCheckForException")
  private suspend fun <A, R> executeWrappedBody(
    handle: TaskHandle<A, R>,
    jobId: Uuid,
    argument: A,
    tag: Any?
  ): JobState {
    val state = JobState(jobId = jobId, taskId = handle.id, argument = argument, tag = tag, runState = RunState.Running)
    _taskJobState[jobId] = state
    stateChangeListeners.forEach { it.onJobStateChanged(state) }
    try {
      val result = handle.body(argument)
      val state = JobState(
        jobId = jobId,
        taskId = handle.id,
        argument = argument,
        tag = tag,
        runState = RunState.FinishedSuccess,
        result = result
      )
      _taskJobState[jobId] = state
      stateChangeListeners.forEach { it.onJobStateChanged(state) }
      return state
    } catch (e: Throwable) {
      if (e is CancellationException) {
        throw e
      } else {
        val state = JobState(
          jobId = jobId,
          taskId = handle.id,
          argument = argument,
          tag = tag,
          runState = RunState.FinishedError,
          error = e
        )
        _taskJobState[jobId] = state
        stateChangeListeners.forEach { it.onJobStateChanged(state) }
        return state
      }
    }
  }

  fun <A, R> restart(handle: TaskHandle<A, R>): JobHandle {
    @Suppress("UNCHECKED_CAST")
    val arg = argumentCache[handle.id] as? A?
    return if (arg != null) {
      start(handle, arg)
    } else {
      JobHandle.NIL
    }
  }

  fun restart(handle: JobHandle): JobHandle {
    val arg = argumentCache[handle.id]
    return if (arg != null) {
      start(handle.taskHandle, arg)
    } else {
      JobHandle.NIL
    }
  }

  fun cancel(taskHandle: TaskHandle<*, *>) {
    taskHandle.scope.coroutineContext.cancelChildren()
  }

  fun cancel(jobHandle: JobHandle) {
    jobHandle.job.cancel()
  }

  suspend fun cancelAndJoin(taskHandle: TaskHandle<*, *>) {
    taskHandle.scope.coroutineContext.job.children.forEach {
      it.cancelAndJoin()
    }
  }

  suspend fun cancelAndJoin(jobHandle: JobHandle) {
    jobHandle.job.cancelAndJoin()
  }
}

@OptIn(ExperimentalUuidApi::class)
fun Scheduler.observeStateChanges(handle: TaskHandle<*, *>): Flow<JobState> {
  return callbackFlow {
    val listener = object : TaskStateChangeListener {
      override suspend fun onJobStateChanged(state: JobState) {
        if (state.taskId == handle.id) send(state)
      }

      override fun onJobCancelled(taskId: Uuid, jobId: Uuid) {
        if (handle.id == taskId) {
          cancel(CancellationException("Task taskId=$taskId, jobId=$jobId was cancelled"))
        }
      }
    }
    this@observeStateChanges.addTaskStateChangeListener(listener)
    awaitClose { this@observeStateChanges.removeTaskStateChangeListener(listener) }
  }
}

inline fun <reified R> Scheduler.successResults(handle: TaskHandle<*, R>): Flow<R> {
  return observeStateChanges(handle).mapNotNull { it.result as? R }
}

inline fun Scheduler.errors(handle: TaskHandle<*, *>): Flow<Throwable> {
  return observeStateChanges(handle).mapNotNull { it.error }
}
