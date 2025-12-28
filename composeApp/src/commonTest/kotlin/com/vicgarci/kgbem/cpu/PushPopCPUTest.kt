package com.vicgarci.kgbem.cpu

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
    fun popBC() {
        memory[0] = 0xC1.toUByte() // POP BC opcode
        memory[8] = 0x34.toUByte() // low byte
        memory[9] = 0x12.toUByte() // high byte

        cpu.step()

        assertEquals(0x1234.toUShort(), registers.bc)
    }


    @Test
    fun popDE() {
        memory[0] = 0xD1.toUByte() // POP DE opcode
        memory[8] = 0x34.toUByte() // low byte
        memory[9] = 0x12.toUByte() // high byte

        cpu.step()

        assertEquals(0x1234.toUShort(), registers.de)
    }

    @Test
    fun popHL() {
        memory[0] = 0xE1.toUByte() // POP HL opcode
        memory[8] = 0x34.toUByte() // low byte
        memory[9] = 0x12.toUByte() // high byte

        cpu.step()

        assertEquals(0x1234.toUShort(), registers.hl)
    }

    @Test
    fun popAF() {
        memory[0] = 0xF1.toUByte() // POP AF opcode
        memory[8] = 0x34.toUByte() // low byte
        memory[9] = 0x12.toUByte() // high byte

        cpu.step()

        // F low byte should always be 0
        assertEquals(0x1230.toUShort(), registers.af)
    }

    @Test
    fun pushBC() {
        memory[0] = 0xC5.toUByte() // PUSH BC opcode
        registers.b = 0x12.toUByte()
        registers.c = 0x34.toUByte()

        cpu.step()

        assertEquals(0x34.toUByte(), memory[6]) // low byte
        assertEquals(0x12.toUByte(), memory[7]) // high byte
    }

    @Test
    fun pushDE() {
        memory[0] = 0xD5.toUByte() // PUSH DE opcode
        registers.d = 0x12.toUByte()
        registers.e = 0x34.toUByte()

        cpu.step()

        assertEquals(0x34.toUByte(), memory[6]) // low byte
        assertEquals(0x12.toUByte(), memory[7]) // high byte
    }

    @Test
    fun pushHL() {
        memory[0] = 0xE5.toUByte() // PUSH HL opcode
        registers.h = 0x12.toUByte()
        registers.l = 0x34.toUByte()

        cpu.step()

        assertEquals(0x34.toUByte(), memory[6]) // low byte
        assertEquals(0x12.toUByte(), memory[7]) // high byte
    }

    @Test
    fun pushAF() {
        memory[0] = 0xF5.toUByte() // PUSH AF opcode
        registers.a = 0x12.toUByte()
        registers.f = 0x34.toUByte()

        cpu.step()

        assertEquals(0x34.toUByte(), memory[6]) // low byte
        assertEquals(0x12.toUByte(), memory[7]) // high byte
    }
}