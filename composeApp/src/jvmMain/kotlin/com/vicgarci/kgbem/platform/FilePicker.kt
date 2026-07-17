package com.vicgarci.kgbem.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter

actual suspend fun pickRomFile(): ByteArray? = withContext(Dispatchers.IO) {
    val dialog = FileDialog(null as Frame?, "Select ROM", FileDialog.LOAD).apply {
        filenameFilter = FilenameFilter { _, name -> name.endsWith(".gb", ignoreCase = true) }
        isVisible = true
    }
    val dir = dialog.directory
    val file = dialog.file
    if (dir != null && file != null) {
        File(dir, file).readBytes()
    } else {
        null
    }
}
