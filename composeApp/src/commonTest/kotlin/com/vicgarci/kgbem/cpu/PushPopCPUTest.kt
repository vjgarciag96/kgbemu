package com.vicgarci.kgbem.cpu

import com.vicgarci.kgbem.cartridge.RomOnlyCartridge
import kotlin.test.Test
import kotlin.test.assertEquals

class PushPopCPUTest {

    private val registers = Registers(
        0.toUByte(),
        0.toUByte(),
        0.toUByte(),
        0.toUByte(),
        0.toUByte(),
        0.toUByte(),
        0.toUByte(),
        0.toUByte(),
    )

    private var programCounter = ProgramCounter(0.toUShort())
    private val stackPointer = StackPointer(0xFFFE.toUShort())

    private fun createCpuWithMemoryBus(rom: ByteArray): Pair<CPU, MemoryBus> {
        val memoryBus = MemoryBus(RomOnlyCartridge(rom))
        val cpu = CPU(registers, programCounter, memoryBus, stackPointer)
        return cpu to memoryBus
    }

    @Test
    fun popBC() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xC1.toByte() // POP BC opcode
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)
        memoryBus.writeByte(0xFFFE.toUShort(), 0x34.toUByte()) // low byte
        memoryBus.writeByte(0xFFFF.toUShort(), 0x12.toUByte()) // high byte

        cpu.step()

        assertEquals(0x1234.toUShort(), registers.bc)
    }


    @Test
    fun popDE() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xD1.toByte() // POP DE opcode
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)
        memoryBus.writeByte(0xFFFE.toUShort(), 0x34.toUByte()) // low byte
        memoryBus.writeByte(0xFFFF.toUShort(), 0x12.toUByte()) // high byte

        cpu.step()

        assertEquals(0x1234.toUShort(), registers.de)
    }

    @Test
    fun popHL() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xE1.toByte() // POP HL opcode
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)
        memoryBus.writeByte(0xFFFE.toUShort(), 0x34.toUByte()) // low byte
        memoryBus.writeByte(0xFFFF.toUShort(), 0x12.toUByte()) // high byte

        cpu.step()

        assertEquals(0x1234.toUShort(), registers.hl)
    }

    @Test
    fun popAF() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xF1.toByte() // POP AF opcode
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)
        memoryBus.writeByte(0xFFFE.toUShort(), 0x34.toUByte()) // low byte
        memoryBus.writeByte(0xFFFF.toUShort(), 0x12.toUByte()) // high byte

        cpu.step()

        // F low byte should always be 0
        assertEquals(0x1230.toUShort(), registers.af)
    }

    @Test
    fun pushBC() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xC5.toByte() // PUSH BC opcode
        registers.b = 0x12.toUByte()
        registers.c = 0x34.toUByte()
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)

        cpu.step()

        assertEquals(0x34.toUByte(), memoryBus.readByte(0xFFFC.toUShort())) // low byte
        assertEquals(0x12.toUByte(), memoryBus.readByte(0xFFFD.toUShort())) // high byte
    }

    @Test
    fun pushDE() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xD5.toByte() // PUSH DE opcode
        registers.d = 0x12.toUByte()
        registers.e = 0x34.toUByte()
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)

        cpu.step()

        assertEquals(0x34.toUByte(), memoryBus.readByte(0xFFFC.toUShort())) // low byte
        assertEquals(0x12.toUByte(), memoryBus.readByte(0xFFFD.toUShort())) // high byte
    }

    @Test
    fun pushHL() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xE5.toByte() // PUSH HL opcode
        registers.h = 0x12.toUByte()
        registers.l = 0x34.toUByte()
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)

        cpu.step()

        assertEquals(0x34.toUByte(), memoryBus.readByte(0xFFFC.toUShort())) // low byte
        assertEquals(0x12.toUByte(), memoryBus.readByte(0xFFFD.toUShort())) // high byte
    }

    @Test
    fun pushAF() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xF5.toByte() // PUSH AF opcode
        registers.a = 0x12.toUByte()
        registers.f = 0x34.toUByte()
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)

        cpu.step()

        assertEquals(0x34.toUByte(), memoryBus.readByte(0xFFFC.toUShort())) // low byte
        assertEquals(0x12.toUByte(), memoryBus.readByte(0xFFFD.toUShort())) // high byte
    }
}
