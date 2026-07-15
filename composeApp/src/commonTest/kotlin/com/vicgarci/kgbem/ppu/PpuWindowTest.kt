package com.vicgarci.kgbem.ppu

import com.vicgarci.kgbem.cartridge.RomOnlyCartridge
import com.vicgarci.kgbem.cpu.MemoryBus
import kotlin.test.Test
import kotlin.test.assertEquals

class PpuWindowTest {

    private fun createTestPpu(): Triple<Ppu, MemoryBus, RecordingFrameSink> {
        val cartridge = RomOnlyCartridge(ByteArray(0x8000))
        val bus = MemoryBus(cartridge)
        val sink = RecordingFrameSink()
        val ppu = Ppu(bus, sink)
        return Triple(ppu, bus, sink)
    }

    private val LCDC: UShort = 0xFF40u
    private val BGP: UShort = 0xFF47u
    private val WY: UShort = 0xFF4Au
    private val WX: UShort = 0xFF4Bu
    private val SCX: UShort = 0xFF43u
    private val SCY: UShort = 0xFF42u

    /**
     * Window at WX=7, WY=0 covers the entire screen starting at pixel 0.
     *
     * LCDC=0xB1: LCD on (bit7), window enabled (bit5), BG on (bit0),
     *            tile data 0x8000 (bit4), BG map 0x9800 (bit3=0),
     *            window map 0x9800 (bit6=0).
     * Window tile 0 first row = [0xFF, 0xFF] -> colour 3 = black.
     * Background tile 1 first row = [0xFF, 0x00] -> colour 1 = light grey.
     * Both share tile map 0x9800, but window reads tile index at (0,0) = tile 0.
     * Since window starts at pixel 0, pixel 0 should be window colour 3 = black.
     */
    @Test
    fun windowAtWx7Wy0OverridesBackgroundAtPixelZero() {
        val (ppu, bus, sink) = createTestPpu()

        // LCDC=0xB1: LCD on, window enable, tile data 0x8000, BG on
        bus.writeByte(LCDC, 0xB1u.toUByte())
        bus.writeByte(BGP, 0xE4u.toUByte())
        bus.writeByte(WY, 0u.toUByte())
        bus.writeByte(WX, 7u.toUByte()) // window starts at pixel 0
        bus.writeByte(SCX, 0u.toUByte())
        bus.writeByte(SCY, 0u.toUByte())

        // Tile 0 at 0x8000: row 0 = [0xFF, 0xFF] -> colour 3 (black)
        bus.writeByte(0x8000u.toUShort(), 0xFFu.toUByte())
        bus.writeByte(0x8001u.toUShort(), 0xFFu.toUByte())

        // Tile 1 at 0x8010: row 0 = [0xFF, 0x00] -> colour 1 (light grey)
        bus.writeByte(0x8010u.toUShort(), 0xFFu.toUByte())
        bus.writeByte(0x8011u.toUShort(), 0x00u.toUByte())

        // Tile map 0x9800: entry 0 = tile 0 (window reads this)
        bus.writeByte(0x9800u.toUShort(), 0x00u.toUByte())
        // Background also uses 0x9800, entry 0 = tile 0, so set entry 1 for BG tile 1
        bus.writeByte(0x9801u.toUShort(), 0x01u.toUByte())

        ppu.step(70224)

        val frame = sink.lastFrame!!
        val black = 0xFF000000.toInt()
        assertEquals(black, frame[0], "Pixel 0 should be window colour 3 (black)")
    }

    /**
     * Window at WX=14, WY=0 starts at pixel 7.
     * Pixels 0-6 should show background, pixel 7+ should show window.
     *
     * Background tile map entry 0 = tile 1 -> colour 1 = light grey.
     * Window tile map entry 0 = tile 0 -> colour 3 = black.
     */
    @Test
    fun windowAtWx14StartsAtPixel7BackgroundBeforeThat() {
        val (ppu, bus, sink) = createTestPpu()

        bus.writeByte(LCDC, 0xB1u.toUByte())
        bus.writeByte(BGP, 0xE4u.toUByte())
        bus.writeByte(WY, 0u.toUByte())
        bus.writeByte(WX, 14u.toUByte()) // window starts at pixel 7
        bus.writeByte(SCX, 0u.toUByte())
        bus.writeByte(SCY, 0u.toUByte())

        // Tile 0 at 0x8000: row 0 = [0xFF, 0xFF] -> colour 3 (black)
        bus.writeByte(0x8000u.toUShort(), 0xFFu.toUByte())
        bus.writeByte(0x8001u.toUShort(), 0xFFu.toUByte())

        // Tile 1 at 0x8010: row 0 = [0xFF, 0x00] -> colour 1 (light grey)
        bus.writeByte(0x8010u.toUShort(), 0xFFu.toUByte())
        bus.writeByte(0x8011u.toUShort(), 0x00u.toUByte())

        // Tile map: entry 0 = tile 1 (background shows light grey)
        bus.writeByte(0x9800u.toUShort(), 0x01u.toUByte())

        // Window also uses tile map 0x9800, but window reads from its own
        // internal coordinates. Window pixel (0,0) maps to tile map entry 0.
        // We want window to show tile 0 (black), but entry 0 is already tile 1.
        // The window reads the SAME tile map. So we need to set entry 0 = tile 1
        // for the background, but the window also reads entry 0 as tile 1.
        //
        // To differentiate, use a different tile map for the window via LCDC bit 6.
        // LCDC=0xF1: bit6=1 -> window map 0x9C00
        bus.writeByte(LCDC, 0xF1u.toUByte())

        // Window tile map at 0x9C00: entry 0 = tile 0 (black)
        bus.writeByte(0x9C00u.toUShort(), 0x00u.toUByte())

        ppu.step(70224)

        val frame = sink.lastFrame!!
        val lightGrey = 0xFFAAAAAA.toInt()
        val black = 0xFF000000.toInt()

        // Pixels 0-6 should be background (light grey)
        for (x in 0 until 7) {
            assertEquals(lightGrey, frame[x], "Pixel $x should be background light grey")
        }

        // Pixel 7+ should be window (black)
        assertEquals(black, frame[7], "Pixel 7 should be window colour 3 (black)")
        assertEquals(black, frame[8], "Pixel 8 should be window colour 3 (black)")
    }
}
