package com.vicgarci.kgbem.cpu

import kotlin.test.Test
import kotlin.test.assertEquals

class LoadCPUTest {

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
    private val memory = Array(2) { 0.toUByte() }
    private val memoryBus = MemoryBus(memory)

    private val cpu = CPU(
        registers,
        programCounter,
        memoryBus,
    )

    @Test
    fun load_constant_intoRegisterB() {
        memory[0] = 0x06.toUByte() // LD B, n opcode
        memory[1] = 0x42.toUByte() // value to load into register B

        cpu.step()

        assertEquals(0x42.toUByte(), registers.b)
    }

    @Test
    fun load_constant_intoRegisterC() {
        memory[0] = 0x0E.toUByte() // LD C, n opcode
        memory[1] = 0x43.toUByte() // value to load into register C

        cpu.step()

        assertEquals(0x43.toUByte(), registers.c)
    }

    @Test
    fun load_constant_intoRegisterD() {
        memory[0] = 0x16.toUByte() // LD D, n opcode
        memory[1] = 0x44.toUByte() // value to load into register D

        cpu.step()

        assertEquals(0x44.toUByte(), registers.d)
    }

    @Test
    fun load_constant_intoRegisterE() {
        memory[0] = 0x1E.toUByte() // LD E, n opcode
        memory[1] = 0x45.toUByte() // value to load into register E

        cpu.step()

        assertEquals(0x45.toUByte(), registers.e)
    }

    @Test
    fun load_constant_intoRegisterH() {
        memory[0] = 0x26.toUByte() // LD H, n opcode
        memory[1] = 0x46.toUByte() // value to load into register H

        cpu.step()

        assertEquals(0x46.toUByte(), registers.h)
    }

    @Test
    fun load_constant_intoRegisterL() {
        memory[0] = 0x2E.toUByte() // LD L, n opcode
        memory[1] = 0x47.toUByte() // value to load into register L

        cpu.step()

        assertEquals(0x47.toUByte(), registers.l)
    }

    @Test
    fun load_constant_intoRegisterA() {
        memory[0] = 0x3E.toUByte() // LD A, n opcode
        memory[1] = 0x48.toUByte() // value to load into register A

        cpu.step()

        assertEquals(0x48.toUByte(), registers.a)
    }
}