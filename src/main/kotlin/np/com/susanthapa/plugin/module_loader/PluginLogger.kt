package np.com.susanthapa.plugin.module_loader

import com.intellij.openapi.diagnostic.Logger
import org.apache.log4j.Level

class PluginLogger {

    enum class DebugLevel {
        DEBUG, PRODUCTION
    }

    private var level = DebugLevel.DEBUG
    private val logger = Logger.getInstance(PluginLogger::class.java)

    fun setLevel(level: DebugLevel) {
        this.level = level
        if (isDebug()) {
            logger.setLevel(Level.ALL)
        }
    }

    fun warn(message: String) {
        if (isDebug()) {
            println("Warn: $message")
        } else {
            logger.warn(message)
        }
    }

    fun info(message: String) {
        if (isDebug()) {
            println("Info: $message")
        } else {
            logger.info(message)
        }
    }

    fun debug(message: String) {
        if (isDebug()) {
            println("Debug: $message")
        } else {
            logger.debug(message)
        }
    }

    fun error(message: String) {
        if (isDebug()) {
            println("Error: $message")
        } else {
            logger.error(message)
        }
    }

    private fun isDebug(): Boolean {
        return level == DebugLevel.DEBUG
    }

}