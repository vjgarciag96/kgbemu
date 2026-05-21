package com.vicgarci.kgbem

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun RomLoader(onRomLoaded: (UByteArray) -> Unit) {
    val scope = rememberCoroutineScope()
    Button(onClick = {
        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                val chooser = JFileChooser().apply {
                    dialogTitle = "Select Game Boy ROM"
                    fileFilter = FileNameExtensionFilter("Game Boy ROMs (*.gb, *.gbc)", "gb", "gbc", "bin")
                }
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    chooser.selectedFile.readBytes().toUByteArray()
                } else null
            }
            bytes?.let { onRomLoaded(it) }
        }
    }) {
        Text("Load ROM")
    }
}
