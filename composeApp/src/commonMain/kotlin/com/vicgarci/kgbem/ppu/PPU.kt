package com.vicgarci.kgbem.ppu

import com.vicgarci.kgbem.cpu.MemoryBus

class PPU(private val memoryBus: MemoryBus) {

    val frameBuffer = IntArray(SCREEN_WIDTH * SCREEN_HEIGHT)

    var vBlankInterrupt = false
        private set
    var statInterrupt = false
        private set
    var frameComplete = false
        private set

    private var dots = 0
    private var ly = 0
    private var mode = MODE_OAM_SCAN

    fun tick(cycles: Int) {
        vBlankInterrupt = false
        statInterrupt = false
        frameComplete = false

        val lcdc = memoryBus.readByte(LCDC_ADDR).toInt()
        if (lcdc and 0x80 == 0) {
            // LCD off: hold LY at 0, stay in mode 0
            ly = 0
            dots = 0
            mode = MODE_HBLANK
            memoryBus.writeByte(LY_ADDR, 0u)
            updateStatMode()
            return
        }

        dots += cycles

        // Loop so large cycle counts (e.g. from test helpers) process all mode transitions
        var advanced = true
        while (advanced) {
            advanced = false
            when (mode) {
                MODE_OAM_SCAN -> {
                    if (dots >= OAM_DOTS) {
                        dots -= OAM_DOTS
                        mode = MODE_PIXEL_TRANSFER
                        updateStatMode()
                        advanced = true
                    }
                }
                MODE_PIXEL_TRANSFER -> {
                    if (dots >= TRANSFER_DOTS) {
                        dots -= TRANSFER_DOTS
                        renderScanline()
                        mode = MODE_HBLANK
                        checkStatInterrupt(STAT_HBLANK_INT)
                        updateStatMode()
                        advanced = true
                    }
                }
                MODE_HBLANK -> {
                    if (dots >= HBLANK_DOTS) {
                        dots -= HBLANK_DOTS
                        ly++
                        memoryBus.writeByte(LY_ADDR, ly.toUByte())
                        checkLycCoincidence()
                        if (ly == SCREEN_HEIGHT) {
                            mode = MODE_VBLANK
                            vBlankInterrupt = true
                            frameComplete = true
                            checkStatInterrupt(STAT_VBLANK_INT)
                        } else {
                            mode = MODE_OAM_SCAN
                            checkStatInterrupt(STAT_OAM_INT)
                        }
                        updateStatMode()
                        advanced = true
                    }
                }
                MODE_VBLANK -> {
                    if (dots >= DOTS_PER_LINE) {
                        dots -= DOTS_PER_LINE
                        ly++
                        memoryBus.writeByte(LY_ADDR, ly.toUByte())
                        checkLycCoincidence()
                        if (ly >= TOTAL_LINES) {
                            ly = 0
                            memoryBus.writeByte(LY_ADDR, 0u)
                            mode = MODE_OAM_SCAN
                            checkStatInterrupt(STAT_OAM_INT)
                        }
                        updateStatMode()
                        advanced = true
                    }
                }
            }
        }
    }

