package de.mickkc.vibin.tasks

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import de.mickkc.vibin.utils.DateTimeUtils

object TaskScheduler {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val logger: Logger = LoggerFactory.getLogger(TaskScheduler::class.java)
    private const val IDLE_INTERVAL_SECONDS = 10L

    fun start() {
        scope.launch {
            while (isActive) {
                val now = DateTimeUtils.now()
                TaskManager.getTasks().forEach { task ->
                    val nextRun = task.nextRun
                    if (task.enabled.get() && (nextRun == null || nextRun <= now)) {
                        try {
                            val result = task.run()
                            if (result != null)
                                TaskManager.setTaskResult(task.id, result)
                        }
                        catch (e: Exception) {
                            logger.error("Error running task ${task.id}: ${e.message}", e)
                            TaskManager.setTaskResult(task.id, TaskResult.failure(e.toString()))
                        }
                    }
                }

                val delay = TaskManager.getNextTaskRunTime()?.let { nextRunTime ->
                    val delaySeconds = nextRunTime - DateTimeUtils.now()
                    if (delaySeconds > 0) delaySeconds else IDLE_INTERVAL_SECONDS
                } ?: IDLE_INTERVAL_SECONDS

                logger.info("Next task run in $delay seconds")

                delay(delay * 1000L)
            }
        }
    }

    suspend fun forceRunTask(taskId: String): TaskResult? {
        val task = TaskManager.getById(taskId) ?: return null
        try {
            val result = task.run(setNext = false, force = true)
            if (result != null) {
                TaskManager.setTaskResult(task.id, result)
            }
            return result

        } catch (e: Exception) {
            logger.error("Error force running task ${task.id}: ${e.message}", e)

            val result = TaskResult.failure(e.toString())
            TaskManager.setTaskResult(task.id, result)
            return result
        }
    }

    fun stop() {
        scope.cancel()
    }
}