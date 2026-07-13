package com.vicgarci.kgbem.cpu

import com.vicgarci.kgbem.cartridge.RomOnlyCartridge
import kotlin.test.Test
import kotlin.test.assertEquals

class MemoryBusTest {

    @Test
    fun readByte_inRomRange_delegatesToCartridge() {
        val rom = ByteArray(0x8000)
        rom[0x0100] = 0x42.toByte()
        val bus = MemoryBus(RomOnlyCartridge(rom))
        assertEquals(0x42.toUByte(), bus.readByte(0x0100.toUShort()))
    }

    @Test
    fun writeByte_inRomRange_isNoOpForRomOnly() {
        val rom = ByteArray(0x8000) { 0xAB.toByte() }
        val bus = MemoryBus(RomOnlyCartridge(rom))
        bus.writeByte(0x0000.toUShort(), 0x00.toUByte())
        // RomOnlyCartridge ignores writes — value should still be 0xAB
        assertEquals(0xAB.toUByte(), bus.readByte(0x0000.toUShort()))
    }

    @Test
    fun readByte_outsideCartridgeRange_readsFromInternalMemory() {
        val bus = MemoryBus(RomOnlyCartridge(ByteArray(0x8000)))
        bus.writeByte(0xC000.toUShort(), 0x55.toUByte())
        assertEquals(0x55.toUByte(), bus.readByte(0xC000.toUShort()))
    }

    @Test
    fun readByte_atRomUpperBoundary_delegatesToCartridge() {
        val rom = ByteArray(0x8000)
        rom[0x7FFF] = 0x99.toByte()
        val bus = MemoryBus(RomOnlyCartridge(rom))
        assertEquals(0x99.toUByte(), bus.readByte(0x7FFF.toUShort()))
    }
}
