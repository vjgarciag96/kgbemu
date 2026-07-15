package com.vicgarci.kgbem.ppu

import com.vicgarci.kgbem.cartridge.RomOnlyCartridge
import com.vicgarci.kgbem.cpu.MemoryBus
import kotlin.test.Test
import kotlin.test.assertEquals

class PpuSpriteTest {

    private fun createTestPpu(): Triple<Ppu, MemoryBus, RecordingFrameSink> {
        val cartridge = RomOnlyCartridge(ByteArray(0x8000))
        val bus = MemoryBus(cartridge)
        val sink = RecordingFrameSink()
        val ppu = Ppu(bus, sink)
        return Triple(ppu, bus, sink)
    }

    // ---- Helper addresses ----
    private val LCDC: UShort = 0xFF40u
    private val BGP: UShort = 0xFF47u
    private val OBP0: UShort = 0xFF48u
    private val OBP1: UShort = 0xFF49u
    private val OAM_BASE: UShort = 0xFE00u

    private val BLACK = 0xFF000000.toInt()
    private val WHITE = 0xFFFFFFFF.toInt()
    private val LIGHT_GREY = 0xFFAAAAAA.toInt()
    private val DARK_GREY = 0xFF555555.toInt()

    private fun writeOamEntry(bus: MemoryBus, index: Int, y: Int, x: Int, tile: Int, attrs: Int) {
        val base = 0xFE00 + index * 4
        bus.writeByte((base).toUShort(), y.toUByte())
        bus.writeByte((base + 1).toUShort(), x.toUByte())
        bus.writeByte((base + 2).toUShort(), tile.toUByte())
        bus.writeByte((base + 3).toUShort(), attrs.toUByte())
    }

    /**
     * Basic sprite render.
     * Sprite at OAM[0]: Y=16, X=8 (top-left of screen), tile=0, attrs=0.
     * Tile 0 row 0 = [0xFF, 0xFF] -> colour 3 (black).
     * OBP0=0xE4. LCDC=0x82 (LCD on, sprites enabled, BG off).
     * After one frame, pixel 0 should be black.
     */
    @Test
    fun basicSpriteRendersBlackPixelAtTopLeft() {
        val (ppu, bus, sink) = createTestPpu()

        bus.writeByte(LCDC, 0x82u.toUByte()) // LCD on + sprites on, BG off
        bus.writeByte(OBP0, 0xE4u.toUByte()) // standard palette

        // Sprite at top-left: Y=16 means screenY=0, X=8 means screenX=0
        writeOamEntry(bus, 0, 16, 8, 0, 0)

        // Tile 0 at 0x8000, row 0: [0xFF, 0xFF] -> colour 3 for all 8 pixels
        bus.writeByte(0x8000u.toUShort(), 0xFFu.toUByte())
        bus.writeByte(0x8001u.toUShort(), 0xFFu.toUByte())

        ppu.step(70224)

        assertEquals(BLACK, sink.lastFrame!![0])
    }

    /**
     * Sprite colour 0 is transparent.
     * Tile data gives colour 0 for pixel 0 -> BG colour 0 (white) shows through.
     */
    @Test
    fun spriteColourZeroIsTransparent() {
        val (ppu, bus, sink) = createTestPpu()

        bus.writeByte(LCDC, 0x82u.toUByte())
        bus.writeByte(OBP0, 0xE4u.toUByte())

        writeOamEntry(bus, 0, 16, 8, 0, 0)

        // Tile 0 row 0: [0x00, 0x00] -> colour 0 for all pixels (transparent)
        bus.writeByte(0x8000u.toUShort(), 0x00u.toUByte())
        bus.writeByte(0x8001u.toUShort(), 0x00u.toUByte())

        ppu.step(70224)

        // Colour 0 is transparent, so BG colour 0 (white) shows through
        assertEquals(WHITE, sink.lastFrame!![0])
    }

