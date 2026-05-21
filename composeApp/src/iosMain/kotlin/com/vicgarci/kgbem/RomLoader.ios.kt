package com.vicgarci.kgbem

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
actual fun RomLoader(onRomLoaded: (UByteArray) -> Unit) {
    // iOS UIDocumentPickerViewController requires bridging from SwiftUI;
    // a full implementation belongs in the iOS entry point.
    Button(onClick = {}) {
        Text("Load ROM (iOS: drop file via sharing)")
    }
}
