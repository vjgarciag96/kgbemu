package com.vicgarci.kgbem.ppu

import com.vicgarci.kgbem.cpu.MemoryBus

/**
 * Game Boy Pixel Processing Unit.
 *
 * Timing (all in T-cycles, 1 machine cycle = 4 T-cycles):
 *   Mode 2 – OAM Search   :  80 dots
 *   Mode 3 – Pixel Transfer: 172 dots
 *   Mode 0 – H-Blank       : 204 dots
 *   Total per scanline     : 456 dots
 *   Visible scanlines      : 0–143  (144 lines)
 *   V-Blank scanlines      : 144–153 (10 lines)
 *   Total per frame        : 70 224 dots ≈ 59.7 fps
 */
class PPU(private val memoryBus: MemoryBus) {

    /** ARGB pixel data for the 160×144 display. Updated once per frame during V-Blank entry. */
    val framebuffer: IntArray = IntArray(SCREEN_WIDTH * SCREEN_HEIGHT) { DMG_COLORS[0] }

    /** True for exactly one [tick] call after V-Blank starts; callers must reset this. */
    var vblankOccurred: Boolean = false

    private var scanlineDots: Int = 0

    // ── Public API ──────────────────────────────────────────────────────────

    /** Advance the PPU by [cycles] T-cycles. */
    fun tick(cycles: Int) {
        if (memoryBus.readByte(LCDC_ADDR).toInt() and LCDC_LCD_ENABLE == 0) return

        scanlineDots += cycles

        if (scanlineDots >= DOTS_PER_LINE) {
            scanlineDots -= DOTS_PER_LINE
            advanceScanline()
        } else {
            updateModeForCurrentDot()
        }
    }

    // ── Scanline advancement ────────────────────────────────────────────────

    private fun advanceScanline() {
        val ly = memoryBus.readByte(LY_ADDR).toInt()

        // Render the scanline that just finished
        if (ly < SCREEN_HEIGHT) {
            renderScanline(ly)
        }

        val newLY = (ly + 1) % LINES_PER_FRAME
        memoryBus.writeLY(newLY.toUByte())
        checkLycCoincidence(newLY)

        when {
            newLY == SCREEN_HEIGHT -> {
                // Entering V-Blank
                vblankOccurred = true
                memoryBus.setInterruptFlagBit(VBLANK_INTERRUPT_BIT, true)
                setMode(MODE_VBLANK)
            }
            newLY < SCREEN_HEIGHT -> setMode(MODE_OAM_SEARCH)
            // Lines 145–153: stay in V-Blank, no mode change
        }
    }

    private fun updateModeForCurrentDot() {
        val ly = memoryBus.readByte(LY_ADDR).toInt()
        if (ly >= SCREEN_HEIGHT) return  // V-Blank; mode already set
        val mode = when {
            scanlineDots < DOTS_OAM_SEARCH -> MODE_OAM_SEARCH
            scanlineDots < DOTS_PIXEL_TRANSFER -> MODE_PIXEL_TRANSFER
            else -> MODE_HBLANK
        }
        setMode(mode)
    }

    // ── Mode management ─────────────────────────────────────────────────────

    private fun setMode(mode: Int) {
        val stat = memoryBus.readByte(STAT_ADDR).toInt()
        if (stat and 0x03 == mode) return  // No change

        val newStat = (stat and 0xFC) or (mode and 0x03)
        memoryBus.writeByte(STAT_ADDR, newStat.toUByte())

        val statIrq = when (mode) {
            MODE_HBLANK -> stat and STAT_HBLANK_IRQ != 0
            MODE_VBLANK -> stat and STAT_VBLANK_IRQ != 0
            MODE_OAM_SEARCH -> stat and STAT_OAM_IRQ != 0
            else -> false
        }
        if (statIrq) {
            memoryBus.setInterruptFlagBit(LCD_STAT_INTERRUPT_BIT, true)
        }
    }

    private fun checkLycCoincidence(ly: Int) {
        val lyc = memoryBus.readByte(LYC_ADDR).toInt()
        val stat = memoryBus.readByte(STAT_ADDR).toInt()
        val coincidence = ly == lyc
        val newStat = if (coincidence) stat or STAT_LYC_COINCIDENCE else stat and STAT_LYC_COINCIDENCE.inv()
        memoryBus.writeByte(STAT_ADDR, newStat.toUByte())
        if (coincidence && newStat and STAT_LYC_IRQ != 0) {
            memoryBus.setInterruptFlagBit(LCD_STAT_INTERRUPT_BIT, true)
        }
    }

    // ── Rendering ───────────────────────────────────────────────────────────

    private fun renderScanline(ly: Int) {
        val lcdc = memoryBus.readByte(LCDC_ADDR).toInt()

        if (lcdc and LCDC_BG_ENABLE != 0) renderBackground(ly, lcdc)
        if (lcdc and LCDC_WINDOW_ENABLE != 0) renderWindow(ly, lcdc)
        if (lcdc and LCDC_OBJ_ENABLE != 0) renderSprites(ly, lcdc)
    }