    /**
     * OBJ-to-BG priority.
     * Attr bit 7 = 1, BG pixel is non-zero colour -> sprite is hidden behind BG.
     */
    @Test
    fun objToBgPriorityHidesSpriteWhenBgNonZero() {
        val (ppu, bus, sink) = createTestPpu()

        // LCD on, BG on, sprites on, tile data 0x8000
        bus.writeByte(LCDC, 0x93u.toUByte()) // 0x80|0x10|0x02|0x01
        bus.writeByte(BGP, 0xE4u.toUByte())
        bus.writeByte(OBP0, 0xE4u.toUByte())

        // BG tile 0 row 0: colour 3 (non-zero) at every pixel
        bus.writeByte(0x8000u.toUShort(), 0xFFu.toUByte())
        bus.writeByte(0x8001u.toUShort(), 0xFFu.toUByte())
        bus.writeByte(0x9800u.toUShort(), 0x00u.toUByte()) // tile map -> tile 0

        // Sprite at OAM[0] with priority bit 7 = 1 (behind BG colours 1-3)
        // Use a different tile so sprite has its own colour
        writeOamEntry(bus, 0, 16, 8, 1, 0x80) // attr bit 7 = 1

        // Tile 1 at 0x8010, row 0: [0xFF, 0x00] -> colour 1 (light grey via OBP0)
        bus.writeByte(0x8010u.toUShort(), 0xFFu.toUByte())
        bus.writeByte(0x8011u.toUShort(), 0x00u.toUByte())

        ppu.step(70224)

        // BG colour is 3 (black), sprite has priority behind -> BG wins
        assertEquals(BLACK, sink.lastFrame!![0])
    }

    /**
     * X flip.
     * Tile row = [0x80, 0x00] -> colour 1 at pixel 0 (leftmost), colour 0 elsewhere.
     * With X flip (attr bit 5 = 1), colour 1 moves to pixel 7.
     */
    @Test
    fun xFlipMirrorsSpriteTileHorizontally() {
        val (ppu, bus, sink) = createTestPpu()

        bus.writeByte(LCDC, 0x82u.toUByte()) // LCD on + sprites on, BG off
        bus.writeByte(OBP0, 0xE4u.toUByte())

        // Sprite at top-left with X flip
        writeOamEntry(bus, 0, 16, 8, 0, 0x20) // attr bit 5 = X flip

        // Tile 0 row 0: low=0x80, high=0x00
        // Without flip: pixel 0 bit 7 -> colour 1, pixels 1-7 -> colour 0
        // With flip: pixel 7 -> colour 1, pixels 0-6 -> colour 0
        bus.writeByte(0x8000u.toUShort(), 0x80u.toUByte())
        bus.writeByte(0x8001u.toUShort(), 0x00u.toUByte())

        ppu.step(70224)

        // Pixel 0 should be white (colour 0 -> transparent, BG colour 0 shows)
        assertEquals(WHITE, sink.lastFrame!![0])
        // Pixel 7 should be light grey (colour 1 via OBP0=0xE4)
        assertEquals(LIGHT_GREY, sink.lastFrame!![7])
    }

    /**
     * Max 10 sprites per scanline.
     * Place 11 sprites at the same scanline. Only first 10 (OAM indices 0-9) render.
     */
    @Test
    fun maxTenSpritesPerScanline() {
        val (ppu, bus, sink) = createTestPpu()

        bus.writeByte(LCDC, 0x82u.toUByte())
        bus.writeByte(OBP0, 0xE4u.toUByte())

        // Place 11 sprites all on scanline 0, each at a different X position
        for (i in 0..10) {
            // Y=16 -> screenY=0, X=8+i*8 -> each sprite at different x
            writeOamEntry(bus, i, 16, 8 + i * 8, 0, 0)
        }

        // Tile 0 row 0: colour 3 (black) for all pixels
        bus.writeByte(0x8000u.toUShort(), 0xFFu.toUByte())
        bus.writeByte(0x8001u.toUShort(), 0xFFu.toUByte())

        ppu.step(70224)

        val frame = sink.lastFrame!!

        // Sprites 0-9 should render (their first pixel is at x = 0, 8, 16, ..., 72)
        for (i in 0..9) {
            val x = i * 8
            assertEquals(BLACK, frame[x], "Sprite $i at pixel $x should be black")
        }

        // Sprite 10 at x=80 should NOT render (11th sprite, exceeds limit)
        assertEquals(WHITE, frame[80], "Sprite 10 at pixel 80 should be white (not rendered)")
    }
}
