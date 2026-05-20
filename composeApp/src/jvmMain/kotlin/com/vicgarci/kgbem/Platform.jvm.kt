package com.vicgarci.kgbem

import java.awt.FileDialog
import java.awt.Frame
import java.io.FilenameFilter

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun loadRomBytes(): ByteArray? {
    val dialog = FileDialog(null as Frame?, "Open Game Boy ROM", FileDialog.LOAD)
    dialog.filenameFilter = FilenameFilter { _, name ->
        name.endsWith(".gb", ignoreCase = true) || name.endsWith(".gbc", ignoreCase = true)
    }
    dialog.isVisible = true
    return dialog.files.firstOrNull()?.readBytes()
}