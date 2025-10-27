package wtf.ndu.vibin.tasks

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import wtf.ndu.vibin.utils.DateTimeUtils

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
                            task.run()
                        }
                        catch (e: Exception) {
                            logger.error("Error running task ${task.id}: ${e.message}", e)
                        }
                    }
                }

                val delay = TaskManager.getNextTaskRunTime()?.let { nextRunTime ->
                    val delaySeconds = nextRunTime - DateTimeUtils.now()
                    if (delaySeconds > 0) delaySeconds else 0
                } ?: IDLE_INTERVAL_SECONDS

                logger.info("Next task run in $delay seconds")

                delay(delay * 1000L)
            }
        }
    }

    fun forceRunTask(taskId: String): Boolean {
        val task = TaskManager.getById(taskId) ?: return false
        scope.launch {
            try {
                task.run(setNext = false)
            } catch (e: Exception) {
                logger.error("Error force running task ${task.id}: ${e.message}", e)
            }
        }
        return true
    }

    fun stop() {
        scope.cancel()
    }
}