    private fun renderScanline() {
        if (ly >= SCREEN_HEIGHT) return
        val lcdc = memoryBus.readByte(LCDC_ADDR).toInt()

        if (lcdc and LCDC_BG_ENABLE == 0) {
            fillLine(ly, COLOR_WHITE)
            return
        }

        val scy = memoryBus.readByte(SCY_ADDR).toInt()
        val scx = memoryBus.readByte(SCX_ADDR).toInt()
        val bgp = memoryBus.readByte(BGP_ADDR).toInt()
        val tileDataSigned = lcdc and LCDC_TILE_DATA_SELECT == 0
        val tileMapBase = if (lcdc and LCDC_BG_MAP_SELECT != 0) TILE_MAP_1 else TILE_MAP_0

        val bgY = (ly + scy) and 0xFF
        val tileRow = bgY / 8
        val tileLine = bgY % 8

        for (x in 0 until SCREEN_WIDTH) {
            val bgX = (x + scx) and 0xFF
            val tileCol = bgX / 8
            val tilePixelBit = 7 - (bgX % 8)

            val tileMapAddr = tileMapBase + tileRow * 32 + tileCol
            val rawIndex = memoryBus.readByte(tileMapAddr.toUShort()).toInt()

            val tileBase = if (tileDataSigned) {
                0x9000 + rawIndex.toByte().toInt() * 16
            } else {
                0x8000 + rawIndex * 16
            }

            val lo = memoryBus.readByte((tileBase + tileLine * 2).toUShort()).toInt()
            val hi = memoryBus.readByte((tileBase + tileLine * 2 + 1).toUShort()).toInt()

            val colorIndex = ((hi shr tilePixelBit) and 1) shl 1 or ((lo shr tilePixelBit) and 1)
            val shade = (bgp shr (colorIndex * 2)) and 0x03

            frameBuffer[ly * SCREEN_WIDTH + x] = SHADES[shade]
        }
    }

    private fun fillLine(line: Int, color: Int) {
        for (x in 0 until SCREEN_WIDTH) frameBuffer[line * SCREEN_WIDTH + x] = color
    }

    private fun updateStatMode() {
        val stat = memoryBus.readByte(STAT_ADDR).toInt()
        memoryBus.writeByte(STAT_ADDR, ((stat and 0xF8) or mode).toUByte())
    }

    private fun checkLycCoincidence() {
        val lyc = memoryBus.readByte(LYC_ADDR).toInt()
        val stat = memoryBus.readByte(STAT_ADDR).toInt()
        val coincidence = ly == lyc
        val newStat = if (coincidence) stat or 0x04 else stat and 0x04.inv()
        memoryBus.writeByte(STAT_ADDR, newStat.toUByte())
        if (coincidence && stat and STAT_LYC_INT != 0) statInterrupt = true
    }

    private fun checkStatInterrupt(bitMask: Int) {
        val stat = memoryBus.readByte(STAT_ADDR).toInt()
        if (stat and bitMask != 0) statInterrupt = true
    }

    companion object {
        const val SCREEN_WIDTH = 160
        const val SCREEN_HEIGHT = 144
        private const val TOTAL_LINES = 154
        private const val DOTS_PER_LINE = 456
        private const val OAM_DOTS = 80
        private const val TRANSFER_DOTS = 172
        private const val HBLANK_DOTS = 204

        private const val MODE_HBLANK = 0
        private const val MODE_VBLANK = 1
        private const val MODE_OAM_SCAN = 2
        private const val MODE_PIXEL_TRANSFER = 3

        private const val LCDC_BG_ENABLE = 0x01
        private const val LCDC_BG_MAP_SELECT = 0x08
        private const val LCDC_TILE_DATA_SELECT = 0x10

        private const val STAT_HBLANK_INT = 0x08
        private const val STAT_VBLANK_INT = 0x10
        private const val STAT_OAM_INT = 0x20
        private const val STAT_LYC_INT = 0x40

        private const val TILE_MAP_0 = 0x9800
        private const val TILE_MAP_1 = 0x9C00

        val LCDC_ADDR = 0xFF40.toUShort()
        val STAT_ADDR = 0xFF41.toUShort()
        val SCY_ADDR = 0xFF42.toUShort()
        val SCX_ADDR = 0xFF43.toUShort()
        val LY_ADDR = 0xFF44.toUShort()
        val LYC_ADDR = 0xFF45.toUShort()
        val BGP_ADDR = 0xFF47.toUShort()

        private val COLOR_WHITE = 0xFFFFFFFF.toInt()

        val SHADES = intArrayOf(
            0xFFFFFFFF.toInt(), // 0: white
            0xFFAAAAAA.toInt(), // 1: light gray
            0xFF555555.toInt(), // 2: dark gray
            0xFF000000.toInt(), // 3: black
        )
    }
}
