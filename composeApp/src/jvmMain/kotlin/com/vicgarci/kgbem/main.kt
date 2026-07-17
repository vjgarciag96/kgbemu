package com.vicgarci.kgbem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.vicgarci.kgbem.di.AppGraph
import com.vicgarci.kgbem.joypad.KeyboardInputSource
import dev.zacsweers.metro.createGraph

fun main() {
    val appGraph = createGraph<AppGraph>()
    val keyboardInput = KeyboardInputSource()
    appGraph.emulatorController.setInputOverride(keyboardInput)

    application {
        var showTooltip by remember { mutableStateOf(true) }

        Window(
            onCloseRequest = ::exitApplication,
            title = "Kgbem",
            onKeyEvent = { event ->
                if (showTooltip) {
                    showTooltip = false
                }
                when (event.type) {
                    KeyEventType.KeyDown -> keyboardInput.onKeyDown(event.key)
                    KeyEventType.KeyUp -> keyboardInput.onKeyUp(event.key)
                    else -> false
                }
            },
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                App(appGraph.emulatorController)

                AnimatedVisibility(
                    visible = showTooltip,
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.inverseSurface,
                            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        ),
                    ) {
                        Text(
                            text = "Controls: Arrows = D-pad, Z = A, X = B, Enter = Start, RShift = Select",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}
