package com.vicgarci.kgbem

import com.vicgarci.kgbem.cartridge.Cartridge
import com.vicgarci.kgbem.ppu.PPU
import com.vicgarci.kgbem.timer.Timer
import kotlin.test.Test
import kotlin.test.assertEquals

class GameBoyBusTest {

    private val romBytes = UByteArray(0x8000) { 0.toUByte() }
    private val cartridge = Cartridge(romBytes)
    private val ppu = PPU()
    private val timer = Timer()
    private val bus = GameBoyBus(cartridge, ppu, timer)

    @Test
    fun readRom_returnsCartridgeData() {
        // ROM bytes are all 0 by default
        assertEquals(0.toUByte(), bus.readByte(0x0000.toUShort()))
        assertEquals(0.toUByte(), bus.readByte(0x7FFF.toUShort()))
    }

    @Test
    fun writeAndReadWram() {
        bus.writeByte(0xC000.toUShort(), 0xAB.toUByte())
        assertEquals(0xAB.toUByte(), bus.readByte(0xC000.toUShort()))
    }

    @Test
    fun writeAndReadHram() {
        bus.writeByte(0xFF80.toUShort(), 0x55.toUByte())
        assertEquals(0x55.toUByte(), bus.readByte(0xFF80.toUShort()))
    }

    @Test
    fun writeAndReadIe() {
        bus.writeByte(0xFFFF.toUShort(), 0x1F.toUByte())
        assertEquals(0x1F.toUByte(), bus.readByte(0xFFFF.toUShort()))
    }

    @Test
    fun writeAndReadInterruptFlags() {
        bus.writeByte(0xFF0F.toUShort(), 0x03.toUByte())
        assertEquals(0x03.toUByte(), bus.readByte(0xFF0F.toUShort()))
    }

    @Test
    fun setInterruptFlagBit_setsCorrectBit() {
        bus.writeByte(0xFF0F.toUShort(), 0x00.toUByte())
        bus.setInterruptFlagBit(0, true)
        assertEquals(0x01.toUByte(), bus.readByte(0xFF0F.toUShort()))
    }

    @Test
    fun setInterruptEnableBit_setsCorrectBit() {
        bus.writeByte(0xFFFF.toUShort(), 0x00.toUByte())
        bus.setInterruptEnableBit(2, true)
        assertEquals(0x04.toUByte(), bus.readByte(0xFFFF.toUShort()))
    }

    @Test
    fun interruptPendingMask_bothFlagAndEnable() {
        bus.writeByte(0xFF0F.toUShort(), 0x01.toUByte())  // VBlank flag set
        bus.writeByte(0xFFFF.toUShort(), 0x01.toUByte())  // VBlank enable set
        assertEquals(0x01.toUByte(), bus.interruptPendingMask)
    }

    @Test
    fun anyInterruptPending_trueWhenBothSet() {
        bus.writeByte(0xFF0F.toUShort(), 0x04.toUByte())  // Timer flag
        bus.writeByte(0xFFFF.toUShort(), 0x04.toUByte())  // Timer enable
        assertEquals(true, bus.anyInterruptPending())
    }

    @Test
    fun anyInterruptPending_falseWhenOnlyFlagSet() {
        bus.writeByte(0xFF0F.toUShort(), 0x04.toUByte())  // Timer flag
        bus.writeByte(0xFFFF.toUShort(), 0x00.toUByte())  // No enable
        assertEquals(false, bus.anyInterruptPending())
    }

    @Test
    fun ppuLcdc_routedThroughBus() {
        bus.writeByte(0xFF40.toUShort(), 0x91.toUByte())
        assertEquals(0x91.toUByte(), ppu.lcdc)
        assertEquals(0x91.toUByte(), bus.readByte(0xFF40.toUShort()))
    }

    @Test
    fun timerDiv_readFromTimer() {
        // DIV register at 0xFF04
        val div = bus.readByte(0xFF04.toUShort())
        assertEquals(timer.div, div)
    }

    @Test
    fun timerDiv_writeResetsDiv() {
        bus.writeByte(0xFF04.toUShort(), 0xFF.toUByte())  // Any write resets DIV
        assertEquals(0.toUByte(), timer.div)
    }

    @Test
    fun wramEcho_readsFromWram() {
        bus.writeByte(0xC000.toUShort(), 0x42.toUByte())
        // Echo RAM at 0xE000 mirrors WRAM 0xC000
        assertEquals(0x42.toUByte(), bus.readByte(0xE000.toUShort()))
    }

    @Test
    fun vram_routedToPpu() {
        bus.writeByte(0x8000.toUShort(), 0xAA.toUByte())
        assertEquals(0xAA.toUByte(), ppu.readVram(0x8000.toUShort()))
        assertEquals(0xAA.toUByte(), bus.readByte(0x8000.toUShort()))
    }

    @Test
    fun oam_routedToPpu() {
        bus.writeByte(0xFE00.toUShort(), 0x55.toUByte())
        assertEquals(0x55.toUByte(), ppu.readOam(0xFE00.toUShort()))
        assertEquals(0x55.toUByte(), bus.readByte(0xFE00.toUShort()))
    }

    @Test
    fun collectInterrupts_movesVblankIrqToFlag() {
        ppu.vblankIrq = true
        bus.collectInterrupts()
        assertEquals(false, ppu.vblankIrq)
        // bit 0 (VBlank) should be set in IF
        val ifReg = bus.readByte(0xFF0F.toUShort())
        assertEquals(true, (ifReg.toInt() and 0x01) != 0)
    }

    @Test
    fun collectInterrupts_movesTimerIrqToFlag() {
        timer.timerIrq = true
        bus.collectInterrupts()
        assertEquals(false, timer.timerIrq)
        // bit 2 (Timer) should be set in IF
        val ifReg = bus.readByte(0xFF0F.toUShort())
        assertEquals(true, (ifReg.toInt() and 0x04) != 0)
    }
}
