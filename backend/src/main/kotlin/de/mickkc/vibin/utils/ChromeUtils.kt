package de.mickkc.vibin.utils

import de.mickkc.vibin.config.EnvUtil
import de.mickkc.vibin.config.EnvUtil.CHROME_BINARY_PATH
import java.io.File

object ChromeUtils {

    private val chromiumDriverFile = EnvUtil.getOrNull(CHROME_BINARY_PATH)
        ?.takeIf { it.isNotEmpty() }
        ?.let { File(it) }

    val isAvailable
        get() = chromiumDriverFile?.let { it.exists() && it.isFile && it.canExecute() } ?: false
}