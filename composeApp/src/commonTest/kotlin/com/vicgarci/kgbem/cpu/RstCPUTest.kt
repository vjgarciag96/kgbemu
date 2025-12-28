package com.vicgarci.kgbem.cpu

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
    private val memory = Array(10) { 0.toUByte() }
    private val memoryBus = MemoryBus(memory)
    private val stackPointer = StackPointer(8.toUShort())

    private val cpu = CPU(
        registers,
        programCounter,
        memoryBus,
        stackPointer,
    )

    @Test
    fun rst_0x00() {
        memory[0] = 0xC7.toUByte() // RST 0x00 opcode

        cpu.step()

        assertEquals(0x0000.toUShort(), programCounter.get())
        val savedProgramCounter = memory[7].toUInt() shl 8 or memory[6].toUInt()
        assertEquals(0x0001.toUShort(), savedProgramCounter.toUShort())
        assertEquals(6.toUShort(), stackPointer.getAndIncrement())
    }

    @Test
    fun rst_0x08() {
        memory[0] = 0xCF.toUByte() // RST 0x08 opcode

        cpu.step()

        assertEquals(0x0008.toUShort(), programCounter.get())
        val savedProgramCounter = memory[7].toUInt() shl 8 or memory[6].toUInt()
        assertEquals(0x0001.toUShort(), savedProgramCounter.toUShort())
        assertEquals(6.toUShort(), stackPointer.getAndIncrement())
    }

    @Test
    fun rst_0x10() {
        memory[0] = 0xD7.toUByte() // RST 0x10 opcode

        cpu.step()

        assertEquals(0x0010.toUShort(), programCounter.get())
        val savedProgramCounter = memory[7].toUInt() shl 8 or memory[6].toUInt()
        assertEquals(0x0001.toUShort(), savedProgramCounter.toUShort())
        assertEquals(6.toUShort(), stackPointer.getAndIncrement())
    }

    @Test
    fun rst_0x18() {
        memory[0] = 0xDF.toUByte() // RST 0x18 opcode

        cpu.step()

        assertEquals(0x0018.toUShort(), programCounter.get())
        val savedProgramCounter = memory[7].toUInt() shl 8 or memory[6].toUInt()
        assertEquals(0x0001.toUShort(), savedProgramCounter.toUShort())
        assertEquals(6.toUShort(), stackPointer.getAndIncrement())
    }

    @Test
    fun rst_0x20() {
        memory[0] = 0xE7.toUByte() // RST 0x20 opcode

        cpu.step()

        assertEquals(0x0020.toUShort(), programCounter.get())
        val savedProgramCounter = memory[7].toUInt() shl 8 or memory[6].toUInt()
        assertEquals(0x0001.toUShort(), savedProgramCounter.toUShort())
        assertEquals(6.toUShort(), stackPointer.getAndIncrement())
    }

    @Test
    fun rst_0x28() {
        memory[0] = 0xEF.toUByte() // RST 0x28 opcode

        cpu.step()

        assertEquals(0x0028.toUShort(), programCounter.get())
        val savedProgramCounter = memory[7].toUInt() shl 8 or memory[6].toUInt()
        assertEquals(0x0001.toUShort(), savedProgramCounter.toUShort())
        assertEquals(6.toUShort(), stackPointer.getAndIncrement())
    }

    @Test
    fun rst_0x30() {
        memory[0] = 0xF7.toUByte() // RST 0x30 opcode

        cpu.step()

        assertEquals(0x0030.toUShort(), programCounter.get())
        val savedProgramCounter = memory[7].toUInt() shl 8 or memory[6].toUInt()
        assertEquals(0x0001.toUShort(), savedProgramCounter.toUShort())
        assertEquals(6.toUShort(), stackPointer.getAndIncrement())
    }

    @Test
    fun rst_0x38() {
        memory[0] = 0xFF.toUByte() // RST 0x38 opcode

        cpu.step()

        assertEquals(0x0038.toUShort(), programCounter.get())
        val savedProgramCounter = memory[7].toUInt() shl 8 or memory[6].toUInt()
        assertEquals(0x0001.toUShort(), savedProgramCounter.toUShort())
        assertEquals(6.toUShort(), stackPointer.getAndIncrement())
    }
}
