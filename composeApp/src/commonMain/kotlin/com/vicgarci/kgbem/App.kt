package com.vicgarci.kgbem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vicgarci.kgbem.emulator.GameBoyEmulator
import com.vicgarci.kgbem.ppu.PPU
import kotlinx.coroutines.isActive

@Composable
fun App() {
    val emulator = remember { GameBoyEmulator().also { it.loadTestPattern() } }
    var framebuffer by remember { mutableStateOf(IntArray(PPU.SCREEN_WIDTH * PPU.SCREEN_HEIGHT)) }

    LaunchedEffect(emulator) {
        while (isActive) {
            emulator.runUntilVBlank()
            framebuffer = emulator.ppu.framebuffer.copyOf()
        }
    }

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "KGBEmu – Game Boy Emulator",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            EmulatorScreen(
                pixels = framebuffer,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .aspectRatio(PPU.SCREEN_WIDTH.toFloat() / PPU.SCREEN_HEIGHT),
            )
            Text(
                text = "Load a ROM to begin",
                color = Color.Gray,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
fun EmulatorScreen(pixels: IntArray, modifier: Modifier = Modifier) {
    if (pixels.size != PPU.SCREEN_WIDTH * PPU.SCREEN_HEIGHT) {
        Box(modifier = modifier.background(Color.Black))
        return
    }
    Canvas(modifier = modifier) {
        val pixelW = size.width / PPU.SCREEN_WIDTH
        val pixelH = size.height / PPU.SCREEN_HEIGHT
        for (y in 0 until PPU.SCREEN_HEIGHT) {
            for (x in 0 until PPU.SCREEN_WIDTH) {
                drawRect(
                    color = Color(pixels[y * PPU.SCREEN_WIDTH + x]),
                    topLeft = Offset(x * pixelW, y * pixelH),
                    size = Size(pixelW, pixelH),
                )
            }
        }
    }
}

/**
 * Write a simple checkerboard test pattern directly into VRAM so the PPU has
 * something visible to render before a real ROM is loaded.
 */
private fun GameBoyEmulator.loadTestPattern() {
    val mem = memoryBus.memory

    // Tile 0: solid light (all pixels = color 0)
    // Tile 1: solid dark  (all pixels = color 3, lo=0xFF hi=0xFF)
    for (row in 0 until 8) {
        mem[0x8000 + row * 2] = 0x00.toUByte()
        mem[0x8001 + row * 2] = 0x00.toUByte()
        mem[0x8010 + row * 2] = 0xFF.toUByte()
        mem[0x8011 + row * 2] = 0xFF.toUByte()
    }

    // Fill the background tile map (0x9800–0x9BFF) with alternating tiles 0/1
    for (tileRow in 0 until 32) {
        for (tileCol in 0 until 32) {
            val index = tileRow * 32 + tileCol
            mem[0x9800 + index] = (if ((tileRow + tileCol) % 2 == 0) 0 else 1).toUByte()
        }
    }

    // BGP: color 0 → shade 0 (lightest), color 3 → shade 3 (darkest)
    mem[0xFF47] = 0xE4.toUByte()
    // LCDC: LCD on (bit 7), BG tile data = 0x8000 (bit 4), BG enable (bit 0)
    mem[0xFF40] = 0x91.toUByte()
}
