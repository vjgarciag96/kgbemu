package com.vicgarci.kgbem.cartridge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class RomOnlyCartridgeTest {

    private val romData = ByteArray(0x8000) { it.toByte() }
    private val cartridge = RomOnlyCartridge(romData)

    @Test
    fun readsCorrectByteAtAddress0x0000() {
        assertEquals(0x00, cartridge.readRom(0x0000))
    }

    @Test
    fun readsCorrectByteAtAddress0x7FFF() {
        // 0x7FFF = 32767, as a byte: 32767 % 256 = 255 = 0xFF
        assertEquals(0xFF, cartridge.readRom(0x7FFF))
    }

    @Test
    fun readsCorrectByteAtAddress0x4000() {
        // 0x4000 = 16384, as a byte: 16384 % 256 = 0
        assertEquals(0x00, cartridge.readRom(0x4000))
    }

    @Test
    fun readsCorrectByteAtMidAddress() {
        // 0x0080 = 128, as a byte: 128 => -128 signed, but masked to 0x80 = 128
        assertEquals(0x80, cartridge.readRom(0x0080))
    }

    @Test
    fun readRomMasksByteTo0xFF() {
        // Address 0xFF = 255, toByte() = -1, and 0xFF masked = 255
        assertEquals(0xFF, cartridge.readRom(0x00FF))
    }

    @Test
    fun writeRomIsNoOp() {
        cartridge.writeRom(0x0000, 0xFF)
        assertEquals(romData[0].toInt() and 0xFF, cartridge.readRom(0x0000))
    }

    @Test
    fun writeRomAtBankedAreaIsNoOp() {
        val originalValue = cartridge.readRom(0x4000)
        cartridge.writeRom(0x4000, 0xAB)
        assertEquals(originalValue, cartridge.readRom(0x4000))
    }

    @Test
    fun readRamReturns0xFF() {
        assertEquals(0xFF, cartridge.readRam(0xA000))
    }

    @Test
    fun readRamAtEndOfRangeReturns0xFF() {
        assertEquals(0xFF, cartridge.readRam(0xBFFF))
    }

    @Test
    fun writeRamIsNoOp() {
        cartridge.writeRam(0xA000, 0x42)
        assertEquals(0xFF, cartridge.readRam(0xA000))
    }

    @Test
    fun hasBatteryReturnsFalse() {
        assertFalse(cartridge.hasBattery())
    }

    @Test
    fun savableStateReturnsNull() {
        assertNull(cartridge.savableState())
    }

    @Test
    fun loadStateIsNoOp() {
        // Should not throw
        cartridge.loadState(byteArrayOf(0x01, 0x02, 0x03))
    }

    @Test
    fun romDataIsDefensivelyCopied() {
        val original = ByteArray(0x8000) { 0x42.toByte() }
        val cart = RomOnlyCartridge(original)
        original[0] = 0x00.toByte()
        assertEquals(0x42, cart.readRom(0x0000))
    }
}
