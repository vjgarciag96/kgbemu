package com.vicgarci.kgbem.platform

import android.content.Context
import android.util.Log
import java.io.File

actual object SaveStorage {

    private const val TAG = "SaveStorage"
    private const val SAVES_DIR = "saves"

    var context: Context? = null

    actual fun save(romTitle: String, data: ByteArray) {
        val ctx = context ?: run {
            Log.e(TAG, "Context not set; cannot save")
            return
        }
        val sanitised = sanitise(romTitle)
        val savesDir = File(ctx.filesDir, SAVES_DIR).also { it.mkdirs() }
        val savFile = File(savesDir, "$sanitised.sav")
        val tmpFile = File(savesDir, "$sanitised.sav.tmp")

        tmpFile.writeBytes(data)
        if (!tmpFile.renameTo(savFile)) {
            Log.e(TAG, "Atomic rename failed for $sanitised; falling back to direct write")
            savFile.writeBytes(data)
            tmpFile.delete()
        }
    }

    actual fun load(romTitle: String): ByteArray? {
        val ctx = context ?: run {
            Log.e(TAG, "Context not set; cannot load")
            return null
        }
        val sanitised = sanitise(romTitle)
        val savFile = File(ctx.filesDir, "$SAVES_DIR/$sanitised.sav")

        if (!savFile.exists()) return null

        val bytes = savFile.readBytes()
        if (bytes.isEmpty()) {
            Log.w(TAG, "Save file for '$sanitised' is empty; returning null")
            return null
        }
        return bytes
    }

    /** Strip path separators and other unsafe characters from the rom title. */
    private fun sanitise(romTitle: String): String =
        romTitle.replace(Regex("[/\\\\]"), "")
            .ifEmpty { "unknown" }
}
