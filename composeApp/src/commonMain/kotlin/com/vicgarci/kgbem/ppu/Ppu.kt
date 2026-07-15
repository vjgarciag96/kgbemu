package com.vicgarci.kgbem.ppu

import com.vicgarci.kgbem.cpu.MemoryBus

class Ppu(
    private val bus: MemoryBus,
    private val frameSink: FrameSink,
) {
    private companion object {
        const val OAM_SEARCH_CYCLES = 80
        const val DRAWING_CYCLES = 172
        const val HBLANK_CYCLES = 204
        const val SCANLINE_CYCLES = 456
        const val VISIBLE_SCANLINES = 144
        const val TOTAL_SCANLINES = 154
        const val SCREEN_WIDTH = 160
        const val SCREEN_HEIGHT = 144

        const val LCDC_ADDRESS: UShort = 0xFF40u
        const val STAT_ADDRESS: UShort = 0xFF41u
        const val SCY_ADDRESS: UShort = 0xFF42u
        const val SCX_ADDRESS: UShort = 0xFF43u
        const val LY_ADDRESS: UShort = 0xFF44u
        const val LYC_ADDRESS: UShort = 0xFF45u
        const val BGP_ADDRESS: UShort = 0xFF47u
        const val WY_ADDRESS: UShort = 0xFF4Au
        const val WX_ADDRESS: UShort = 0xFF4Bu
        const val IF_ADDRESS: UShort = 0xFF0Fu

        const val WHITE = 0xFFFFFFFF.toInt()

        /** ARGB shade table indexed by 2-bit colour ID. */
        val SHADE_TABLE = intArrayOf(
            0xFFFFFFFF.toInt(), // 0 = white
            0xFFAAAAAA.toInt(), // 1 = light grey
            0xFF555555.toInt(), // 2 = dark grey
            0xFF000000.toInt(), // 3 = black
        )
    }

    private enum class Mode(val bits: Int) {
        HBLANK(0),
        VBLANK(1),
        OAM_SEARCH(2),
        DRAWING(3),
    }

    private var mode: Mode = Mode.OAM_SEARCH
    private var scanline: Int = 0
    private var cycleClock: Int = 0

    private var windowLineCounter: Int = 0

    private val backBuffer = IntArray(SCREEN_WIDTH * SCREEN_HEIGHT)
    private val frontBuffer = IntArray(SCREEN_WIDTH * SCREEN_HEIGHT)

    init {
        updateStatMode()
        updateLy()
    }

    fun step(cycles: Int) {
        repeat(cycles) {
            tickOneCycle()
        }
    }

    private fun tickOneCycle() {
        cycleClock++

        when (mode) {
            Mode.OAM_SEARCH -> {
                if (cycleClock >= OAM_SEARCH_CYCLES) {
                    cycleClock -= OAM_SEARCH_CYCLES
                    setMode(Mode.DRAWING)
                }
            }
            Mode.DRAWING -> {
                if (cycleClock >= DRAWING_CYCLES) {
                    cycleClock -= DRAWING_CYCLES
                    setMode(Mode.HBLANK)
                }
            }
            Mode.HBLANK -> {
                if (cycleClock >= HBLANK_CYCLES) {
                    cycleClock -= HBLANK_CYCLES
                    scanline++
                    updateLy()

                    if (scanline >= VISIBLE_SCANLINES) {
                        setMode(Mode.VBLANK)
                        // Fire VBlank interrupt
                        val ifValue = bus.readByte(IF_ADDRESS).toInt()
                        bus.writeByte(IF_ADDRESS, (ifValue or 0x01).toUByte())
                        swapBuffers()
                        checkLycCoincidence()
                    } else {
                        setMode(Mode.OAM_SEARCH)
                        checkLycCoincidence()
                    }
                }
            }
            Mode.VBLANK -> {
                if (cycleClock >= SCANLINE_CYCLES) {
                    cycleClock -= SCANLINE_CYCLES
                    scanline++

                    if (scanline >= TOTAL_SCANLINES) {
                        scanline = 0
                        windowLineCounter = 0
                        updateLy()
                        setMode(Mode.OAM_SEARCH)
                        checkLycCoincidence()
                    } else {
                        updateLy()
                        checkLycCoincidence()
                    }
                }
            }
        }
    }

    private fun setMode(newMode: Mode) {
        mode = newMode
        updateStatMode()
        if (newMode == Mode.DRAWING) {
            renderScanline()
        }
    }

    private fun updateStatMode() {
        val stat = bus.readByte(STAT_ADDRESS).toInt()
        // Preserve upper bits, replace lower 2 with current mode
        val result = (stat and 0xFC) or mode.bits
        bus.writeByte(STAT_ADDRESS, result.toUByte())
    }

    private fun updateLy() {
        bus.writeByte(LY_ADDRESS, scanline.toUByte())
    }

    private fun swapBuffers() {
        val lcdc = bus.readByte(LCDC_ADDRESS).toInt()
        if (lcdc and 0x80 == 0) {
            // LCD off: output all white
            backBuffer.fill(WHITE)
        }
        backBuffer.copyInto(frontBuffer)
        frameSink.onFrame(frontBuffer)
    }

    private fun renderScanline() {
        val lcdc = bus.readByte(LCDC_ADDRESS).toInt()
        // Only render when LCD is on (bit 7)
        if (lcdc and 0x80 == 0) return

        val bgp = bus.readByte(BGP_ADDRESS).toInt()
        val scy = bus.readByte(SCY_ADDRESS).toInt()
        val scx = bus.readByte(SCX_ADDRESS).toInt()

        // Determine tile map base address from LCDC bit 3
        val tileMapBase = if (lcdc and 0x08 != 0) 0x9C00 else 0x9800

        // Determine tile data addressing mode from LCDC bit 4
        val unsignedAddressing = lcdc and 0x10 != 0

        val y = scanline
        // Y position in the 256x256 background space (wraps)
        val bgY = (scy + y) and 0xFF
        val tileRow = bgY / 8
        val tileYOffset = bgY % 8

        val lineOffset = y * SCREEN_WIDTH

        for (x in 0 until SCREEN_WIDTH) {
            // X position in the 256x256 background space (wraps)
            val bgX = (scx + x) and 0xFF
            val tileCol = bgX / 8
            val tileXBit = 7 - (bgX % 8)

            // Fetch tile index from tile map
            val tileMapAddr = tileMapBase + tileRow * 32 + tileCol
            val tileIndex = bus.readByte(tileMapAddr.toUShort()).toInt()

            // Compute tile data address
            val tileDataAddr = if (unsignedAddressing) {
                // 0x8000 + tileIndex * 16
                0x8000 + tileIndex * 16
            } else {
                // 0x8800 base, signed indexing: tile 0 is at 0x9000
                0x9000 + tileIndex.toByte().toInt() * 16
            }

            // Read the two bytes for this row of the tile
            val rowAddr = tileDataAddr + tileYOffset * 2
            val lowByte = bus.readByte(rowAddr.toUShort()).toInt()
            val highByte = bus.readByte((rowAddr + 1).toUShort()).toInt()

            // Extract 2-bit colour ID
            val colourBit0 = (lowByte shr tileXBit) and 1
            val colourBit1 = (highByte shr tileXBit) and 1
            val colourId = (colourBit1 shl 1) or colourBit0

            // Apply BGP palette
            val shade = (bgp shr (colourId * 2)) and 0x03

            backBuffer[lineOffset + x] = SHADE_TABLE[shade]
        }

        // --- Window layer ---
        val windowEnabled = lcdc and 0x20 != 0 // LCDC bit 5
        if (!windowEnabled) return

        val wy = bus.readByte(WY_ADDRESS).toInt()
        val wx = bus.readByte(WX_ADDRESS).toInt()

        // Window only renders if current scanline >= WY
        if (y < wy) return

        val windowStartX = wx - 7
        // If entire window is off-screen to the right, skip
        if (windowStartX >= SCREEN_WIDTH) return

        // Window tile map from LCDC bit 6 (separate from BG tile map bit 3)
        val winTileMapBase = if (lcdc and 0x40 != 0) 0x9C00 else 0x9800

        // Window uses same tile data addressing as background (LCDC bit 4)
        val winTileRow = windowLineCounter / 8
        val winTileYOffset = windowLineCounter % 8

        for (x in 0 until SCREEN_WIDTH) {
            if (x < windowStartX) continue

            val winX = x - windowStartX
            val winTileCol = winX / 8
            val winTileXBit = 7 - (winX % 8)

            val winTileMapAddr = winTileMapBase + winTileRow * 32 + winTileCol
            val winTileIndex = bus.readByte(winTileMapAddr.toUShort()).toInt()

            val winTileDataAddr = if (unsignedAddressing) {
                0x8000 + winTileIndex * 16
            } else {
                0x9000 + winTileIndex.toByte().toInt() * 16
            }

            val winRowAddr = winTileDataAddr + winTileYOffset * 2
            val winLowByte = bus.readByte(winRowAddr.toUShort()).toInt()
            val winHighByte = bus.readByte((winRowAddr + 1).toUShort()).toInt()

            val winColourBit0 = (winLowByte shr winTileXBit) and 1
            val winColourBit1 = (winHighByte shr winTileXBit) and 1
            val winColourId = (winColourBit1 shl 1) or winColourBit0

            val winShade = (bgp shr (winColourId * 2)) and 0x03
            backBuffer[lineOffset + x] = SHADE_TABLE[winShade]
        }

        windowLineCounter++
    }

    private fun checkLycCoincidence() {
        val ly = scanline
        val lyc = bus.readByte(LYC_ADDRESS).toInt()
        val stat = bus.readByte(STAT_ADDRESS).toInt()

        if (ly == lyc) {
            // Set coincidence flag (bit 2)
            val newStat = stat or 0x04
            bus.writeByte(STAT_ADDRESS, newStat.toUByte())

            // If STAT bit 6 (LYC=LY interrupt enable) is set, fire LCD STAT interrupt
            if (stat and 0x40 != 0) {
                val ifValue = bus.readByte(IF_ADDRESS).toInt()
                bus.writeByte(IF_ADDRESS, (ifValue or 0x02).toUByte())
            }
        } else {
            // Clear coincidence flag (bit 2)
            val newStat = stat and 0x04.inv()
            bus.writeByte(STAT_ADDRESS, newStat.toUByte())
        }
    }
}
