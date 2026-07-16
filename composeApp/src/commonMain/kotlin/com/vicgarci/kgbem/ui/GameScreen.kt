package com.vicgarci.kgbem.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.vicgarci.kgbem.emulator.EmulatorController
import com.vicgarci.kgbem.emulator.EmulatorState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    emulatorController: EmulatorController,
    state: EmulatorState,
) {
    val isRunning = state is EmulatorState.Running

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (isRunning) {
                                emulatorController.pause()
                            } else {
                                emulatorController.resume()
                            }
                        },
                    ) {
                        Icon(
                            imageVector = if (isRunning) {
                                Icons.Default.Pause
                            } else {
                                Icons.Default.PlayArrow
                            },
                            contentDescription = if (isRunning) "Pause" else "Play",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .aspectRatio(160f / 144f)
                .background(Color.Gray),
        )
    }
}
