package com.vicgarci.kgbem.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vicgarci.kgbem.cartridge.RomError
import com.vicgarci.kgbem.emulator.EmulatorController
import com.vicgarci.kgbem.emulator.EmulatorState
import com.vicgarci.kgbem.platform.pickRomFile
import kotlinx.coroutines.launch

@Composable
fun LauncherScreen(
    emulatorController: EmulatorController,
    state: EmulatorState,
) {
    val scope = rememberCoroutineScope()

    val onPickRom: () -> Unit = {
        scope.launch {
            val bytes = pickRomFile()
            if (bytes != null) {
                emulatorController.loadRom(bytes)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (state) {
            is EmulatorState.Idle -> {
                Text(
                    text = "kgbemu",
                    style = MaterialTheme.typography.headlineLarge,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onPickRom) {
                    Text("Open ROM")
                }
            }

            is EmulatorState.Loading -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = {}, enabled = false) {
                    Text("Open ROM")
                }
            }

            is EmulatorState.Error -> {
                val message = when (state.error) {
                    is RomError.Truncated ->
                        "ROM file is incomplete or corrupted."
                    is RomError.InvalidHeader ->
                        "ROM header is invalid. The file may be corrupted."
                    is RomError.UnsupportedMapper ->
                        "This cartridge type is not supported yet."
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onPickRom) {
                    Text("Try Again")
                }
            }

            else -> {
                // Running / Paused states are handled by GameScreen
            }
        }
    }
}
