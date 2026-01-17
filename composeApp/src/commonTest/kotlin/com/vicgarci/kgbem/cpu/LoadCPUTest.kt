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
    private val memory = Array(0x10000) { 0.toUByte() }
    private val memoryBus = MemoryBus(memory)
    private val stackPointer = StackPointer(0x3.toUShort())

    private val cpu = CPU(
        registers,
        programCounter,
        memoryBus,
        stackPointer,
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

    @Test
    fun load_constant_into_memoryAtHL() {
        registers.hl = 0x1234.toUShort()
        memory[0] = 0x36.toUByte() // LD (HL), n opcode
        memory[1] = 0x99.toUByte() // value to store at HL

        cpu.step()

        assertEquals(0x99.toUByte(), memory[0x1234])
    }

    @Test
    fun load_constant_intoRegisterBC() {
        memory[0] = 0x01.toUByte() // LD BC, nn opcode
        memory[1] = 0x34.toUByte() // low byte
        memory[2] = 0x12.toUByte() // high byte

        cpu.step()

        assertEquals(0x1234.toUShort(), registers.bc)
    }

    @Test
    fun load_constant_intoRegisterDE() {
        memory[0] = 0x11.toUByte() // LD DE, nn opcode
        memory[1] = 0x56.toUByte() // low byte
        memory[2] = 0x34.toUByte() // high byte

        cpu.step()

        assertEquals(0x3456.toUShort(), registers.de)
    }

    @Test
    fun load_constant_intoRegisterHL() {
        memory[0] = 0x21.toUByte() // LD HL, nn opcode
        memory[1] = 0x78.toUByte() // low byte
        memory[2] = 0x56.toUByte() // high byte

        cpu.step()

        assertEquals(0x5678.toUShort(), registers.hl)
    }

    @Test
    fun load_constant_intoRegisterSP() {
        memory[0] = 0x31.toUByte() // LD SP, nn opcode
        memory[1] = 0x9A.toUByte() // low byte
        memory[2] = 0x78.toUByte() // high byte

        cpu.step()

        assertEquals(0x789A.toUShort(), stackPointer.get())
    }

    @Test
    fun loadIncrement_A_from_HL() {
        registers.hl = 0x9.toUShort()
        memory[0x9] = 0x42.toUByte() // Value at HL
        memory[0] = 0x2A.toUByte() // LD A, (HL+) opcode

        cpu.step()

        assertEquals(0x42.toUByte(), registers.a) // A should now hold the value at HL
        assertEquals(0xA.toUShort(), registers.hl) // HL should increment by 1
    }

    @Test
    fun loadIncrement_HL_from_A() {
        registers.hl = 0x9.toUShort()
        registers.a = 0x42.toUByte() // Value in A
        memory[0] = 0x22.toUByte() // LD (HL+), A opcode

        cpu.step()

        assertEquals(0x42.toUByte(), memory[0x9]) // Memory at HL should now hold the value of A
        assertEquals(0xA.toUShort(), registers.hl) // HL should increment by 1
    }

    @Test
    fun loadDecrement_A_from_HL() {
        registers.hl = 0x9.toUShort()
        memory[0x9] = 0x42.toUByte() // Value at HL
        memory[0] = 0x3A.toUByte() // LD A, (HL-) opcode

        cpu.step()

        assertEquals(0x42.toUByte(), registers.a) // A should now hold the value at HL
        assertEquals(0x8.toUShort(), registers.hl) // HL should decrement by 1
    }

    @Test
    fun loadDecrement_HL_from_A() {
        registers.hl = 0x9.toUShort()
        registers.a = 0x42.toUByte() // Value in A
        memory[0] = 0x32.toUByte() // LD (HL-), A opcode

        cpu.step()

        assertEquals(0x42.toUByte(), memory[0x9]) // Memory at HL should now hold the value of A
        assertEquals(0x8.toUShort(), registers.hl) // HL should decrement by 1
    }

    @Test
    fun loadIncrement_wrapAround() {
        registers.hl = 0xFFFF.toUShort()
        memory[0] = 0x22.toUByte() // LD (HL+), A opcode

        cpu.step()

        assertEquals(0x0000.toUShort(), registers.hl)
    }

    @Test
    fun loadDecrement_wrapAround() {
        registers.hl = 0x0000.toUShort()
        memory[0] = 0x32.toUByte() // LD (HL-), A opcode

        cpu.step()

        assertEquals(0xFFFF.toUShort(), registers.hl)
    }

    @Test
    fun load_registerB_into_registerA() {
        registers.b = 0x45.toUByte()
        memory[0] = 0x78.toUByte() // LD A, B opcode

        cpu.step()

        assertEquals(0x45.toUByte(), registers.a) // Value from B should be loaded into A
    }

    @Test
    fun load_registerC_into_registerB() {
        registers.c = 0x42.toUByte()
        memory[0] = 0x41.toUByte() // LD B, C opcode

        cpu.step()

        assertEquals(0x42.toUByte(), registers.b) // Value from C should be loaded into B
    }

    @Test
    fun load_registerD_into_registerB() {
        registers.d = 0x50.toUByte()
        memory[0] = 0x42.toUByte() // LD B, D opcode

        cpu.step()

        assertEquals(0x50.toUByte(), registers.b) // Value from D should be loaded into B
    }

    @Test
    fun load_registerE_into_registerD() {
        registers.e = 0x43.toUByte()
        memory[0] = 0x53.toUByte() // LD D, E opcode

        cpu.step()

        assertEquals(0x43.toUByte(), registers.d) // Value from E should be loaded into D
    }
    @Test
    fun load_registerH_into_registerE() {
        registers.h = 0x47.toUByte()
        memory[0] = 0x5C.toUByte() // LD E, H opcode

        cpu.step()

        assertEquals(0x47.toUByte(), registers.e) // Value from H should be loaded into E
    }

    @Test
    fun load_registerL_into_registerH() {
        registers.l = 0x44.toUByte()
        memory[0] = 0x65.toUByte() // LD H, L opcode

        cpu.step()

        assertEquals(0x44.toUByte(), registers.h) // Value from L should be loaded into H
    }

    @Test
    fun load_registerA_into_registerC() {
        registers.a = 0x46.toUByte()
        memory[0] = 0x4F.toUByte() // LD C, A opcode

        cpu.step()

        assertEquals(0x46.toUByte(), registers.c) // Value from A should be loaded into C
    }

    @Test
    fun load_memoryAtHL_into_registerA() {
        registers.hl = 0x1000.toUShort()
        memory[0x1000] = 0x42.toUByte() // Value at HL
        memory[0] = 0x7E.toUByte() // LD A, (HL) opcode

        cpu.step()

        assertEquals(0x42.toUByte(), registers.a) // Value at HL should be loaded into A
    }

    @Test
    fun load_registerA_into_memoryAtHL() {
        registers.hl = 0x1000.toUShort()
        registers.a = 0x42.toUByte() // Value in A
        memory[0] = 0x77.toUByte() // LD (HL), A opcode

        cpu.step()

        assertEquals(0x42.toUByte(), memory[0x1000]) // Value in A should be stored at HL
    }

    @Test
    fun load_registerA_into_memoryAtBC() {
        registers.bc = 0x2000.toUShort()
        registers.a = 0x77.toUByte() // Value in A
        memory[0] = 0x02.toUByte() // LD (BC), A opcode

        cpu.step()

        assertEquals(0x77.toUByte(), memory[0x2000])
    }

    @Test
    fun load_registerA_into_memoryAtDE() {
        registers.de = 0x2001.toUShort()
        registers.a = 0x88.toUByte() // Value in A
        memory[0] = 0x12.toUByte() // LD (DE), A opcode

        cpu.step()

        assertEquals(0x88.toUByte(), memory[0x2001])
    }

    @Test
    fun load_memoryAtBC_into_registerA() {
        registers.bc = 0x2002.toUShort()
        memory[0x2002] = 0x55.toUByte() // Value at BC
        memory[0] = 0x0A.toUByte() // LD A, (BC) opcode

        cpu.step()

        assertEquals(0x55.toUByte(), registers.a)
    }

    @Test
    fun load_memoryAtDE_into_registerA() {
        registers.de = 0x2003.toUShort()
        memory[0x2003] = 0x66.toUByte() // Value at DE
        memory[0] = 0x1A.toUByte() // LD A, (DE) opcode

        cpu.step()

        assertEquals(0x66.toUByte(), registers.a)
    }

    @Test
    fun load_registerA_into_memoryAtImmediate() {
        registers.a = 0x5A.toUByte() // Value in A
        memory[0] = 0xEA.toUByte() // LD (nn), A opcode
        memory[1] = 0x34.toUByte() // low byte
        memory[2] = 0x12.toUByte() // high byte

        cpu.step()

        assertEquals(0x5A.toUByte(), memory[0x1234])
    }

    @Test
    fun load_memoryAtImmediate_into_registerA() {
        memory[0x5678] = 0xAB.toUByte() // Value at nn
        memory[0] = 0xFA.toUByte() // LD A, (nn) opcode
        memory[1] = 0x78.toUByte() // low byte
        memory[2] = 0x56.toUByte() // high byte

        cpu.step()

        assertEquals(0xAB.toUByte(), registers.a)
    }

    @Test
    fun load_registerA_into_highMemoryAtImmediate() {
        registers.a = 0x1A.toUByte()
        memory[0] = 0xE0.toUByte() // LDH (n), A opcode
        memory[1] = 0x10.toUByte()

        cpu.step()

        assertEquals(0x1A.toUByte(), memory[0xFF10])
    }

    @Test
    fun load_highMemoryAtImmediate_into_registerA() {
        memory[0xFF20] = 0x2B.toUByte()
        memory[0] = 0xF0.toUByte() // LDH A, (n) opcode
        memory[1] = 0x20.toUByte()

        cpu.step()

        assertEquals(0x2B.toUByte(), registers.a)
    }

    @Test
    fun load_registerA_into_highMemoryAtC() {
        registers.a = 0x3C.toUByte()
        registers.c = 0x30.toUByte()
        memory[0] = 0xE2.toUByte() // LD (C), A opcode

        cpu.step()

        assertEquals(0x3C.toUByte(), memory[0xFF30])
    }

    @Test
    fun load_highMemoryAtC_into_registerA() {
        registers.c = 0x40.toUByte()
        memory[0xFF40] = 0x4D.toUByte()
        memory[0] = 0xF2.toUByte() // LD A, (C) opcode

        cpu.step()

        assertEquals(0x4D.toUByte(), registers.a)
    }

    @Test
    fun load_memoryAtHL_into_registerB() {
        registers.hl = 0x2000.toUShort()
        memory[0x2000] = 0x55.toUByte() // Value at HL
        memory[0] = 0x46.toUByte() // LD B, (HL) opcode

        cpu.step()

        assertEquals(0x55.toUByte(), registers.b) // Value at HL should be loaded into B
    }

    @Test
    fun load_registerC_into_memoryAtHL() {
        registers.hl = 0x2000.toUShort()
        registers.c = 0x66.toUByte() // Value in C
        memory[0] = 0x71.toUByte() // LD (HL), C opcode

        cpu.step()

        assertEquals(0x66.toUByte(), memory[0x2000]) // Value in C should be stored at HL
    }
}
