package de.mickkc.vibin.tasks

import de.mickkc.vibin.dto.TaskDto
import de.mickkc.vibin.utils.DateTimeUtils
import io.ktor.util.collections.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration

abstract class BaseTask {

    abstract val id: String
    open val interval: Duration? = null
    open val runOnStartup: Boolean = false

    private val running = AtomicBoolean(false)
    private val interrupted = AtomicBoolean(false)
    private val runAfterTasks = ConcurrentSet<String>()

    var enabled: AtomicBoolean = AtomicBoolean(true)

    var lastRun: Long? = null
    var nextRun: Long? = null

    protected val logger: Logger = LoggerFactory.getLogger("Task: $id")

    abstract suspend fun runTask(): TaskResult

    suspend fun run(setNext: Boolean = true, force: Boolean = false): TaskResult? {
        if (!enabled.get() && !force) return null
        if (!running.compareAndSet(false, true)) {
            logger.warn("Task $id is already running, skipping this run.")
            return null
        }

        lastRun = DateTimeUtils.now()

        if (setNext)
            setNextRun()

        logger.info("Running task $id")

        val start = System.currentTimeMillis()
        val result = try {
            reportStart()
            runTask()
        }
        catch (e: Exception) {
            logger.error("Error running task $id: ${e.message}", e)
            TaskResult.failure(e.toString())
        }
        val end = System.currentTimeMillis()

        logger.info("Finished task $id. Duration: ${end - start} ms. Result: ${result.message}")

        reportCompletion(result)
        running.set(false)
        interrupted.set(false)

        runAfterTasks.forEach { runTaskId ->
            TaskManager.getById(runTaskId)?.let { task ->
                logger.info("Running dependent task $runTaskId after $id")
                task.run()
            }
        }

        return result
    }

    fun setNextRun() {
        if (interval == null) {
            nextRun = null
            return
        }
        do {
            nextRun = (nextRun ?: lastRun ?: DateTimeUtils.now()) + interval!!.inWholeSeconds
        }
        while (nextRun!! <= DateTimeUtils.now())
    }

    fun toDto(): TaskDto {
        return TaskDto(
            id = id,
            enabled = enabled.get(),
            lastRun = lastRun,
            nextRun = nextRun,
            interval = interval?.inWholeSeconds,
            lastResult = TaskManager.getTaskResult(id)
        )
    }

    suspend fun reportProgress(message: String) = TaskManager.reportProgress(TaskProgress(id, message = message))
    suspend fun reportProgress(progress: Int, total: Int) = TaskManager.reportProgress(TaskProgress(id, progress = if (total == 0) 0.0 else (progress.toDouble() / total.toDouble())))
    suspend fun reportProgress(message: String, progress: Int, total: Int) = TaskManager.reportProgress(TaskProgress(id, progress = if (total == 0) 0.0 else (progress.toDouble() / total.toDouble()), message = "$message ($progress / $total)"))

    suspend fun reportStart() = TaskManager.reportStart(id)
    suspend fun reportCompletion(result: TaskResult) = TaskManager.reportCompletion(id, result)

    fun isRunning(): Boolean = running.get()
    fun isInterrupted(): Boolean = interrupted.get()
    fun interrupt() = interrupted.set(true)

    fun runAfterCompletion(task: BaseTask) = runAfterTasks.add(task.id)
}