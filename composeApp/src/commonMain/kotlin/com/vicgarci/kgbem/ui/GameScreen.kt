package com.vicgarci.kgbem.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vicgarci.kgbem.emulator.EmulatorController
import com.vicgarci.kgbem.emulator.EmulatorState
import com.vicgarci.kgbem.joypad.GameBoyButton
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

    var showMenu by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    if (showMenu) {
        ModalBottomSheet(
            onDismissRequest = {
                showMenu = false
                emulatorController.resume()
            },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                TextButton(
                    onClick = {
                        showMenu = false
                        emulatorController.resume()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Resume")
                }
                TextButton(
                    onClick = {
                        showMenu = false
                        emulatorController.reset()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Reset")
                }
                TextButton(
                    onClick = {
                        showMenu = false
                        emulatorController.unload()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Load New ROM")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = {
                        emulatorController.pause()
                        showMenu = true
                    }) {
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
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.Black),
        ) {
            // Game viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(GB_WIDTH.toFloat() / GB_HEIGHT.toFloat()),
                contentAlignment = Alignment.Center,
            ) {
                val currentPixels = pixels
                if (currentPixels != null) {
                    pixelBuffer.updatePixels(currentPixels)
                    val imageBitmap = pixelBuffer.toImageBitmap()
                    Canvas(
                        modifier = Modifier.fillMaxSize(),
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
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        // No frame data yet -- render blank screen.
                    }
                }

                // PAUSED overlay
                if (state is EmulatorState.Paused) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "PAUSED",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            // Touch overlay beneath the viewport
            TouchControlsOverlay(
                onButtonDown = emulatorController::buttonDown,
                onButtonUp = emulatorController::buttonUp,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.6f),
            )
        }
    }
}

/**
 * Semi-transparent touch controls overlay.
 *
 * Layout:
 * - Lower-left: D-pad cross (UP / DOWN / LEFT / RIGHT)
 * - Lower-right: A (top) and B (bottom) action buttons
 * - Centre-bottom: START and SELECT
 */
@Composable
private fun TouchControlsOverlay(
    onButtonDown: (GameBoyButton) -> Unit,
    onButtonUp: (GameBoyButton) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.padding(16.dp)) {
        // D-pad -- lower-left
        DPad(
            onButtonDown = onButtonDown,
            onButtonUp = onButtonUp,
            modifier = Modifier.align(Alignment.BottomStart),
        )

        // A / B buttons -- lower-right
        ActionButtons(
            onButtonDown = onButtonDown,
            onButtonUp = onButtonUp,
            modifier = Modifier.align(Alignment.BottomEnd),
        )

        // Start / Select -- centre-bottom
        StartSelectButtons(
            onButtonDown = onButtonDown,
            onButtonUp = onButtonUp,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun DPad(
    onButtonDown: (GameBoyButton) -> Unit,
    onButtonUp: (GameBoyButton) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 3x3 grid with directional buttons on the cross positions
    val buttonSize = 56.dp // exceeds 48dp minimum
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // UP
        GameButton(
            label = "\u25B2",
            button = GameBoyButton.UP,
            onButtonDown = onButtonDown,
            onButtonUp = onButtonUp,
            modifier = Modifier.size(buttonSize),
        )
        Row {
            // LEFT
            GameButton(
                label = "\u25C0",
                button = GameBoyButton.LEFT,
                onButtonDown = onButtonDown,
                onButtonUp = onButtonUp,
                modifier = Modifier.size(buttonSize),
            )
            Spacer(Modifier.size(buttonSize))
            // RIGHT
            GameButton(
                label = "\u25B6",
                button = GameBoyButton.RIGHT,
                onButtonDown = onButtonDown,
                onButtonUp = onButtonUp,
                modifier = Modifier.size(buttonSize),
            )
        }
        // DOWN
        GameButton(
            label = "\u25BC",
            button = GameBoyButton.DOWN,
            onButtonDown = onButtonDown,
            onButtonUp = onButtonUp,
            modifier = Modifier.size(buttonSize),
        )
    }
}

@Composable
private fun ActionButtons(
    onButtonDown: (GameBoyButton) -> Unit,
    onButtonUp: (GameBoyButton) -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonSize = 64.dp
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // A button (upper)
        GameButton(
            label = "A",
            button = GameBoyButton.A,
            onButtonDown = onButtonDown,
            onButtonUp = onButtonUp,
            modifier = Modifier
                .size(buttonSize)
                .clip(CircleShape),
        )
        Spacer(Modifier.height(12.dp))
        // B button (lower)
        GameButton(
            label = "B",
            button = GameBoyButton.B,
            onButtonDown = onButtonDown,
            onButtonUp = onButtonUp,
            modifier = Modifier
                .size(buttonSize)
                .clip(CircleShape),
        )
    }
}

@Composable
private fun StartSelectButtons(
    onButtonDown: (GameBoyButton) -> Unit,
    onButtonUp: (GameBoyButton) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        GameButton(
            label = "SELECT",
            button = GameBoyButton.SELECT,
            onButtonDown = onButtonDown,
            onButtonUp = onButtonUp,
            modifier = Modifier
                .height(36.dp)
                .width(72.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        GameButton(
            label = "START",
            button = GameBoyButton.START,
            onButtonDown = onButtonDown,
            onButtonUp = onButtonUp,
            modifier = Modifier
                .height(36.dp)
                .width(72.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
    }
}

/**
 * A single virtual button that fires [onButtonDown] on press and
 * [onButtonUp] on release (supports press-and-hold).
 */
@Composable
private fun GameButton(
    label: String,
    button: GameBoyButton,
    onButtonDown: (GameBoyButton) -> Unit,
    onButtonUp: (GameBoyButton) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(Color.DarkGray, shape = RoundedCornerShape(8.dp))
            .pointerInput(button) {
                detectTapGestures(
                    onPress = {
                        onButtonDown(button)
                        tryAwaitRelease()
                        onButtonUp(button)
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
