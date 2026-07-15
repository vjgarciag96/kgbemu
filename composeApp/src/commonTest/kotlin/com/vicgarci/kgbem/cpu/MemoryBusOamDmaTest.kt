package com.vicgarci.kgbem.cpu

import com.vicgarci.kgbem.cartridge.RomOnlyCartridge
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MemoryBusOamDmaTest {

    private fun createBus(): MemoryBus {
        val rom = ByteArray(0x8000)
        return MemoryBus(RomOnlyCartridge(rom))
    }

    @Test
    fun writeTo0xFF46_activatesDma_and_completesAfter640Cycles() {
        val bus = createBus()
        // Seed WRAM at 0xC000 with a known value
        bus.writeByte(0xC000.toUShort(), 0xAB.toUByte())

        // Trigger DMA from page 0xC0
        bus.writeByte(0xFF46.toUShort(), 0xC0.toUByte())

        assertTrue(bus.dmaActive, "DMA should be active after writing to 0xFF46")
        assertEquals(640, bus.dmaRemainingCycles)

        // Step 639 cycles — should still be active
        bus.advanceDma(639)
        assertTrue(bus.dmaActive, "DMA should still be active after 639 T-cycles")
        assertEquals(1, bus.dmaRemainingCycles)

        // Step 1 more — should complete
        bus.advanceDma(1)
        assertFalse(bus.dmaActive, "DMA should be inactive after 640 T-cycles")
        assertEquals(0, bus.dmaRemainingCycles)
    }

    @Test
    fun dmaCompletesWithCorrectDataInOam() {
        val bus = createBus()
        // Seed source data at 0xC000
        bus.writeByte(0xC000.toUShort(), 0x42.toUByte())
        bus.writeByte(0xC001.toUShort(), 0x7F.toUByte())
        bus.writeByte(0xC09F.toUShort(), 0xEE.toUByte())

        // Trigger DMA
        bus.writeByte(0xFF46.toUShort(), 0xC0.toUByte())

        // Complete the DMA
        bus.advanceDma(640)

        // Verify OAM contents
        assertEquals(
            0x42.toUByte(),
            bus.readByte(0xFE00.toUShort()),
            "OAM[0] should match source 0xC000",
        )
        assertEquals(
            0x7F.toUByte(),
            bus.readByte(0xFE01.toUShort()),
            "OAM[1] should match source 0xC001",
        )
        assertEquals(
            0xEE.toUByte(),
            bus.readByte(0xFE9F.toUShort()),
            "OAM[159] should match source 0xC09F",
        )
    }

    @Test
    fun duringDma_readOutsideHram_returns0xFF() {
        val bus = createBus()
        // Write a known value at 0x8000 (VRAM range)
        bus.writeByte(0x8000.toUShort(), 0x55.toUByte())

        // Trigger DMA
        bus.writeByte(0xFF46.toUShort(), 0xC0.toUByte())

        // Read from VRAM while DMA is active — should return 0xFF
        assertEquals(
            0xFF.toUByte(),
            bus.readByte(0x8000.toUShort()),
            "Read outside HRAM during DMA should return 0xFF",
        )
    }

    @Test
    fun duringDma_readFromHram_returnsActualValue() {
        val bus = createBus()
        // Write a known value into HRAM
        bus.writeByte(0xFF80.toUShort(), 0x33.toUByte())

        // Trigger DMA
        bus.writeByte(0xFF46.toUShort(), 0xC0.toUByte())

        // Read from HRAM while DMA active — should return actual value
        assertEquals(
            0x33.toUByte(),
            bus.readByte(0xFF80.toUShort()),
            "HRAM should remain accessible during DMA",
        )
    }

    @Test
    fun duringDma_writeOutsideHram_isIgnored() {
        val bus = createBus()
        bus.writeByte(0xC100.toUShort(), 0x11.toUByte())

        // Trigger DMA
        bus.writeByte(0xFF46.toUShort(), 0xC0.toUByte())

        // Attempt write outside HRAM
        bus.writeByte(0xC100.toUShort(), 0x99.toUByte())

        // Complete DMA
        bus.advanceDma(640)

        // Value should be unchanged (the write during DMA was ignored)
        assertEquals(
            0x11.toUByte(),
            bus.readByte(0xC100.toUShort()),
            "Write outside HRAM during DMA should be ignored",
        )
    }

    @Test
    fun duringDma_writeToHram_succeeds() {
        val bus = createBus()

        // Trigger DMA
        bus.writeByte(0xFF46.toUShort(), 0xC0.toUByte())

        // Write to HRAM during DMA
        bus.writeByte(0xFF90.toUShort(), 0xBB.toUByte())

        // Should be readable from HRAM
        assertEquals(
            0xBB.toUByte(),
            bus.readByte(0xFF90.toUShort()),
            "HRAM writes should succeed during DMA",
        )
    }

    @Test
    fun dmaFromRomRange_copiesCartridgeData() {
        val rom = ByteArray(0x8000)
        rom[0x0000] = 0xDE.toByte()
        rom[0x009F] = 0xAD.toByte()
        val bus = MemoryBus(RomOnlyCartridge(rom))

        // Trigger DMA from page 0x00 (ROM)
        bus.writeByte(0xFF46.toUShort(), 0x00.toUByte())
        bus.advanceDma(640)

        assertEquals(
            0xDE.toUByte(),
            bus.readByte(0xFE00.toUShort()),
            "OAM[0] should contain ROM data from 0x0000",
        )
        assertEquals(
            0xAD.toUByte(),
            bus.readByte(0xFE9F.toUShort()),
            "OAM[159] should contain ROM data from 0x009F",
        )
    }
}
