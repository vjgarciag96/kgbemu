package com.vicgarci.kgbem.ppu

class PPU {

    // VRAM: 8192 bytes (0x8000–0x9FFF)
    private val vram = UByteArray(8192)

    // OAM: 160 bytes (0xFE00–0xFE9F)
    private val oam = UByteArray(160)

    // LCD Registers
    var lcdc: UByte = 0x91u
    var stat: UByte = 0x85u
    var scy: UByte = 0u
    var scx: UByte = 0u
    var ly: UByte = 0u
    var lyc: UByte = 0u
    var bgp: UByte = 0xFCu
    var obp0: UByte = 0xFFu
    var obp1: UByte = 0xFFu
    var wy: UByte = 0u
    var wx: UByte = 0u

    // Interrupt flags
    var vblankIrq: Boolean = false
    var statIrq: Boolean = false

    // Mode state
    private var mode: Int = 2  // 0=HBlank, 1=VBlank, 2=OAM Scan, 3=Drawing
    private var dots: Int = 0
    private var windowLineCounter: Int = 0

    // Frame buffer (ARGB format)
    val frameBuffer = IntArray(160 * 144) { GB_PALETTE[0] }

    companion object {
        val GB_PALETTE = intArrayOf(
            0xFFE0F8D0.toInt(),
            0xFF88C070.toInt(),
            0xFF346856.toInt(),
            0xFF081820.toInt()
        )
    }

    private fun applyPalette(palette: UByte, colorIndex: Int): Int {
        val paletteShift = (palette.toInt() shr (colorIndex * 2)) and 3
        return GB_PALETTE[paletteShift]
    }

    fun step(cycles: Int) {
        // If LCD is disabled, do nothing
        if ((lcdc.toInt() and 0x80) == 0) return

        dots += cycles

        when (mode) {
            2 -> { // OAM Scan
                if (dots >= 80) {
                    dots -= 80
                    setMode(3)
                }
            }
            3 -> { // Drawing
                if (dots >= 172) {
                    dots -= 172
                    renderScanline()
                    setMode(0)
                    // Fire STAT interrupt if Mode 0 interrupt enabled
                    if ((stat.toInt() and 0x08) != 0) {
                        statIrq = true
                    }
                }
            }
            0 -> { // HBlank
                if (dots >= 204) {
                    dots -= 204
                    ly = (ly.toInt() + 1).toUByte()
                    checkLycMatch()
                    if (ly.toInt() == 144) {
                        setMode(1)
                        vblankIrq = true
                        // Fire STAT interrupt if Mode 1 interrupt enabled
                        if ((stat.toInt() and 0x10) != 0) {
                            statIrq = true
                        }
                    } else {
                        setMode(2)
                        // Fire STAT interrupt if Mode 2 interrupt enabled
                        if ((stat.toInt() and 0x20) != 0) {
                            statIrq = true
                        }
                    }
                }
            }
            1 -> { // VBlank
                if (dots >= 456) {
                    dots -= 456
                    ly = (ly.toInt() + 1).toUByte()
                    checkLycMatch()
                    if (ly.toInt() >= 154) {
                        ly = 0u
                        windowLineCounter = 0
                        checkLycMatch()
                        setMode(2)
                        // Fire STAT interrupt if Mode 2 interrupt enabled
                        if ((stat.toInt() and 0x20) != 0) {
                            statIrq = true
                        }
                    }
                }
            }
        }
    }

    private fun checkLycMatch() {
        if (ly == lyc) {
            // Set bit 2
            stat = (stat.toInt() or 0x04).toUByte()
            // Fire STAT interrupt if LYC=LY interrupt enabled
            if ((stat.toInt() and 0x40) != 0) {
                statIrq = true
            }
        } else {
            // Clear bit 2
            stat = (stat.toInt() and 0x04.inv()).toUByte()
        }
    }

    private fun setMode(newMode: Int) {
        mode = newMode
        // Update bits 1-0 of STAT
        stat = ((stat.toInt() and 0xFC) or (newMode and 0x03)).toUByte()
    }

    private fun renderScanline() {
        val scanline = ly.toInt()
        if (scanline >= 144) return

        // Clear this scanline to default color
        frameBuffer.fill(GB_PALETTE[0], scanline * 160, (scanline + 1) * 160)

        if ((lcdc.toInt() and 0x01) != 0) {
            renderBackground(scanline)
            renderWindow(scanline)
        }
        if ((lcdc.toInt() and 0x02) != 0) {
            renderSprites(scanline)
        }
    }

