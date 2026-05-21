package com.vicgarci.kgbem

import com.vicgarci.kgbem.cartridge.Cartridge
import com.vicgarci.kgbem.ppu.PPU
import com.vicgarci.kgbem.timer.Timer
import kotlin.test.Test
import kotlin.test.assertEquals

class GameBoyBusTest {

    private val romBytes = UByteArray(0x8000) { 0u }
    private val cartridge = Cartridge(romBytes)
    private val ppu = PPU()
    private val timer = Timer()
    private val bus = GameBoyBus(cartridge, ppu, timer)

    @Test
    fun readRom_returnsCartridgeData() {
        // ROM bytes are all 0 by default
        assertEquals(0u.toUByte(), bus.readByte(0x0000u))
        assertEquals(0u.toUByte(), bus.readByte(0x7FFFu))
    }

    @Test
    fun writeAndReadWram() {
        bus.writeByte(0xC000u, 0xABu)
        assertEquals(0xABu.toUByte(), bus.readByte(0xC000u))
    }

    @Test
    fun writeAndReadHram() {
        bus.writeByte(0xFF80u, 0x55u)
        assertEquals(0x55u.toUByte(), bus.readByte(0xFF80u))
    }

    @Test
    fun writeAndReadIe() {
        bus.writeByte(0xFFFFu, 0x1Fu)
        assertEquals(0x1Fu.toUByte(), bus.readByte(0xFFFFu))
    }

    @Test
    fun writeAndReadInterruptFlags() {
        bus.writeByte(0xFF0Fu, 0x03u)
        assertEquals(0x03u.toUByte(), bus.readByte(0xFF0Fu))
    }

    @Test
    fun setInterruptFlagBit_setsCorrectBit() {
        bus.writeByte(0xFF0Fu, 0x00u)
        bus.setInterruptFlagBit(0, true)
        assertEquals(0x01u.toUByte(), bus.readByte(0xFF0Fu))
    }

    @Test
    fun setInterruptEnableBit_setsCorrectBit() {
        bus.writeByte(0xFFFFu, 0x00u)
        bus.setInterruptEnableBit(2, true)
        assertEquals(0x04u.toUByte(), bus.readByte(0xFFFFu))
    }

    @Test
    fun interruptPendingMask_bothFlagAndEnable() {
        bus.writeByte(0xFF0Fu, 0x01u)  // VBlank flag set
        bus.writeByte(0xFFFFu, 0x01u)  // VBlank enable set
        assertEquals(0x01u.toUByte(), bus.interruptPendingMask)
    }

    @Test
    fun anyInterruptPending_trueWhenBothSet() {
        bus.writeByte(0xFF0Fu, 0x04u)  // Timer flag
        bus.writeByte(0xFFFFu, 0x04u)  // Timer enable
        assertEquals(true, bus.anyInterruptPending())
    }

    @Test
    fun anyInterruptPending_falseWhenOnlyFlagSet() {
        bus.writeByte(0xFF0Fu, 0x04u)  // Timer flag
        bus.writeByte(0xFFFFu, 0x00u)  // No enable
        assertEquals(false, bus.anyInterruptPending())
    }

    @Test
    fun ppuLcdc_routedThroughBus() {
        bus.writeByte(0xFF40u, 0x91u)
        assertEquals(0x91u.toUByte(), ppu.lcdc)
        assertEquals(0x91u.toUByte(), bus.readByte(0xFF40u))
    }

    @Test
    fun timerDiv_readFromTimer() {
        // DIV register at 0xFF04
        val div = bus.readByte(0xFF04u)
        assertEquals(timer.div, div)
    }

    @Test
    fun timerDiv_writeResetsDiv() {
        bus.writeByte(0xFF04u, 0xFFu)  // Any write resets DIV
        assertEquals(0.toUByte(), timer.div)
    }

    @Test
    fun wramEcho_readsFromWram() {
        bus.writeByte(0xC000u, 0x42u)
        // Echo RAM at 0xE000 mirrors WRAM 0xC000
        assertEquals(0x42u.toUByte(), bus.readByte(0xE000u))
    }

    @Test
    fun vram_routedToPpu() {
        bus.writeByte(0x8000u, 0xAAu)
        assertEquals(0xAAu.toUByte(), ppu.readVram(0x8000u))
        assertEquals(0xAAu.toUByte(), bus.readByte(0x8000u))
    }

    @Test
    fun oam_routedToPpu() {
        bus.writeByte(0xFE00u, 0x55u)
        assertEquals(0x55u.toUByte(), ppu.readOam(0xFE00u))
        assertEquals(0x55u.toUByte(), bus.readByte(0xFE00u))
    }

    @Test
    fun collectInterrupts_movesVblankIrqToFlag() {
        ppu.vblankIrq = true
        bus.collectInterrupts()
        assertEquals(false, ppu.vblankIrq)
        // bit 0 (VBlank) should be set in IF
        val ifReg = bus.readByte(0xFF0Fu)
        assertEquals(true, (ifReg.toInt() and 0x01) != 0)
    }

    @Test
    fun collectInterrupts_movesTimerIrqToFlag() {
        timer.timerIrq = true
        bus.collectInterrupts()
        assertEquals(false, timer.timerIrq)
        // bit 2 (Timer) should be set in IF
        val ifReg = bus.readByte(0xFF0Fu)
        assertEquals(true, (ifReg.toInt() and 0x04) != 0)
    }
}
