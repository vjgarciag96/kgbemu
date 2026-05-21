package com.vicgarci.kgbem

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlinx.browser.document
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader

@Composable
actual fun RomLoader(onRomLoaded: (UByteArray) -> Unit) {
    Button(onClick = { triggerFilePicker(onRomLoaded) }) {
        Text("Load ROM")
    }
}

private fun triggerFilePicker(onRomLoaded: (UByteArray) -> Unit) {
    val input = document.createElement("input") as HTMLInputElement
    input.type = "file"
    input.accept = ".gb,.gbc,.bin"
    input.onchange = {
        val file = input.files?.item(0) ?: return@onchange null
        val reader = FileReader()
        reader.onload = { event ->
            val buffer = (event.target as? FileReader)?.result as? ArrayBuffer
            if (buffer != null) {
                val bytes = Int8Array(buffer).let { arr ->
                    UByteArray(arr.length) { i -> arr[i].toUByte() }
                }
                onRomLoaded(bytes)
            }
            null
        }
        reader.readAsArrayBuffer(file)
        null
    }
    input.click()
}