    private fun renderBackground(ly: Int, lcdc: Int) {
        val scx = memoryBus.readByte(SCX_ADDR).toInt()
        val scy = memoryBus.readByte(SCY_ADDR).toInt()
        val bgp = memoryBus.readByte(BGP_ADDR).toInt()

        val tileMapBase = if (lcdc and LCDC_BG_TILEMAP != 0) 0x9C00 else 0x9800
        val signedAddressing = lcdc and LCDC_TILEDATA_SELECT == 0

        val yInBg = (ly + scy) and 0xFF
        val tileRow = yInBg / 8
        val tilePixelY = yInBg % 8

        for (x in 0 until SCREEN_WIDTH) {
            val xInBg = (x + scx) and 0xFF
            val tileCol = xInBg / 8
            val tilePixelX = xInBg % 8

            val tileIndex = memoryBus.readByte((tileMapBase + tileRow * 32 + tileCol).toUShort()).toInt()
            val tileAddress = resolveTileAddress(tileIndex, signedAddressing)

            val colorId = tileColorId(tileAddress, tilePixelY, tilePixelX)
            framebuffer[ly * SCREEN_WIDTH + x] = paletteColor(bgp, colorId)
        }
    }

    private fun renderWindow(ly: Int, lcdc: Int) {
        val wy = memoryBus.readByte(WY_ADDR).toInt()
        if (ly < wy) return

        val wx = memoryBus.readByte(WX_ADDR).toInt() - 7
        val bgp = memoryBus.readByte(BGP_ADDR).toInt()
        val tileMapBase = if (lcdc and LCDC_WINDOW_TILEMAP != 0) 0x9C00 else 0x9800
        val signedAddressing = lcdc and LCDC_TILEDATA_SELECT == 0

        val windowLine = ly - wy
        val tileRow = windowLine / 8
        val tilePixelY = windowLine % 8

        for (x in maxOf(0, wx) until SCREEN_WIDTH) {
            val tileCol = (x - wx) / 8
            val tilePixelX = (x - wx) % 8

            val tileIndex = memoryBus.readByte((tileMapBase + tileRow * 32 + tileCol).toUShort()).toInt()
            val tileAddress = resolveTileAddress(tileIndex, signedAddressing)

            val colorId = tileColorId(tileAddress, tilePixelY, tilePixelX)
            framebuffer[ly * SCREEN_WIDTH + x] = paletteColor(bgp, colorId)
        }
    }

