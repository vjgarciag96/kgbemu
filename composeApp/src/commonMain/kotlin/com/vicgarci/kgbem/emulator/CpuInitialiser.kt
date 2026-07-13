package com.vicgarci.kgbem.emulator

import com.vicgarci.kgbem.cpu.MemoryBus
import com.vicgarci.kgbem.cpu.ProgramCounter
import com.vicgarci.kgbem.cpu.Registers
import com.vicgarci.kgbem.cpu.StackPointer

/**
 * Sets the CPU and memory to DMG-01 post-boot state, allowing
 * the emulator to start execution at PC=0x0100 without running
 * the boot ROM.
 */
object CpuInitialiser {

    fun applyPostBootState(
        registers: Registers,
        programCounter: ProgramCounter,
        stackPointer: StackPointer,
        bus: MemoryBus,
    ) {
        // CPU registers
        registers.a = 0x01.toUByte()
        registers.f = 0xB0.toUByte()
        registers.b = 0x00.toUByte()
        registers.c = 0x13.toUByte()
        registers.d = 0x00.toUByte()
        registers.e = 0xD8.toUByte()
        registers.h = 0x01.toUByte()
        registers.l = 0x4D.toUByte()

        stackPointer.setTo(0xFFFE.toUShort())
        programCounter.setTo(0x0100.toUShort())

        // I/O registers and memory
        bus.writeByte(0xFF05.toUShort(), 0x00.toUByte()) // TIMA
        bus.writeByte(0xFF06.toUShort(), 0x00.toUByte()) // TMA
        bus.writeByte(0xFF07.toUShort(), 0x00.toUByte()) // TAC
        bus.writeByte(0xFF40.toUShort(), 0x91.toUByte()) // LCDC
        bus.writeByte(0xFF42.toUShort(), 0x00.toUByte()) // SCY
        bus.writeByte(0xFF43.toUShort(), 0x00.toUByte()) // SCX
        bus.writeByte(0xFF45.toUShort(), 0x00.toUByte()) // LYC
        bus.writeByte(0xFF47.toUShort(), 0xFC.toUByte()) // BGP
        bus.writeByte(0xFF48.toUShort(), 0xFF.toUByte()) // OBP0
        bus.writeByte(0xFF49.toUShort(), 0xFF.toUByte()) // OBP1
        bus.writeByte(0xFF4A.toUShort(), 0x00.toUByte()) // WY
        bus.writeByte(0xFF4B.toUShort(), 0x00.toUByte()) // WX
        bus.writeByte(0xFFFF.toUShort(), 0x00.toUByte()) // IE
    }
}
