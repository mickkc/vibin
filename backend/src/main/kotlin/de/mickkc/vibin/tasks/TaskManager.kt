package de.mickkc.vibin.tasks

import de.mickkc.vibin.repos.TaskSettingsRepo
import de.mickkc.vibin.utils.DateTimeUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ConcurrentHashMap

object TaskManager {

    private val tasks = mutableMapOf<String, BaseTask>()
    private val results = ConcurrentHashMap<String, TaskResult>()
    private val progresses = ConcurrentHashMap<String, TaskProgress>()

    private val progressMessageResponseFlow = MutableSharedFlow<TaskAction>()
    val sharedFlow = progressMessageResponseFlow.asSharedFlow()


    fun registerTask(task: BaseTask) {

        val settings = TaskSettingsRepo.getById(task.id)
        if (settings != null) {
            task.enabled.set(settings.enabled)
        }

        tasks[task.id] = task

        if (task.enabled.get()) {
            if (task.runOnStartup) {
                task.nextRun = DateTimeUtils.now()
            } else {
                if (task.interval != null)
                    task.nextRun = DateTimeUtils.now() + task.interval!!.inWholeSeconds
            }
        }
    }

    fun cancelTask(taskId: String) {
        val task = tasks[taskId] ?: return
        task.interrupt()
    }

    suspend fun waitForTaskCompletion(taskId: String) {
        val task = tasks[taskId] ?: return
        while (task.isRunning()) {
            delay(100)
        }
    }

    fun getTasks(): Collection<BaseTask> {
        return tasks.values
    }

    fun getSchedulableTasks(): Collection<BaseTask> {
        return tasks.values.filter { it.interval != null }
    }

    /**
     * Returns the next scheduled run time (in epoch seconds) among all tasks, or null if no tasks are scheduled.
     * This also includes tasks that are currently disabled because they may be enabled while waiting.
     */
    fun getNextTaskRunTime(): Long? {
        return tasks.values
            .mapNotNull { it.nextRun }
            .filter { it >= DateTimeUtils.now() }
            .minOrNull()
    }

    fun getById(taskId: String): BaseTask? {
        return tasks[taskId]
    }

    fun setTaskEnabled(taskId: String, enabled: Boolean) {
        TaskSettingsRepo.setTaskEnabled(taskId, enabled)
        tasks[taskId]?.enabled?.set(enabled)
    }

    fun setTaskResult(taskId: String, result: TaskResult) {
        results[taskId] = result
    }

    fun getTaskResult(taskId: String): TaskResult? {
        return results[taskId]
    }

    suspend fun reportStart(taskId: String) {
        progressMessageResponseFlow.emit(TaskAction(taskId, action = "start"))
    }

    suspend fun reportCompletion(taskId: String, result: TaskResult) {
        progressMessageResponseFlow.emit(TaskAction(taskId, action = "complete", result = result))
    }

    suspend fun reportProgress(progress: TaskProgress) {
        progresses[progress.taskId] = progress
        progressMessageResponseFlow.emit(TaskAction(progress.taskId, action = "progress", progress = progress))
    }

    fun getRunningProgresses(): List<TaskProgress> {
        return tasks.values
            .filter { it.isRunning() }
            .map { progresses[it.id] ?: TaskProgress(it.id) }
    }
}