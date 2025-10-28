package wtf.ndu.vibin.tasks

import wtf.ndu.vibin.repos.TaskSettingsRepo
import java.util.concurrent.ConcurrentHashMap

object TaskManager {

    private val tasks = mutableMapOf<String, BaseTask>()
    private val results = ConcurrentHashMap<String, TaskResult>()

    fun registerTask(task: BaseTask) {

        val settings = TaskSettingsRepo.getById(task.id)
        if (settings != null) {
            task.enabled.set(settings.enabled)
        }

        tasks[task.id] = task
    }

    fun getTasks(): Collection<BaseTask> {
        return tasks.values
    }

    /**
     * Returns the next scheduled run time (in epoch seconds) among all tasks, or null if no tasks are scheduled.
     * This also includes tasks that are currently disabled because they may be enabled while waiting.
     */
    fun getNextTaskRunTime(): Long? {
        return tasks.values
            .mapNotNull { it.nextRun }
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
}