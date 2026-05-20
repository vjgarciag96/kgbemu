package com.vicgarci.kgbem.ppu

import com.vicgarci.kgbem.cpu.MemoryBus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PPUTest {

    private val memoryBus = MemoryBus()
    private val ppu = PPU(memoryBus)

    private fun lcdOn() {
        memoryBus.writeByte(PPU.LCDC_ADDR, 0x91u) // LCD on, BG on, tile data 0x8000
        memoryBus.writeByte(PPU.BGP_ADDR, 0xE4u)   // standard palette: 3,2,1,0
    }

    @Test
    fun ppu_advances_through_oam_then_transfer_then_hblank() {
        lcdOn()
        // After OAM_DOTS (80) we should be in pixel-transfer (mode 3)
        ppu.tick(80)
        val statAfterOam = memoryBus.readByte(PPU.STAT_ADDR).toInt() and 0x03
        assertEquals(3, statAfterOam)

        // After TRANSFER_DOTS (172) we should be in H-blank (mode 0)
        ppu.tick(172)
        val statAfterTransfer = memoryBus.readByte(PPU.STAT_ADDR).toInt() and 0x03
        assertEquals(0, statAfterTransfer)
    }

    @Test
    fun ppu_ly_increments_after_each_scanline() {
        lcdOn()
        assertEquals(0, memoryBus.readByte(PPU.LY_ADDR).toInt())
        ppu.tick(456) // one full scanline
        assertEquals(1, memoryBus.readByte(PPU.LY_ADDR).toInt())
    }

    @Test
    fun vblank_fires_at_line_144() {
        lcdOn()
        assertFalse(ppu.vBlankInterrupt)
        ppu.tick(144 * 456) // advance to end of line 143, start of line 144
        assertTrue(ppu.vBlankInterrupt)
    }

    @Test
    fun ly_resets_to_zero_after_full_frame() {
        lcdOn()
        ppu.tick(154 * 456) // full frame (144 visible + 10 V-blank lines)
        assertEquals(0, memoryBus.readByte(PPU.LY_ADDR).toInt())
    }

    @Test
    fun ppu_renders_white_pixels_when_bg_disabled() {
        // LCDC bit 0 = 0 → BG disabled
        memoryBus.writeByte(PPU.LCDC_ADDR, 0x80u) // LCD on, BG off

        ppu.tick(456) // render line 0

        // All pixels should be white (0xFFFFFFFF)
        for (x in 0 until PPU.SCREEN_WIDTH) {
            assertEquals(PPU.SHADES[0], ppu.frameBuffer[x], "pixel x=$x should be white")
        }
    }

    @Test
    fun ppu_renders_black_pixels_with_all_ones_tile_and_black_palette() {
        lcdOn()
        // BGP = 0xFF → all color indices map to shade 3 (black)
        memoryBus.writeByte(PPU.BGP_ADDR, 0xFFu)
        // Write a solid tile (all bits set → color index 3 everywhere) at tile 0
        // Tile 0 data: 16 bytes at 0x8000; each row = 2 bytes, both 0xFF
        for (i in 0 until 16) {
            memoryBus.writeByte((0x8000 + i).toUShort(), 0xFFu)
        }
        // Tile map at 0x9800: ensure tile 0 is selected (default is 0)

        ppu.tick(456) // render line 0

        for (x in 0 until PPU.SCREEN_WIDTH) {
            assertEquals(PPU.SHADES[3], ppu.frameBuffer[x], "pixel x=$x should be black")
        }
    }

    @Test
    fun ppu_frame_buffer_is_160_by_144() {
        assertEquals(PPU.SCREEN_WIDTH * PPU.SCREEN_HEIGHT, ppu.frameBuffer.size)
    }

    @Test
    fun lcd_off_holds_ly_at_zero() {
        memoryBus.writeByte(PPU.LCDC_ADDR, 0x00u) // LCD off
        ppu.tick(1000)
        assertEquals(0, memoryBus.readByte(PPU.LY_ADDR).toInt())
    }
}
