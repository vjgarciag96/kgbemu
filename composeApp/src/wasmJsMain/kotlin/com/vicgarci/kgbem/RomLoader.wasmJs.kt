package com.vicgarci.kgbem

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

// Full WasmJS file-picker interop requires JsAny bridging.
// The JS target (jsMain) provides a working implementation;
// this stub keeps the WasmJS build green until the interop layer is ready.
@Composable
actual fun RomLoader(onRomLoaded: (UByteArray) -> Unit) {
    Button(onClick = {}) {
        Text("Load ROM")
    }
}
