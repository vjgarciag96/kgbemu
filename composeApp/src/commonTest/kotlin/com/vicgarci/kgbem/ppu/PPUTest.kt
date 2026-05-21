package com.vicgarci.kgbem.ppu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PPUTest {

    private val ppu = PPU()

    @Test
    fun modeTransition_afterOamScan_entersDrawingMode() {
        // Initial mode is 2 (OAM Scan)
        // After 80 T-cycles, should enter mode 3 (Drawing)
        ppu.step(80)
        // The STAT register bits 1-0 should now be 3
        val statMode = ppu.stat.toInt() and 0x03
        assertEquals(3, statMode)
    }

    @Test
    fun modeTransition_afterDrawing_entersHBlank() {
        // Mode 2 -> 3 (80 cycles) -> 0 (172 more cycles)
        ppu.step(80)
        ppu.step(172)
        val statMode = ppu.stat.toInt() and 0x03
        assertEquals(0, statMode)
    }

    @Test
    fun modeTransition_afterHBlank_lyIncrements() {
        // Full scanline = 456 cycles
        ppu.step(456)
        // After one scanline, LY should be 1 and mode should be back to 2
        assertEquals(1.toUByte(), ppu.ly)
        val statMode = ppu.stat.toInt() and 0x03
        assertEquals(2, statMode)
    }

    @Test
    fun modeTransition_after144Scanlines_entersVBlank() {
        // Run 144 complete scanlines
        repeat(144) {
            ppu.step(456)
        }
        assertEquals(144.toUByte(), ppu.ly)
        val statMode = ppu.stat.toInt() and 0x03
        assertEquals(1, statMode)
        assertTrue(ppu.vblankIrq)
    }

    @Test
    fun vblank_after10Lines_returnsToScanline0() {
        // Run 144 scanlines to enter VBlank
        repeat(144) { ppu.step(456) }
        ppu.vblankIrq = false

        // Run 10 more VBlank scanlines (154 total)
        repeat(10) { ppu.step(456) }

        assertEquals(0.toUByte(), ppu.ly)
        val statMode = ppu.stat.toInt() and 0x03
        assertEquals(2, statMode)
    }

    @Test
    fun lycMatch_setsStatBit2() {
        ppu.lyc = 1u
        // Run one full scanline so LY becomes 1
        ppu.step(456)
        // bit 2 of STAT should be set
        assertTrue((ppu.stat.toInt() and 0x04) != 0)
    }

    @Test
    fun lycMismatch_clearsStatBit2() {
        ppu.lyc = 5u
        ppu.step(456)
        // LY is 1, LYC is 5, no match
        assertFalse((ppu.stat.toInt() and 0x04) != 0)
    }

    @Test
    fun lcdDisabled_doesNotAdvanceDots() {
        // Disable LCD (clear bit 7 of LCDC)
        ppu.lcdc = (ppu.lcdc.toInt() and 0x7F).toUByte()
        val initialLy = ppu.ly
        ppu.step(456)
        // LY should not have changed
        assertEquals(initialLy, ppu.ly)
    }

    @Test
    fun frameBuffer_defaultColor_isFirstPaletteEntry() {
        // Frame buffer should be initialized with the first palette color
        assertEquals(PPU.GB_PALETTE[0], ppu.frameBuffer[0])
        assertEquals(PPU.GB_PALETTE[0], ppu.frameBuffer[160 * 144 - 1])
    }

    @Test
    fun renderScanline_withBackground_writesPixels() {
        // Set up a simple tile in VRAM at tile index 0
        // Tile data: all pixels set to color 1 (both bit planes = 1 = color 3 actually, let's use simple pattern)
        // Tile 0, row 0: lo = 0xFF, hi = 0x00 -> color = (0 shl 1) | 1 = 1 for each pixel
        ppu.writeVram(0x8000u, 0xFFu)  // lo byte of tile 0, row 0
        ppu.writeVram(0x8001u, 0x00u)  // hi byte -> color index 1

        // Enable LCD, BG enabled, tile data = 0x8000 (unsigned, lcdc bit 4 = 1)
        ppu.lcdc = 0x91u  // bit 7=1 (LCD on), bit 4=1 (tile data 0x8000), bit 0=1 (BG on)

        // Set BG tile map at 0x9800 (lcdc bit 3 = 0)
        // Tile map index 0 = tile 0 (already set by default 0)
        // Set palette: color 1 maps to palette slot 1 (bits 3-2 = 01 = index 1)
        ppu.bgp = 0b00000100u  // color 0 -> slot 0, color 1 -> slot 1

        // Run mode 2 + mode 3 to trigger renderScanline
        ppu.step(80)   // OAM scan
        ppu.step(172)  // Drawing -> renders scanline 0

        // The first pixel of scanline 0 should be palette color 1 (from color index 1)
        val expectedColor = PPU.GB_PALETTE[1]
        assertEquals(expectedColor, ppu.frameBuffer[0])
    }
}
