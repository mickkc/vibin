package de.mickkc.vibin.tasks

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import de.mickkc.vibin.dto.TaskDto
import de.mickkc.vibin.utils.DateTimeUtils
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration

abstract class BaseTask {

    abstract val id: String
    abstract val interval: Duration
    open val runOnStartup: Boolean = false

    var enabled: AtomicBoolean = AtomicBoolean(true)

    var lastRun: Long? = null
    var nextRun: Long? = null

    protected val logger: Logger = LoggerFactory.getLogger("Task: $id")

    abstract suspend fun runTask(): TaskResult

    init {
        nextRun = DateTimeUtils.now() + interval.inWholeSeconds
    }

    suspend fun run(setNext: Boolean = true, force: Boolean = false): TaskResult? {
        if (!enabled.get() && !force) return null

        lastRun = DateTimeUtils.now()

        if (setNext)
            setNextRun()

        logger.info("Running task $id")

        val start = System.currentTimeMillis()
        val result = try {
            runTask()
        }
        catch (e: Exception) {
            logger.error("Error running task $id: ${e.message}", e)
            TaskResult.failure(e.toString())
        }
        val end = System.currentTimeMillis()

        logger.info("Finished task $id. Duration: ${end - start} ms. Result: ${result.message}")

        return result
    }

    fun setNextRun() {
        do {
            nextRun = (nextRun ?: lastRun ?: DateTimeUtils.now()) + interval.inWholeSeconds
        }
        while (nextRun!! <= DateTimeUtils.now())
    }

    fun toDto(): TaskDto {
        return TaskDto(
            id = id,
            enabled = enabled.get(),
            lastRun = lastRun,
            nextRun = nextRun!!,
            interval = interval.inWholeSeconds,
            lastResult = TaskManager.getTaskResult(id)
        )
    }
}