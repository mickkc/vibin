package de.mickkc.vibin.utils

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

object ProcessUtil {

    private val logger: Logger = LoggerFactory.getLogger(ProcessUtil::class.java)

    suspend fun execute(command: Array<String>, workingDir: File? = null): ProcessResult {
        val processBuilder = ProcessBuilder(*command)
        if (workingDir != null) {
            processBuilder.directory(workingDir)
        }

        processBuilder.redirectOutput(ProcessBuilder.Redirect.PIPE)
        processBuilder.redirectError(ProcessBuilder.Redirect.PIPE)

        logger.info("Running process: ${command.joinToString(" ")} in directory: ${workingDir?.absolutePath ?: "default"}")

        lateinit var stdOut: Deferred<String>
        lateinit var stdErr: Deferred<String>

        val exitCode = withContext(Dispatchers.IO) {
            val process = processBuilder.start()
            stdOut = async { process.inputStream.bufferedReader().use { it.readText() } }
            stdErr = async { process.errorStream.bufferedReader().use { it.readText() } }
            process.waitFor()
        }

        val output = stdOut.await()
        val error = stdErr.await()

        return ProcessResult(
            exitCode = exitCode,
            output = output,
            error = error
        )
    }
}


data class ProcessResult(
    val exitCode: Int,
    val output: String,
    val error: String
) {
    val isSuccess: Boolean
        get() = exitCode == 0

    val isError: Boolean
        get() = !isSuccess
}