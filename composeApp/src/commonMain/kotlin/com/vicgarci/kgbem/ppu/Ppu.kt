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

        const val STAT_ADDRESS: UShort = 0xFF41u
        const val LY_ADDRESS: UShort = 0xFF44u
        const val LYC_ADDRESS: UShort = 0xFF45u
        const val IF_ADDRESS: UShort = 0xFF0Fu
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
                        // Copy back buffer to front buffer and emit frame
                        backBuffer.copyInto(frontBuffer)
                        frameSink.onFrame(frontBuffer)
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
