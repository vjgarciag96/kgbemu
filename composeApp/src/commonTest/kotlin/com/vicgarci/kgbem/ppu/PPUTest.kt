package com.vicgarci.kgbem.ppu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PPUTest {

    private val ppu = PPU()

    /** Advance PPU by [cycles] T-cycles using small steps to trigger all transitions correctly. */
    private fun advanceCycles(cycles: Int) {
        var remaining = cycles
        while (remaining > 0) {
            val step = minOf(4, remaining)
            ppu.step(step)
            remaining -= step
        }
    }

    /** Advance PPU by exactly one full scanline (456 T-cycles). */
    private fun advanceScanline() = advanceCycles(456)

    @Test
    fun modeTransition_afterOamScan_entersDrawingMode() {
        // Initial mode is 2 (OAM Scan)
        // After 80 T-cycles, should enter mode 3 (Drawing)
        advanceCycles(80)
        // The STAT register bits 1-0 should now be 3
        val statMode = ppu.stat.toInt() and 0x03
        assertEquals(3, statMode)
    }

    @Test
    fun modeTransition_afterDrawing_entersHBlank() {
        // Mode 2 -> 3 (80 cycles) -> 0 (172 more cycles)
        advanceCycles(80 + 172)
        val statMode = ppu.stat.toInt() and 0x03
        assertEquals(0, statMode)
    }

    @Test
    fun modeTransition_afterHBlank_lyIncrements() {
        // Full scanline = 456 cycles
        advanceScanline()
        // After one scanline, LY should be 1 and mode should be back to 2
        assertEquals(1.toUByte(), ppu.ly)
        val statMode = ppu.stat.toInt() and 0x03
        assertEquals(2, statMode)
    }

    @Test
    fun modeTransition_after144Scanlines_entersVBlank() {
        // Run 144 complete scanlines
        repeat(144) {
            advanceScanline()
        }
        assertEquals(144.toUByte(), ppu.ly)
        val statMode = ppu.stat.toInt() and 0x03
        assertEquals(1, statMode)
        assertTrue(ppu.vblankIrq)
    }

    @Test
    fun vblank_after10Lines_returnsToScanline0() {
        // Run 144 scanlines to enter VBlank
        repeat(144) { advanceScanline() }
        ppu.vblankIrq = false

        // Run 10 more VBlank scanlines (154 total)
        repeat(10) { advanceScanline() }

        assertEquals(0.toUByte(), ppu.ly)
        val statMode = ppu.stat.toInt() and 0x03
        assertEquals(2, statMode)
    }

    @Test
    fun lycMatch_setsStatBit2() {
        ppu.lyc = 1.toUByte()
        // Run one full scanline so LY becomes 1
        advanceScanline()
        // bit 2 of STAT should be set
        assertTrue((ppu.stat.toInt() and 0x04) != 0)
    }

    @Test
    fun lycMismatch_clearsStatBit2() {
        ppu.lyc = 5.toUByte()
        advanceScanline()
        // LY is 1, LYC is 5, no match
        assertFalse((ppu.stat.toInt() and 0x04) != 0)
    }

    @Test
    fun lcdDisabled_doesNotAdvanceDots() {
        // Disable LCD (clear bit 7 of LCDC)
        ppu.lcdc = (ppu.lcdc.toInt() and 0x7F).toUByte()
        val initialLy = ppu.ly
        advanceScanline()
        // LY should not have changed since LCD is disabled
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
        // Tile 0, row 0: lo = 0xFF, hi = 0x00 -> color index 1 for each pixel (hi bit 0, lo bit 1 = 01)
        ppu.writeVram(0x8000.toUShort(), 0xFF.toUByte())  // lo byte of tile 0, row 0
        ppu.writeVram(0x8001.toUShort(), 0x00.toUByte())  // hi byte -> color index 1

        // Enable LCD, BG enabled, tile data = 0x8000 (unsigned, lcdc bit 4 = 1)
        ppu.lcdc = 0x91.toUByte()  // bit 7=1 (LCD on), bit 4=1 (tile data 0x8000), bit 0=1 (BG on)

        // Set palette: color 1 maps to palette slot 1 (bits 3-2 = 01 = index 1)
        ppu.bgp = 0b00000100.toUByte()  // color 0 -> slot 0, color 1 -> slot 1

        // Run mode 2 + mode 3 to trigger renderScanline
        // Mode 2 ends at 80 cycles, mode 3 ends at 80+172=252 cycles
        advanceCycles(80 + 172)

        // The first pixel of scanline 0 should be palette color 1 (from color index 1)
        val expectedColor = PPU.GB_PALETTE[1]
        assertEquals(expectedColor, ppu.frameBuffer[0])
    }
}
