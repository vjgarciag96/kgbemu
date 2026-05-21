package com.vicgarci.kgbem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "KGB Emu",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp),
            )
            Text(
                text = "Load a ROM to start",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun GameBoyScreen(
    frameBuffer: IntArray,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.aspectRatio(160f / 144f)) {
        val scaleX = size.width / 160f
        val scaleY = size.height / 144f

        for (y in 0 until 144) {
            var x = 0
            while (x < 160) {
                val color = frameBuffer[y * 160 + x]
                var runEnd = x + 1
                while (runEnd < 160 && frameBuffer[y * 160 + runEnd] == color) {
                    runEnd++
                }
                drawRect(
                    color = Color(color),
                    topLeft = Offset(x * scaleX, y * scaleY),
                    size = Size((runEnd - x) * scaleX, scaleY),
                )
                x = runEnd
            }
        }
    }
}