    private fun renderSprites(ly: Int, lcdc: Int) {
        val spriteHeight = if (lcdc and LCDC_OBJ_SIZE != 0) 16 else 8
        val obp0 = memoryBus.readByte(OBP0_ADDR).toInt()
        val obp1 = memoryBus.readByte(OBP1_ADDR).toInt()

        var spritesOnLine = 0
        for (i in 0 until 40) {
            if (spritesOnLine >= MAX_SPRITES_PER_LINE) break

            val oamBase = 0xFE00 + i * 4
            val spriteY = memoryBus.readByte(oamBase.toUShort()).toInt() - 16
            if (ly < spriteY || ly >= spriteY + spriteHeight) continue

            spritesOnLine++

            val spriteX = memoryBus.readByte((oamBase + 1).toUShort()).toInt() - 8
            var tileIndex = memoryBus.readByte((oamBase + 2).toUShort()).toInt()
            val attrs = memoryBus.readByte((oamBase + 3).toUShort()).toInt()

            if (spriteHeight == 16) tileIndex = tileIndex and 0xFE

            val palette = if (attrs and OBJ_ATTR_PALETTE != 0) obp1 else obp0
            val flipX = attrs and OBJ_ATTR_FLIP_X != 0
            val flipY = attrs and OBJ_ATTR_FLIP_Y != 0
            val bgPriority = attrs and OBJ_ATTR_BG_PRIORITY != 0

            val rowInSprite = if (flipY) spriteHeight - 1 - (ly - spriteY) else ly - spriteY
            val tileAddress = 0x8000 + tileIndex * 16 + rowInSprite * 2

            val lo = memoryBus.readByte(tileAddress.toUShort()).toInt()
            val hi = memoryBus.readByte((tileAddress + 1).toUShort()).toInt()

            for (tileX in 0 until 8) {
                val screenX = spriteX + tileX
                if (screenX < 0 || screenX >= SCREEN_WIDTH) continue

                val bitPos = if (flipX) tileX else 7 - tileX
                val colorId = ((hi shr bitPos) and 1) shl 1 or ((lo shr bitPos) and 1)
                if (colorId == 0) continue  // Transparent pixel

                val shade = (palette shr (colorId * 2)) and 0x03
                val bgPixelIsNonZero = framebuffer[ly * SCREEN_WIDTH + screenX] != DMG_COLORS[0]
                if (!bgPriority || !bgPixelIsNonZero) {
                    framebuffer[ly * SCREEN_WIDTH + screenX] = DMG_COLORS[shade]
                }
            }
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Resolve the VRAM address for a tile's row data.
     *
     * When [signedAddressing] is true (LCDC bit 4 = 0), indices 0–127 map to
     * 0x9000–0x97FF and indices 128–255 (signed: -128–-1) map to 0x8800–0x8FFF.
     * When false (LCDC bit 4 = 1), all indices map to 0x8000–0x8FFF.
     */
    private fun resolveTileAddress(tileIndex: Int, signedAddressing: Boolean): Int {
        return if (signedAddressing) {
            val signed = if (tileIndex > 127) tileIndex - 256 else tileIndex
            0x9000 + signed * 16
        } else {
            0x8000 + tileIndex * 16
        }
    }

    /** Read the 2-bit color ID for pixel [pixelX] on tile row [pixelY]. */
    private fun tileColorId(tileAddress: Int, pixelY: Int, pixelX: Int): Int {
        val lo = memoryBus.readByte((tileAddress + pixelY * 2).toUShort()).toInt()
        val hi = memoryBus.readByte((tileAddress + pixelY * 2 + 1).toUShort()).toInt()
        val bitPos = 7 - pixelX
        return ((hi shr bitPos) and 1) shl 1 or ((lo shr bitPos) and 1)
    }

    /** Map a 2-bit [colorId] through a palette register byte to a DMG ARGB color. */
    private fun paletteColor(palette: Int, colorId: Int): Int {
        val shade = (palette shr (colorId * 2)) and 0x03
        return DMG_COLORS[shade]
    }

    // ── Constants ────────────────────────────────────────────────────────────

    companion object {
        const val SCREEN_WIDTH = 160
        const val SCREEN_HEIGHT = 144

        /** Classic DMG green-tinted palette in ARGB order (lightest to darkest). */
        val DMG_COLORS: IntArray = intArrayOf(
            0xFF9BBC0F.toInt(),  // shade 0 – lightest
            0xFF8BAC0F.toInt(),  // shade 1
            0xFF306230.toInt(),  // shade 2
            0xFF0F380F.toInt(),  // shade 3 – darkest
        )

        private const val DOTS_PER_LINE = 456
        private const val LINES_PER_FRAME = 154
        private const val DOTS_OAM_SEARCH = 80
        private const val DOTS_PIXEL_TRANSFER = 252  // 80 + 172
        private const val MAX_SPRITES_PER_LINE = 10

        private const val MODE_HBLANK = 0
        private const val MODE_VBLANK = 1
        private const val MODE_OAM_SEARCH = 2
        private const val MODE_PIXEL_TRANSFER = 3

        private const val VBLANK_INTERRUPT_BIT = 0
        private const val LCD_STAT_INTERRUPT_BIT = 1

        // I/O register addresses
        private val LCDC_ADDR = 0xFF40.toUShort()
        private val STAT_ADDR = 0xFF41.toUShort()
        private val SCY_ADDR = 0xFF42.toUShort()
        private val SCX_ADDR = 0xFF43.toUShort()
        private val LY_ADDR = 0xFF44.toUShort()
        private val LYC_ADDR = 0xFF45.toUShort()
        private val BGP_ADDR = 0xFF47.toUShort()
        private val OBP0_ADDR = 0xFF48.toUShort()
        private val OBP1_ADDR = 0xFF49.toUShort()
        private val WY_ADDR = 0xFF4A.toUShort()
        private val WX_ADDR = 0xFF4B.toUShort()

        // LCDC bits
        private const val LCDC_LCD_ENABLE = 0x80
        private const val LCDC_WINDOW_TILEMAP = 0x40
        private const val LCDC_WINDOW_ENABLE = 0x20
        private const val LCDC_TILEDATA_SELECT = 0x10
        private const val LCDC_BG_TILEMAP = 0x08
        private const val LCDC_OBJ_SIZE = 0x04
        private const val LCDC_OBJ_ENABLE = 0x02
        private const val LCDC_BG_ENABLE = 0x01

        // STAT bits
        private const val STAT_LYC_IRQ = 0x40
        private const val STAT_OAM_IRQ = 0x20
        private const val STAT_VBLANK_IRQ = 0x10
        private const val STAT_HBLANK_IRQ = 0x08
        private const val STAT_LYC_COINCIDENCE = 0x04

        // OBJ attribute bits
        private const val OBJ_ATTR_BG_PRIORITY = 0x80
        private const val OBJ_ATTR_FLIP_Y = 0x40
        private const val OBJ_ATTR_FLIP_X = 0x20
        private const val OBJ_ATTR_PALETTE = 0x10
    }
}
