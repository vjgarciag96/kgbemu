package com.vicgarci.kgbem.cpu

import com.vicgarci.kgbem.cartridge.RomOnlyCartridge
import kotlin.test.Test
import kotlin.test.assertEquals

class RstCPUTest {

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
    fun rst_0x00() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xC7.toByte() // RST 0x00 opcode
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)

        cpu.step()

        assertEquals(0x0000.toUShort(), programCounter.get())
        val savedHigh = memoryBus.readByte(0xFFFD.toUShort()).toUInt()
        val savedLow = memoryBus.readByte(0xFFFC.toUShort()).toUInt()
        val savedProgramCounter = savedHigh shl 8 or savedLow
        assertEquals(0x0001.toUShort(), savedProgramCounter.toUShort())
        assertEquals(0xFFFC.toUShort(), stackPointer.getAndIncrement())
    }

    @Test
    fun rst_0x08() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xCF.toByte() // RST 0x08 opcode
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)

        cpu.step()

        assertEquals(0x0008.toUShort(), programCounter.get())
        val savedHigh = memoryBus.readByte(0xFFFD.toUShort()).toUInt()
        val savedLow = memoryBus.readByte(0xFFFC.toUShort()).toUInt()
        val savedProgramCounter = savedHigh shl 8 or savedLow
        assertEquals(0x0001.toUShort(), savedProgramCounter.toUShort())
        assertEquals(0xFFFC.toUShort(), stackPointer.getAndIncrement())
    }

    @Test
    fun rst_0x10() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xD7.toByte() // RST 0x10 opcode
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)

        cpu.step()

        assertEquals(0x0010.toUShort(), programCounter.get())
        val savedHigh = memoryBus.readByte(0xFFFD.toUShort()).toUInt()
        val savedLow = memoryBus.readByte(0xFFFC.toUShort()).toUInt()
        val savedProgramCounter = savedHigh shl 8 or savedLow
        assertEquals(0x0001.toUShort(), savedProgramCounter.toUShort())
        assertEquals(0xFFFC.toUShort(), stackPointer.getAndIncrement())
    }

    @Test
    fun rst_0x18() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xDF.toByte() // RST 0x18 opcode
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)

        cpu.step()

        assertEquals(0x0018.toUShort(), programCounter.get())
        val savedHigh = memoryBus.readByte(0xFFFD.toUShort()).toUInt()
        val savedLow = memoryBus.readByte(0xFFFC.toUShort()).toUInt()
        val savedProgramCounter = savedHigh shl 8 or savedLow
        assertEquals(0x0001.toUShort(), savedProgramCounter.toUShort())
        assertEquals(0xFFFC.toUShort(), stackPointer.getAndIncrement())
    }

    @Test
    fun rst_0x20() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xE7.toByte() // RST 0x20 opcode
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)

        cpu.step()

        assertEquals(0x0020.toUShort(), programCounter.get())
        val savedHigh = memoryBus.readByte(0xFFFD.toUShort()).toUInt()
        val savedLow = memoryBus.readByte(0xFFFC.toUShort()).toUInt()
        val savedProgramCounter = savedHigh shl 8 or savedLow
        assertEquals(0x0001.toUShort(), savedProgramCounter.toUShort())
        assertEquals(0xFFFC.toUShort(), stackPointer.getAndIncrement())
    }

    @Test
    fun rst_0x28() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xEF.toByte() // RST 0x28 opcode
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)

        cpu.step()

        assertEquals(0x0028.toUShort(), programCounter.get())
        val savedHigh = memoryBus.readByte(0xFFFD.toUShort()).toUInt()
        val savedLow = memoryBus.readByte(0xFFFC.toUShort()).toUInt()
        val savedProgramCounter = savedHigh shl 8 or savedLow
        assertEquals(0x0001.toUShort(), savedProgramCounter.toUShort())
        assertEquals(0xFFFC.toUShort(), stackPointer.getAndIncrement())
    }

    @Test
    fun rst_0x30() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xF7.toByte() // RST 0x30 opcode
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)

        cpu.step()

        assertEquals(0x0030.toUShort(), programCounter.get())
        val savedHigh = memoryBus.readByte(0xFFFD.toUShort()).toUInt()
        val savedLow = memoryBus.readByte(0xFFFC.toUShort()).toUInt()
        val savedProgramCounter = savedHigh shl 8 or savedLow
        assertEquals(0x0001.toUShort(), savedProgramCounter.toUShort())
        assertEquals(0xFFFC.toUShort(), stackPointer.getAndIncrement())
    }

    @Test
    fun rst_0x38() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xFF.toByte() // RST 0x38 opcode
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)

        cpu.step()

        assertEquals(0x0038.toUShort(), programCounter.get())
        val savedHigh = memoryBus.readByte(0xFFFD.toUShort()).toUInt()
        val savedLow = memoryBus.readByte(0xFFFC.toUShort()).toUInt()
        val savedProgramCounter = savedHigh shl 8 or savedLow
        assertEquals(0x0001.toUShort(), savedProgramCounter.toUShort())
        assertEquals(0xFFFC.toUShort(), stackPointer.getAndIncrement())
    }
}
