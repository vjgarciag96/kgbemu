package com.vicgarci.kgbem

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vicgarci.kgbem.emulator.EmulatorController
import com.vicgarci.kgbem.emulator.EmulatorState
import com.vicgarci.kgbem.ui.GameScreen
import com.vicgarci.kgbem.ui.LauncherScreen

@Composable
fun App(emulatorController: EmulatorController) {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            val state by emulatorController.emulatorState.collectAsStateWithLifecycle()

            when (state) {
                is EmulatorState.Running,
                is EmulatorState.Paused,
                -> GameScreen(emulatorController, state)

                else -> LauncherScreen(emulatorController, state)
            }
        }
    }
}