    private fun renderBackground(scanline: Int) {
        val scrolledY = (scanline + scy.toInt()) and 0xFF
        val tileRow = scrolledY / 8
        val tilePixelY = scrolledY % 8
        val mapBase = if ((lcdc.toInt() and 0x08) != 0) 0x9C00 else 0x9800

        for (screenX in 0 until 160) {
            val scrolledX = (screenX + scx.toInt()) and 0xFF
            val tileCol = scrolledX / 8
            val tilePixelX = scrolledX % 8
            val tileIndex = vram[(mapBase - 0x8000) + tileRow * 32 + tileCol].toInt()
            val tileDataBase = getTileDataBase(tileIndex)
            val colorIndex = getTilePixelColor(tileDataBase, tilePixelX, tilePixelY)
            frameBuffer[scanline * 160 + screenX] = applyPalette(bgp, colorIndex)
        }
    }

    private fun getTileDataBase(tileIndex: Int): Int {
        return if ((lcdc.toInt() and 0x10) == 0) {
            // Signed addressing: base 0x9000 - 0x8000 = 0x1000
            val base = 0x1000
            val signed = tileIndex.toByte().toInt()
            base + signed * 16
        } else {
            // Unsigned addressing
            tileIndex * 16
        }
    }

    private fun renderWindow(scanline: Int) {
        if ((lcdc.toInt() and 0x20) == 0) return
        val wyInt = wy.toInt()
        if (scanline < wyInt) return
        val wxInt = wx.toInt() - 7
        if (wxInt >= 160 || wyInt >= 144) return

        val windowLine = windowLineCounter
        val tileRow = windowLine / 8
        val tilePixelY = windowLine % 8
        windowLineCounter++

        val mapBase = if ((lcdc.toInt() and 0x40) != 0) 0x9C00 else 0x9800

        for (screenX in maxOf(0, wxInt) until 160) {
            val windowX = screenX - wxInt
            val tileCol = windowX / 8
            val tilePixelX = windowX % 8
            val tileIndex = vram[(mapBase - 0x8000) + tileRow * 32 + tileCol].toInt()
            val tileDataBase = getTileDataBase(tileIndex)
            val colorIndex = getTilePixelColor(tileDataBase, tilePixelX, tilePixelY)
            frameBuffer[scanline * 160 + screenX] = applyPalette(bgp, colorIndex)
        }
    }

    private fun renderSprites(scanline: Int) {
        val spriteHeight = if ((lcdc.toInt() and 0x04) != 0) 16 else 8
        var count = 0

        for (i in 0 until 40) {
            if (count >= 10) break
            val oamBase = i * 4
            val sprY = oam[oamBase].toInt() - 16
            val sprX = oam[oamBase + 1].toInt() - 8
            val tileIdx = oam[oamBase + 2].toInt() and (if (spriteHeight == 16) 0xFE else 0xFF)
            val attr = oam[oamBase + 3].toInt()

            if (scanline < sprY || scanline >= sprY + spriteHeight) continue
            if (sprX < -7 || sprX >= 160) continue
            count++

            val bgPrio = (attr and 0x80) != 0
            val yFlip = (attr and 0x40) != 0
            val xFlip = (attr and 0x20) != 0
            val palette = if ((attr and 0x10) != 0) obp1 else obp0

            var tileRow = scanline - sprY
            if (yFlip) tileRow = spriteHeight - 1 - tileRow

            val tileDataBase = tileIdx * 16

            for (tilePixelX in 0 until 8) {
                val screenX = sprX + tilePixelX
                if (screenX < 0 || screenX >= 160) continue
                val pixelX = if (xFlip) 7 - tilePixelX else tilePixelX
                val colorIndex = getTilePixelColor(tileDataBase, pixelX, tileRow)
                if (colorIndex == 0) continue  // transparent
                if (bgPrio && frameBuffer[scanline * 160 + screenX] != GB_PALETTE[0]) continue
                frameBuffer[scanline * 160 + screenX] = applyPalette(palette, colorIndex)
            }
        }
    }

    private fun getTilePixelColor(tileDataBase: Int, pixelX: Int, pixelY: Int): Int {
        val lo = vram[tileDataBase + pixelY * 2].toInt()
        val hi = vram[tileDataBase + pixelY * 2 + 1].toInt()
        val bit = 7 - pixelX
        return ((hi shr bit) and 1) shl 1 or ((lo shr bit) and 1)
    }

    // Public memory access methods
    fun readVram(address: UShort): UByte = vram[(address - 0x8000u).toInt()]
    fun writeVram(address: UShort, value: UByte) { vram[(address - 0x8000u).toInt()] = value }
    fun readOam(address: UShort): UByte = oam[(address - 0xFE00u).toInt()]
    fun writeOam(address: UShort, value: UByte) { oam[(address - 0xFE00u).toInt()] = value }
    fun resetLy() { ly = 0u }
}
