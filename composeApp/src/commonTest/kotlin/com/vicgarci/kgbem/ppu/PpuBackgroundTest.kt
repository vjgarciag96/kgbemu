package com.vicgarci.kgbem.ppu

import com.vicgarci.kgbem.cartridge.RomOnlyCartridge
import com.vicgarci.kgbem.cpu.MemoryBus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PpuBackgroundTest {

    private fun createTestPpu(): Triple<Ppu, MemoryBus, RecordingFrameSink> {
        val cartridge = RomOnlyCartridge(ByteArray(0x8000))
        val bus = MemoryBus(cartridge)
        val sink = RecordingFrameSink()
        val ppu = Ppu(bus, sink)
        return Triple(ppu, bus, sink)
    }

    // ---- Helper addresses ----
    private val LCDC: UShort = 0xFF40u
    private val SCY: UShort = 0xFF42u
    private val SCX: UShort = 0xFF43u
    private val BGP: UShort = 0xFF47u

    /**
     * Known tile pattern test.
     *
     * LCDC=0x91 (LCD on, BG enabled, tile data 0x8000, tile map 0x9800).
     * BGP=0xE4 (standard: 0->white, 1->light grey, 2->dark grey, 3->black).
     * Tile 0 at 0x8000: first row = [0xFF, 0x00] -> colour 1 (light grey) for all 8 pixels.
     * Tile map at 0x9800 entry (0,0) = tile index 0.
     * SCX=0, SCY=0.
     * After one full frame, top-left pixel should be light grey (0xFFAAAAAA).
     */
    @Test
    fun knownTilePatternRendersCorrectColour() {
        val (ppu, bus, sink) = createTestPpu()

        // LCDC: LCD on (bit7), BG on (bit0), tile data 0x8000 (bit4)
        bus.writeByte(LCDC, 0x91u.toUByte())
        // BGP: standard mapping 0->white, 1->light, 2->dark, 3->black = 0b11_10_01_00 = 0xE4
        bus.writeByte(BGP, 0xE4u.toUByte())
        // SCX=0, SCY=0
        bus.writeByte(SCX, 0u.toUByte())
        bus.writeByte(SCY, 0u.toUByte())

        // Write tile 0 at 0x8000: row 0 low=0xFF, high=0x00
        // colour = bit1=0, bit0=1 for every pixel -> colour 1
        bus.writeByte(0x8000u.toUShort(), 0xFFu.toUByte()) // low byte
        bus.writeByte(0x8001u.toUShort(), 0x00u.toUByte()) // high byte

        // Write tile index 0 at tile map 0x9800 (first entry)
        bus.writeByte(0x9800u.toUShort(), 0x00u.toUByte())

        // Step one full frame
        ppu.step(70224)

        val frame = sink.lastFrame!!
        val lightGrey = 0xFFAAAAAA.toInt()
        assertEquals(lightGrey, frame[0], "Top-left pixel should be light grey (colour 1)")
    }

    /**
     * SCX scroll test.
     *
     * SCX=8 shifts background right by 8 pixels, so pixel 0 of scanline 0
     * comes from tile column 1 instead of tile column 0.
     */
    @Test
    fun scxScrollShiftsBackgroundByEightPixels() {
        val (ppu, bus, sink) = createTestPpu()

        bus.writeByte(LCDC, 0x91u.toUByte())
        bus.writeByte(BGP, 0xE4u.toUByte())
        bus.writeByte(SCX, 8u.toUByte()) // shift right 8 pixels = 1 tile
        bus.writeByte(SCY, 0u.toUByte())

        // Tile 0 at 0x8000: all rows = [0x00, 0x00] -> colour 0 (white)
        // (default memory is already zero, so tile 0 = white)

        // Tile 1 at 0x8010: first row = [0xFF, 0xFF] -> colour 3 (black)
        bus.writeByte(0x8010u.toUShort(), 0xFFu.toUByte()) // low byte
        bus.writeByte(0x8011u.toUShort(), 0xFFu.toUByte()) // high byte

        // Tile map: entry (0,0)=tile 0, entry (0,1)=tile 1
        bus.writeByte(0x9800u.toUShort(), 0x00u.toUByte()) // tile 0
        bus.writeByte(0x9801u.toUShort(), 0x01u.toUByte()) // tile 1

        ppu.step(70224)

        val frame = sink.lastFrame!!
        val black = 0xFF000000.toInt()
        val white = 0xFFFFFFFF.toInt()

        // With SCX=8, pixel 0 reads from bgX=8, which is tile column 1 -> tile 1 -> black
        assertEquals(black, frame[0], "Pixel 0 should come from tile column 1 (black)")

        // Pixel 8 reads from bgX=16 -> tile column 2 -> tile index 0 (default) -> white
        assertEquals(white, frame[8], "Pixel 8 should come from tile column 2 (white, default tile)")
    }

    /**
     * LCD off test.
     *
     * LCDC bit 7 = 0 -> all pixels in output frame are 0xFFFFFFFF (white).
     */
    @Test
    fun lcdOffOutputsAllWhitePixels() {
        val (ppu, bus, sink) = createTestPpu()

        // LCDC bit 7 = 0 (LCD off)
        bus.writeByte(LCDC, 0x00u.toUByte())

        ppu.step(70224)

        val frame = sink.lastFrame!!
        val white = 0xFFFFFFFF.toInt()
        assertTrue(frame.all { it == white }, "All pixels should be white when LCD is off")
        assertEquals(160 * 144, frame.size)
    }

    /**
     * Verify signed tile data addressing (LCDC bit 4 = 0).
     *
     * When LCDC bit 4 is 0, tile data uses 0x8800 signed indexing
     * (tile 0 starts at 0x9000).
     */
    @Test
    fun signedTileDataAddressingUsesTileAtNineThousand() {
        val (ppu, bus, sink) = createTestPpu()

        // LCDC=0x81: LCD on (bit7), BG on (bit0), tile data 0x8800 signed (bit4=0)
        bus.writeByte(LCDC, 0x81u.toUByte())
        bus.writeByte(BGP, 0xE4u.toUByte())
        bus.writeByte(SCX, 0u.toUByte())
        bus.writeByte(SCY, 0u.toUByte())

        // Tile index 0 in signed mode -> address 0x9000
        // Write row 0: low=0xFF, high=0xFF -> colour 3 (black)
        bus.writeByte(0x9000u.toUShort(), 0xFFu.toUByte())
        bus.writeByte(0x9001u.toUShort(), 0xFFu.toUByte())

        // Tile map entry (0,0) = tile index 0
        bus.writeByte(0x9800u.toUShort(), 0x00u.toUByte())

        ppu.step(70224)

        val frame = sink.lastFrame!!
        val black = 0xFF000000.toInt()
        assertEquals(black, frame[0], "With signed addressing, tile 0 should be read from 0x9000")
    }
}
