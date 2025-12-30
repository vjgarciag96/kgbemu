package com.vicgarci.kgbem.cpu

import com.vicgarci.kgbem.cpu.FlagsRegister.Companion.toFlagsRegister
import com.vicgarci.kgbem.cpu.FlagsRegister.Companion.toUByte
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CPUIncDecTest {

    private val registers = Registers(
        0x1.toUByte(),
        0.toUByte(),
        0.toUByte(),
        0xFF.toUByte(),
        0.toUByte(),
        0.toUByte(),
        0.toUByte(),
        0.toUByte(),
    )

    private var programCounter = ProgramCounter(0.toUShort())
    private val memory = Array(0x10000) { 0.toUByte() }
    private val memoryBus = MemoryBus(memory)
    private val stackPointer = StackPointer(0xFFFF.toUShort())

    private val cpu = CPU(
        registers,
        programCounter,
        memoryBus,
        stackPointer,
    )

    @Test
    fun increment_8bit_registerB() {
        memory[0] = 0x04.toUByte() // INC B

        cpu.step()

        assertEquals(0x01.toUByte(), registers.b)
        val flags = registers.f.toFlagsRegister()
        assertEquals(false, flags.zero)
        assertFalse(flags.subtract)
        assertFalse(flags.halfCarry)
    }

    @Test
    fun increment_8bit_registerC() {
        registers.c = 0x7F.toUByte()
        memory[0] = 0x0C.toUByte() // INC C

        cpu.step()

        assertEquals(0x80.toUByte(), registers.c)
        val flags = registers.f.toFlagsRegister()
        assertFalse(flags.zero)
        assertFalse(flags.subtract)
        assertTrue(flags.halfCarry)
    }

    @Test
    fun increment_8bit_registerD() {
        registers.d = 0xFF.toUByte()
        memory[0] = 0x14.toUByte() // INC D

        cpu.step()

        assertEquals(0x00.toUByte(), registers.d)
        val flags = registers.f.toFlagsRegister()
        assertTrue(flags.zero)
        assertFalse(flags.subtract)
        assertTrue(flags.halfCarry)
    }

    @Test
    fun increment_8bit_registerE() {
        registers.e = 0x0F.toUByte()
        memory[0] = 0x1C.toUByte() // INC E

        cpu.step()

        assertEquals(0x10.toUByte(), registers.e)
        val flags = registers.f.toFlagsRegister()
        assertFalse(flags.zero)
        assertFalse(flags.subtract)
        assertTrue(flags.halfCarry)
    }

    @Test
    fun increment_8bit_registerH() {
        registers.h = 0x00.toUByte()
        memory[0] = 0x24.toUByte() // INC H

        cpu.step()

        assertEquals(0x01.toUByte(), registers.h)
        val flags = registers.f.toFlagsRegister()
        assertFalse(flags.zero)
        assertFalse(flags.subtract)
        assertFalse(flags.halfCarry)
    }

    @Test
    fun increment_8bit_registerL() {
        registers.l = 0xFE.toUByte()
        memory[0] = 0x2C.toUByte() // INC L

        cpu.step()

        assertEquals(0xFF.toUByte(), registers.l)
        val flags = registers.f.toFlagsRegister()
        assertFalse(flags.zero)
        assertFalse(flags.subtract)
        assertFalse(flags.halfCarry)
    }

    @Test
    fun increment_8bit_registerA() {
        registers.a = 0x7F.toUByte()
        memory[0] = 0x3C.toUByte() // INC A

        cpu.step()

        assertEquals(0x80.toUByte(), registers.a)
        val flags = registers.f.toFlagsRegister()
        assertFalse(flags.zero)
        assertFalse(flags.subtract)
        assertTrue(flags.halfCarry)
    }

    @Test
    fun increment_8bit_preservesPrevCarry() {
        registers.f = FlagsRegister(
            zero = false,
            subtract = true,
            halfCarry = false,
            carry = true,
        ).toUByte()
        memory[0] = 0x04.toUByte() // INC B

        cpu.step()

        assertEquals(0x01.toUByte(), registers.b)
        val flags = registers.f.toFlagsRegister()
        assertEquals(false, flags.zero)
        assertFalse(flags.halfCarry)
        assertTrue(flags.carry)
    }

    @Test
    fun decrement_8bit_registerD() {
        memory[0] = 0x15.toUByte() // DEC D

        cpu.step()

        assertEquals(0xFE.toUByte(), registers.d)
        val flags = registers.f.toFlagsRegister()
        assertEquals(false, flags.zero)
        assertTrue(flags.subtract)
        assertFalse(flags.halfCarry)
    }

    @Test
    fun decrement_8bit_registerB() {
        registers.b = 0x01.toUByte()
        memory[0] = 0x05.toUByte() // DEC B

        cpu.step()

        assertEquals(0x00.toUByte(), registers.b)
        val flags = registers.f.toFlagsRegister()
        assertTrue(flags.zero)
        assertTrue(flags.subtract)
        assertFalse(flags.halfCarry)
    }

    @Test
    fun decrement_8bit_registerC() {
        registers.c = 0x00.toUByte()
        memory[0] = 0x0D.toUByte() // DEC C

        cpu.step()

        assertEquals(0xFF.toUByte(), registers.c)
        val flags = registers.f.toFlagsRegister()
        assertFalse(flags.zero)
        assertTrue(flags.subtract)
        assertTrue(flags.halfCarry)
    }

    @Test
    fun decrement_8bit_registerE() {
        registers.e = 0x10.toUByte()
        memory[0] = 0x1D.toUByte() // DEC E

        cpu.step()

        assertEquals(0x0F.toUByte(), registers.e)
        val flags = registers.f.toFlagsRegister()
        assertFalse(flags.zero)
        assertTrue(flags.subtract)
        assertTrue(flags.halfCarry)
    }

    @Test
    fun decrement_8bit_registerH() {
        registers.h = 0x80.toUByte()
        memory[0] = 0x25.toUByte() // DEC H

        cpu.step()

        assertEquals(0x7F.toUByte(), registers.h)
        val flags = registers.f.toFlagsRegister()
        assertFalse(flags.zero)
        assertTrue(flags.subtract)
        assertTrue(flags.halfCarry)
    }

    @Test
    fun decrement_8bit_registerL() {
        registers.l = 0xFF.toUByte()
        memory[0] = 0x2D.toUByte() // DEC L

        cpu.step()

        assertEquals(0xFE.toUByte(), registers.l)
        val flags = registers.f.toFlagsRegister()
        assertFalse(flags.zero)
        assertTrue(flags.subtract)
        assertFalse(flags.halfCarry)
    }

    @Test
    fun decrement_8bit_registerA() {
        registers.a = 0x00.toUByte()
        memory[0] = 0x3D.toUByte() // DEC A

        cpu.step()

        assertEquals(0xFF.toUByte(), registers.a)
        val flags = registers.f.toFlagsRegister()
        assertFalse(flags.zero)
        assertTrue(flags.subtract)
        assertTrue(flags.halfCarry)
    }

    @Test
    fun increment_16bit_registerBC() {
        registers.bc = 0x00FF.toUShort()
        memory[0] = 0x03.toUByte() // INC BC

        cpu.step()

        assertEquals(0x0100.toUShort(), registers.bc)
    }

    @Test
    fun increment_16bit_registerDE() {
        registers.de = 0xFFFF.toUShort()
        memory[0] = 0x13.toUByte() // INC DE

        cpu.step()

        assertEquals(0x0000.toUShort(), registers.de)
    }

    @Test
    fun increment_16bit_registerHL() {
        registers.hl = 0x1234.toUShort()
        memory[0] = 0x23.toUByte() // INC HL

        cpu.step()

        assertEquals(0x1235.toUShort(), registers.hl)
    }

    @Test
    fun increment_16bit_stackPointer() {
        stackPointer.setTo(0x7FFF.toUShort())
        memory[0] = 0x33.toUByte() // INC SP

        cpu.step()

        assertEquals(0x8000.toUShort(), stackPointer.get())
    }

    @Test
    fun increment_16bit_doesNotAffectFlags() {
        registers.f = FlagsRegister(
            zero = true,
            subtract = true,
            halfCarry = true,
            carry = true,
        ).toUByte()
        val originalFlags = registers.f
        memory[0] = 0x23.toUByte() // INC HL

        cpu.step()

        assertEquals(originalFlags, registers.f)
    }

    @Test
    fun decrement_16bit_registerBC() {
        registers.bc = 0x0100.toUShort()
        memory[0] = 0x0B.toUByte() // DEC BC

        cpu.step()

        assertEquals(0x00FF.toUShort(), registers.bc)
    }

    @Test
    fun decrement_16bit_registerDE() {
        registers.de = 0x0000.toUShort()
        memory[0] = 0x1B.toUByte() // DEC DE

        cpu.step()

        assertEquals(0xFFFF.toUShort(), registers.de)
    }

    @Test
    fun decrement_16bit_registerHL() {
        registers.hl = 0x1234.toUShort()
        memory[0] = 0x2B.toUByte() // DEC HL

        cpu.step()

        assertEquals(0x1233.toUShort(), registers.hl)
    }

    @Test
    fun decrement_16bit_stackPointer() {
        stackPointer.setTo(0x8000.toUShort())
        memory[0] = 0x3B.toUByte() // DEC SP

        cpu.step()

        assertEquals(0x7FFF.toUShort(), stackPointer.get())
    }

    @Test
    fun decrement_16bit_doesNotAffectFlags() {
        registers.f = FlagsRegister(
            zero = true,
            subtract = true,
            halfCarry = true,
            carry = true,
        ).toUByte()
        val originalFlags = registers.f
        memory[0] = 0x2B.toUByte() // DEC HL

        cpu.step()

        assertEquals(originalFlags, registers.f)
    }
}
