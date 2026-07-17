package com.vicgarci.kgbem.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vicgarci.kgbem.emulator.EmulatorController
import com.vicgarci.kgbem.emulator.EmulatorState
import com.vicgarci.kgbem.platform.PixelBuffer

private const val GB_WIDTH = 160
private const val GB_HEIGHT = 144

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    emulatorController: EmulatorController,
    state: EmulatorState,
) {
    val isRunning = state is EmulatorState.Running

    val pixelBuffer = remember { PixelBuffer(GB_WIDTH, GB_HEIGHT) }
    val pixels by emulatorController.frameState.collectAsStateWithLifecycle()

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
        val currentPixels = pixels
        if (currentPixels != null) {
            pixelBuffer.updatePixels(currentPixels)
            val imageBitmap = pixelBuffer.toImageBitmap()
            Canvas(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxWidth()
                    .aspectRatio(GB_WIDTH.toFloat() / GB_HEIGHT.toFloat())
                    .background(Color.Black),
            ) {
                drawImage(
                    image = imageBitmap,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(GB_WIDTH, GB_HEIGHT),
                    dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                    filterQuality = FilterQuality.None,
                )
            }
        } else {
            Canvas(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxWidth()
                    .aspectRatio(GB_WIDTH.toFloat() / GB_HEIGHT.toFloat())
                    .background(Color.Black),
            ) {
                // No frame data yet -- render blank screen.
            }
        }
    }
}
