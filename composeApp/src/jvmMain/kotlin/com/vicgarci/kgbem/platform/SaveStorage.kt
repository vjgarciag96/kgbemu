package com.vicgarci.kgbem.platform

import java.io.File
import java.util.logging.Logger

actual object SaveStorage {

    private val logger = Logger.getLogger("SaveStorage")

    /**
     * Base directory for save files.
     * Exposed as a var so tests can redirect to a temp directory.
     */
    var baseDir: String = System.getProperty("user.home") + "/.kgbemu/saves"

    private fun sanitise(romTitle: String): String =
        romTitle.replace("/", "")

    actual fun save(romTitle: String, data: ByteArray) {
        val safe = sanitise(romTitle)
        val dir = File(baseDir)
        dir.mkdirs()

        val savFile = File(dir, "$safe.sav")
        val tmpFile = File(dir, "$safe.sav.tmp")

        tmpFile.writeBytes(data)
        tmpFile.renameTo(savFile)
    }

    actual fun load(romTitle: String): ByteArray? {
        val safe = sanitise(romTitle)
        val savFile = File(baseDir, "$safe.sav")

        if (!savFile.exists()) return null

        val data = savFile.readBytes()
        if (data.isEmpty()) {
            logger.warning("Save file for '$safe' has zero length")
            return null
        }
        return data
    }
}
