package com.vicgarci.kgbem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vicgarci.kgbem.ppu.PPU
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    var gameBoy by remember { mutableStateOf<GameBoy?>(null) }
    var frameCount by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(gameBoy) {
        val gb = gameBoy ?: return@LaunchedEffect
        while (true) {
            gb.runFrame()
            frameCount++
            delay(16)
        }
    }

    MaterialTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val gb = gameBoy
            if (gb != null) {
                GameBoyScreen(
                    frameBuffer = gb.frameBuffer,
                    frameCount = frameCount,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(8.dp),
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "kgbemu — Game Boy Emulator",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Button(onClick = {
                        scope.launch {
                            errorMessage = null
                            try {
                                val bytes = withContext(Dispatchers.Default) { loadRomBytes() }
                                    ?: return@launch
                                val cartridge = Cartridge.fromBytes(bytes)
                                gameBoy = GameBoy(cartridge)
                            } catch (e: Exception) {
                                errorMessage = "Failed to load ROM: ${e.message}"
                            }
                        }
                    }) {
                        Text("Load ROM (.gb)")
                    }
                    errorMessage?.let { msg ->
                        Text(msg, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun GameBoyScreen(
    frameBuffer: IntArray,
    frameCount: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(PPU.SCREEN_WIDTH.toFloat() / PPU.SCREEN_HEIGHT)
            .background(Color.Black),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scaleX = size.width / PPU.SCREEN_WIDTH
            val scaleY = size.height / PPU.SCREEN_HEIGHT
            for (y in 0 until PPU.SCREEN_HEIGHT) {
                for (x in 0 until PPU.SCREEN_WIDTH) {
                    val argb = frameBuffer[y * PPU.SCREEN_WIDTH + x]
                    val r = ((argb shr 16) and 0xFF) / 255f
                    val g = ((argb shr 8) and 0xFF) / 255f
                    val b = (argb and 0xFF) / 255f
                    drawRect(
                        color = Color(r, g, b),
                        topLeft = Offset(x * scaleX, y * scaleY),
                        size = Size(scaleX, scaleY),
                    )
                }
            }
        }
    }
}
