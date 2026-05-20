package com.vicgarci.kgbem

import com.vicgarci.kgbem.cpu.CPU
import com.vicgarci.kgbem.cpu.MemoryBus
import com.vicgarci.kgbem.cpu.ProgramCounter
import com.vicgarci.kgbem.cpu.Registers
import com.vicgarci.kgbem.cpu.StackPointer
import com.vicgarci.kgbem.ppu.PPU

class GameBoy(cartridge: Cartridge) {

    val memoryBus = MemoryBus()
    val ppu = PPU(memoryBus)
    private val timer = Timer(memoryBus)

    private val registers = Registers(
        a = 0x01u, b = 0x00u, c = 0x13u, d = 0x00u,
        e = 0xD8u, f = 0xB0u, h = 0x01u, l = 0x4Du,
    )
    private val cpu = CPU(
        registers,
        ProgramCounter(0x0100u),
        memoryBus,
        StackPointer(),
    )

    val frameBuffer: IntArray get() = ppu.frameBuffer

    init {
        memoryBus.loadCartridge(cartridge)
        applyPostBootState()
    }

    fun runFrame() {
        var cyclesLeft = CYCLES_PER_FRAME
        while (cyclesLeft > 0) {
            val cycles = cpu.step()
            ppu.tick(cycles)
            timer.tick(cycles)
            if (ppu.vBlankInterrupt) memoryBus.setInterruptFlagBit(0, true)
            if (ppu.statInterrupt) memoryBus.setInterruptFlagBit(1, true)
            if (timer.timerInterrupt) memoryBus.setInterruptFlagBit(2, true)
            cyclesLeft -= cycles
        }
    }

    private fun applyPostBootState() {
        memoryBus.writeByte(0xFF40.toUShort(), 0x91u) // LCDC: LCD on, BG on, tile data 0x8000
        memoryBus.writeByte(0xFF47.toUShort(), 0xFCu) // BGP: 3,3,3,0 palette
        memoryBus.writeByte(0xFF48.toUShort(), 0xFFu) // OBP0
        memoryBus.writeByte(0xFF49.toUShort(), 0xFFu) // OBP1
        memoryBus.writeByte(0xFF10.toUShort(), 0x80u) // NR10
        memoryBus.writeByte(0xFF11.toUShort(), 0xBFu) // NR11
        memoryBus.writeByte(0xFF12.toUShort(), 0xF3u) // NR12
        memoryBus.writeByte(0xFF14.toUShort(), 0xBFu) // NR14
        memoryBus.writeByte(0xFF16.toUShort(), 0x3Fu) // NR21
        memoryBus.writeByte(0xFF17.toUShort(), 0x00u) // NR22
        memoryBus.writeByte(0xFF19.toUShort(), 0xBFu) // NR24
        memoryBus.writeByte(0xFF1A.toUShort(), 0x7Fu) // NR30
        memoryBus.writeByte(0xFF1B.toUShort(), 0xFFu) // NR31
        memoryBus.writeByte(0xFF1C.toUShort(), 0x9Fu) // NR32
        memoryBus.writeByte(0xFF1E.toUShort(), 0xBFu) // NR34
        memoryBus.writeByte(0xFF20.toUShort(), 0xFFu) // NR41
        memoryBus.writeByte(0xFF23.toUShort(), 0xBFu) // NR44
        memoryBus.writeByte(0xFF24.toUShort(), 0x77u) // NR50
        memoryBus.writeByte(0xFF25.toUShort(), 0xF3u) // NR51
        memoryBus.writeByte(0xFF26.toUShort(), 0xF1u) // NR52
    }

    companion object {
        const val CYCLES_PER_FRAME = 70224
    }
}
