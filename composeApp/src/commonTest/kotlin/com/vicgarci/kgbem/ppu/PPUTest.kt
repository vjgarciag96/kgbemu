package com.vicgarci.kgbem.ppu

import com.vicgarci.kgbem.cpu.MemoryBus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PPUTest {

    private fun makeBus(): MemoryBus = MemoryBus().also { bus ->
        bus.initializePostBoot()
    }

    // ── Scanline / LY advancement ──────────────────────────────────────────

    @Test
    fun ly_increments_after_456_dots() {
        val bus = makeBus()
        val ppu = PPU(bus)

        ppu.tick(456)

        assertEquals(1, bus.readByte(0xFF44.toUShort()).toInt())
    }

    @Test
    fun ly_wraps_at_154() {
        val bus = makeBus()
        val ppu = PPU(bus)

        ppu.tick(456 * 154)

        assertEquals(0, bus.readByte(0xFF44.toUShort()).toInt())
    }

    @Test
    fun vblank_interrupt_fires_at_ly_144() {
        val bus = makeBus()
        bus.writeByte(0xFF0F.toUShort(), 0x00.toUByte())  // clear all interrupt flags
        val ppu = PPU(bus)

        ppu.tick(456 * 144)

        assertTrue(ppu.vblankOccurred, "vblankOccurred should be true after LY reaches 144")
        val interruptFlag = bus.readByte(0xFF0F.toUShort()).toInt()
        assertTrue(interruptFlag and 0x01 != 0, "V-Blank interrupt flag (bit 0 of IF) must be set")
    }

    @Test
    fun vblank_does_not_fire_before_ly_144() {
        val bus = makeBus()
        bus.writeByte(0xFF0F.toUShort(), 0x00.toUByte())  // clear all interrupt flags
        val ppu = PPU(bus)

        ppu.tick(456 * 143)

        assertFalse(ppu.vblankOccurred)
        val interruptFlag = bus.readByte(0xFF0F.toUShort()).toInt()
        assertEquals(0, interruptFlag and 0x01, "V-Blank flag must not be set before line 144")
    }

    // ── PPU mode transitions ───────────────────────────────────────────────

    @Test
    fun mode_is_oam_search_at_start_of_visible_line() {
        val bus = makeBus()
        val ppu = PPU(bus)

        ppu.tick(1)

        val stat = bus.readByte(0xFF41.toUShort()).toInt()
        assertEquals(2, stat and 0x03, "Mode should be 2 (OAM Search) at dot 1 of a visible line")
    }

    @Test
    fun mode_is_pixel_transfer_after_oam_search() {
        val bus = makeBus()
        val ppu = PPU(bus)

        ppu.tick(81)

        val stat = bus.readByte(0xFF41.toUShort()).toInt()
        assertEquals(3, stat and 0x03, "Mode should be 3 (Pixel Transfer) after dot 80")
    }

    @Test
    fun mode_is_hblank_after_pixel_transfer() {
        val bus = makeBus()
        val ppu = PPU(bus)

        ppu.tick(253)

        val stat = bus.readByte(0xFF41.toUShort()).toInt()
        assertEquals(0, stat and 0x03, "Mode should be 0 (H-Blank) after dot 252")
    }

    @Test
    fun mode_is_vblank_after_line_144() {
        val bus = makeBus()
        val ppu = PPU(bus)

        ppu.tick(456 * 144 + 1)

        val stat = bus.readByte(0xFF41.toUShort()).toInt()
        assertEquals(1, stat and 0x03, "Mode should be 1 (V-Blank) after line 144")
    }

    // ── LY=LYC coincidence ─────────────────────────────────────────────────

    @Test
    fun lyc_coincidence_flag_is_set_when_ly_equals_lyc() {
        val bus = makeBus()
        bus.writeByte(0xFF45.toUShort(), 5.toUByte())  // LYC = 5
        val ppu = PPU(bus)

        ppu.tick(456 * 5)

        val stat = bus.readByte(0xFF41.toUShort()).toInt()
        assertTrue(stat and 0x04 != 0, "LYC=LY flag (bit 2 of STAT) should be set when LY == LYC")
    }

    @Test
    fun lyc_coincidence_flag_is_clear_when_ly_differs_from_lyc() {
        val bus = makeBus()
        bus.writeByte(0xFF45.toUShort(), 10.toUByte())  // LYC = 10
        val ppu = PPU(bus)

        ppu.tick(456 * 5)

        val stat = bus.readByte(0xFF41.toUShort()).toInt()
        assertEquals(0, stat and 0x04, "LYC=LY flag must be clear when LY ≠ LYC")
    }

    // ── Background rendering ───────────────────────────────────────────────

    @Test
    fun solid_tile_renders_correct_color_across_scanline() {
        val bus = makeBus()
        // Tile where every pixel is color ID 1 (lo=0xFF, hi=0x00)
        // BGP=0xE4: color 1 → shade 1 → DMG_COLORS[1]
        for (row in 0 until 8) {
            bus.memory[0x8000 + row * 2] = 0xFF.toUByte()  // lo
            bus.memory[0x8001 + row * 2] = 0x00.toUByte()  // hi
        }
        for (i in 0 until 32 * 32) bus.memory[0x9800 + i] = 0x00.toUByte()
        bus.memory[0xFF40] = 0x91.toUByte()  // LCDC
        bus.memory[0xFF47] = 0xE4.toUByte()  // BGP
        bus.memory[0xFF42] = 0x00.toUByte()  // SCY
        bus.memory[0xFF43] = 0x00.toUByte()  // SCX

        val ppu = PPU(bus)
        ppu.tick(456)

        val expected = PPU.DMG_COLORS[1]
        for (x in 0 until PPU.SCREEN_WIDTH) {
            assertEquals(expected, ppu.framebuffer[x], "All pixels on scanline 0 should be shade 1")
        }
    }

    @Test
    fun background_scroll_shifts_tile_map_correctly() {
        val bus = makeBus()
        // Tile 0: all color 0; tile 1: all color 3 (lo=hi=0xFF → color ID 3)
        for (row in 0 until 8) {
            bus.memory[0x8000 + row * 2] = 0x00.toUByte()  // tile 0 lo
            bus.memory[0x8001 + row * 2] = 0x00.toUByte()  // tile 0 hi
            bus.memory[0x8010 + row * 2] = 0xFF.toUByte()  // tile 1 lo
            bus.memory[0x8011 + row * 2] = 0xFF.toUByte()  // tile 1 hi
        }
        for (col in 0 until 32) bus.memory[0x9800 + col] = if (col == 0) 0x00.toUByte() else 0x01.toUByte()
        bus.memory[0xFF40] = 0x91.toUByte()
        bus.memory[0xFF47] = 0xE4.toUByte()
        bus.memory[0xFF42] = 0x00.toUByte()  // SCY = 0
        bus.memory[0xFF43] = 0x08.toUByte()  // SCX = 8 → tile column 1 starts at screen X = 0

        val ppu = PPU(bus)
        ppu.tick(456)

        assertEquals(
            PPU.DMG_COLORS[3],
            ppu.framebuffer[0],
            "With SCX=8, screen X=0 should show tile 1 (shade 3)",
        )
    }

    // ── LCD disabled ──────────────────────────────────────────────────────

    @Test
    fun ppu_does_nothing_when_lcd_disabled() {
        val bus = makeBus()
        bus.memory[0xFF40] = 0x00.toUByte()  // LCDC bit 7 = 0 → LCD off
        val ppu = PPU(bus)

        ppu.tick(456 * 144)

        assertFalse(ppu.vblankOccurred, "V-Blank must not fire when LCD is disabled")
        assertEquals(0, bus.readByte(0xFF44.toUShort()).toInt(), "LY must not advance when LCD is off")
    }

    // ── DMA transfer ──────────────────────────────────────────────────────

    @Test
    fun dma_transfer_copies_source_to_oam() {
        val bus = makeBus()
        for (i in 0 until 0xA0) bus.memory[0xC000 + i] = (i and 0xFF).toUByte()
        bus.writeByte(0xFF46.toUShort(), 0xC0.toUByte())

        for (i in 0 until 0xA0) {
            assertEquals(
                (i and 0xFF).toUByte(),
                bus.readByte((0xFE00 + i).toUShort()),
                "OAM byte $i should match source after DMA",
            )
        }
    }
}
