package com.vicgarci.kgbem.platform

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile

actual object SaveStorage {

    private fun savesDir(): String {
        val docs = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true,
        ).first() as String
        return "$docs/saves"
    }

    /** Strip path separators to prevent directory traversal. */
    private fun sanitise(romTitle: String): String =
        romTitle.replace(Regex("[/\\\\]"), "").ifEmpty { "unknown" }

    /**
     * Persists [data] to `Documents/saves/<romTitle>.sav` using an atomic
     * write-to-temp + rename strategy to avoid partial writes on crash.
     */
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    actual fun save(romTitle: String, data: ByteArray) {
        val dir = savesDir()
        NSFileManager.defaultManager.createDirectoryAtPath(
            dir,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        val safe = sanitise(romTitle)
        val tmpPath = "$dir/$safe.sav.tmp"
        val finalPath = "$dir/$safe.sav"

        val nsData = data.toNSData()
        nsData.writeToFile(tmpPath, atomically = false)

        // Remove existing file before rename (POSIX rename overwrites, but
        // NSFileManager.moveItemAtPath does not).
        val fm = NSFileManager.defaultManager
        fm.removeItemAtPath(finalPath, error = null)
        fm.moveItemAtPath(tmpPath, toPath = finalPath, error = null)
    }

    /**
     * Loads save data for [romTitle].
     * Returns null when the file does not exist or has zero length (corrupted).
     */
    actual fun load(romTitle: String): ByteArray? {
        val safe = sanitise(romTitle)
        val path = "${savesDir()}/$safe.sav"
        val nsData = NSData.dataWithContentsOfFile(path) ?: return null
        if (nsData.length.toInt() == 0) return null
        return nsData.toByteArray()
    }
}

/**
 * Converts a Kotlin [ByteArray] to [NSData].
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData()
    return usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}
