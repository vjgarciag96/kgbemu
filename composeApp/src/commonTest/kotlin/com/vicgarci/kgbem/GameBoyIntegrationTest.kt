package com.vicgarci.kgbem

import com.vicgarci.kgbem.ppu.PPU
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameBoyIntegrationTest {

    private fun minimalRom(): UByteArray {
        // 32KB ROM with an infinite JP loop at 0x0100 (post-boot PC)
        val rom = UByteArray(0x8000) { 0x00.toUByte() }
        rom[0x0100] = 0xC3.toUByte() // JP nn
        rom[0x0101] = 0x00.toUByte() // lo: 0x0100
        rom[0x0102] = 0x01.toUByte() // hi: 0x0100
        return rom
    }

    @Test
    fun runFrame_withMinimalRom_doesNotCrash() {
        val gameBoy = GameBoy(minimalRom())
        gameBoy.runFrame()
    }

    @Test
    fun frameBuffer_hasCorrectSize() {
        val gameBoy = GameBoy(minimalRom())
        assertEquals(160 * 144, gameBoy.frameBuffer.size)
    }

    @Test
    fun frameBuffer_containsOnlyValidPaletteColors() {
        val gameBoy = GameBoy(minimalRom())
        gameBoy.runFrame()
        assertTrue(gameBoy.frameBuffer.all { it in PPU.GB_PALETTE })
    }

    @Test
    fun runMultipleFrames_remainsStable() {
        val gameBoy = GameBoy(minimalRom())
        repeat(10) { gameBoy.runFrame() }
        assertTrue(gameBoy.frameBuffer.all { it in PPU.GB_PALETTE })
    }
}
