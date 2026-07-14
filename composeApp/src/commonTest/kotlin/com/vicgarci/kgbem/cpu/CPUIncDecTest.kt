package com.vicgarci.kgbem.cpu

import com.vicgarci.kgbem.cartridge.RomOnlyCartridge
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
    private val stackPointer = StackPointer(0xFFFF.toUShort())

    private fun createCpu(rom: ByteArray): CPU {
        val memoryBus = MemoryBus(RomOnlyCartridge(rom))
        return CPU(registers, programCounter, memoryBus, stackPointer)
    }

    @Test
    fun increment_8bit_registerB() {
        val rom = ByteArray(0x8000)
        rom[0] = 0x04.toByte() // INC B
        val cpu = createCpu(rom)

        val cycles = cpu.step()

        assertEquals(4, cycles)
        assertEquals(0x01.toUByte(), registers.b)
        val flags = registers.f.toFlagsRegister()
        assertEquals(false, flags.zero)
        assertFalse(flags.subtract)
        assertFalse(flags.halfCarry)
    }

    @Test
    fun increment_8bit_registerC() {
        registers.c = 0x7F.toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0x0C.toByte() // INC C
        val cpu = createCpu(rom)

        val cycles = cpu.step()

        assertEquals(4, cycles)
        assertEquals(0x80.toUByte(), registers.c)
        val flags = registers.f.toFlagsRegister()
        assertFalse(flags.zero)
        assertFalse(flags.subtract)
        assertTrue(flags.halfCarry)
    }

    @Test
    fun increment_8bit_registerD() {
        registers.d = 0xFF.toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0x14.toByte() // INC D
        val cpu = createCpu(rom)

        val cycles = cpu.step()

        assertEquals(4, cycles)
        assertEquals(0x00.toUByte(), registers.d)
        val flags = registers.f.toFlagsRegister()
        assertTrue(flags.zero)
        assertFalse(flags.subtract)
        assertTrue(flags.halfCarry)
    }

    @Test
    fun increment_8bit_registerE() {
        registers.e = 0x0F.toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0x1C.toByte() // INC E
        val cpu = createCpu(rom)

        val cycles = cpu.step()

        assertEquals(4, cycles)
        assertEquals(0x10.toUByte(), registers.e)
        val flags = registers.f.toFlagsRegister()
        assertFalse(flags.zero)
        assertFalse(flags.subtract)
        assertTrue(flags.halfCarry)
    }

    @Test
    fun increment_8bit_registerH() {
        registers.h = 0x00.toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0x24.toByte() // INC H
        val cpu = createCpu(rom)

        val cycles = cpu.step()

        assertEquals(4, cycles)
        assertEquals(0x01.toUByte(), registers.h)
        val flags = registers.f.toFlagsRegister()
        assertFalse(flags.zero)
        assertFalse(flags.subtract)
        assertFalse(flags.halfCarry)
    }

    @Test
    fun increment_8bit_registerL() {
        registers.l = 0xFE.toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0x2C.toByte() // INC L
        val cpu = createCpu(rom)

        val cycles = cpu.step()

        assertEquals(4, cycles)
        assertEquals(0xFF.toUByte(), registers.l)
        val flags = registers.f.toFlagsRegister()
        assertFalse(flags.zero)
        assertFalse(flags.subtract)
        assertFalse(flags.halfCarry)
    }

    @Test
    fun increment_8bit_registerA() {
        registers.a = 0x7F.toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0x3C.toByte() // INC A
        val cpu = createCpu(rom)

        val cycles = cpu.step()

        assertEquals(4, cycles)
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
        val rom = ByteArray(0x8000)
        rom[0] = 0x04.toByte() // INC B
        val cpu = createCpu(rom)

        val cycles = cpu.step()

        assertEquals(4, cycles)
        assertEquals(0x01.toUByte(), registers.b)
        val flags = registers.f.toFlagsRegister()
        assertEquals(false, flags.zero)
        assertFalse(flags.halfCarry)
        assertTrue(flags.carry)
    }

    @Test
    fun decrement_8bit_registerD() {
        val rom = ByteArray(0x8000)
        rom[0] = 0x15.toByte() // DEC D
        val cpu = createCpu(rom)

        val cycles = cpu.step()

        assertEquals(4, cycles)
        assertEquals(0xFE.toUByte(), registers.d)
        val flags = registers.f.toFlagsRegister()
        assertEquals(false, flags.zero)
        assertTrue(flags.subtract)
        assertFalse(flags.halfCarry)
    }

    @Test
    fun decrement_8bit_registerB() {
        registers.b = 0x01.toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0x05.toByte() // DEC B
        val cpu = createCpu(rom)

        val cycles = cpu.step()

        assertEquals(4, cycles)
        assertEquals(0x00.toUByte(), registers.b)
        val flags = registers.f.toFlagsRegister()
        assertTrue(flags.zero)
        assertTrue(flags.subtract)
        assertFalse(flags.halfCarry)
    }

    @Test
    fun decrement_8bit_registerC() {
        registers.c = 0x00.toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0x0D.toByte() // DEC C
        val cpu = createCpu(rom)

        val cycles = cpu.step()

        assertEquals(4, cycles)
        assertEquals(0xFF.toUByte(), registers.c)
        val flags = registers.f.toFlagsRegister()
        assertFalse(flags.zero)
        assertTrue(flags.subtract)
        assertTrue(flags.halfCarry)
    }

    @Test
    fun decrement_8bit_registerE() {
        registers.e = 0x10.toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0x1D.toByte() // DEC E
        val cpu = createCpu(rom)

        val cycles = cpu.step()

        assertEquals(4, cycles)
        assertEquals(0x0F.toUByte(), registers.e)
        val flags = registers.f.toFlagsRegister()
        assertFalse(flags.zero)
        assertTrue(flags.subtract)
        assertTrue(flags.halfCarry)
    }

    @Test
    fun decrement_8bit_registerH() {
        registers.h = 0x80.toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0x25.toByte() // DEC H
        val cpu = createCpu(rom)

        val cycles = cpu.step()

        assertEquals(4, cycles)
        assertEquals(0x7F.toUByte(), registers.h)
        val flags = registers.f.toFlagsRegister()
        assertFalse(flags.zero)
        assertTrue(flags.subtract)
        assertTrue(flags.halfCarry)
    }

    @Test
    fun decrement_8bit_registerL() {
        registers.l = 0xFF.toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0x2D.toByte() // DEC L
        val cpu = createCpu(rom)

        val cycles = cpu.step()

        assertEquals(4, cycles)
        assertEquals(0xFE.toUByte(), registers.l)
        val flags = registers.f.toFlagsRegister()
        assertFalse(flags.zero)
        assertTrue(flags.subtract)
        assertFalse(flags.halfCarry)
    }

    @Test
    fun decrement_8bit_registerA() {
        registers.a = 0x00.toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0x3D.toByte() // DEC A
        val cpu = createCpu(rom)

        val cycles = cpu.step()

        assertEquals(4, cycles)
        assertEquals(0xFF.toUByte(), registers.a)
        val flags = registers.f.toFlagsRegister()
        assertFalse(flags.zero)
        assertTrue(flags.subtract)
        assertTrue(flags.halfCarry)
    }

    @Test
    fun increment_16bit_registerBC() {
        registers.bc = 0x00FF.toUShort()
        val rom = ByteArray(0x8000)
        rom[0] = 0x03.toByte() // INC BC
        val cpu = createCpu(rom)

        val cycles = cpu.step()

        assertEquals(8, cycles)
        assertEquals(0x0100.toUShort(), registers.bc)
    }

    @Test
    fun increment_16bit_registerDE() {
        registers.de = 0xFFFF.toUShort()
        val rom = ByteArray(0x8000)
        rom[0] = 0x13.toByte() // INC DE
        val cpu = createCpu(rom)

        val cycles = cpu.step()

        assertEquals(8, cycles)
        assertEquals(0x0000.toUShort(), registers.de)
    }

    @Test
    fun increment_16bit_registerHL() {
        registers.hl = 0x1234.toUShort()
        val rom = ByteArray(0x8000)
        rom[0] = 0x23.toByte() // INC HL
        val cpu = createCpu(rom)

        val cycles = cpu.step()

        assertEquals(8, cycles)
        assertEquals(0x1235.toUShort(), registers.hl)
    }

    @Test
    fun increment_16bit_stackPointer() {
        stackPointer.setTo(0x7FFF.toUShort())
        val rom = ByteArray(0x8000)
        rom[0] = 0x33.toByte() // INC SP
        val cpu = createCpu(rom)

        val cycles = cpu.step()

        assertEquals(8, cycles)
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
        val rom = ByteArray(0x8000)
        rom[0] = 0x23.toByte() // INC HL
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(originalFlags, registers.f)
    }

    @Test
    fun decrement_16bit_registerBC() {
        registers.bc = 0x0100.toUShort()
        val rom = ByteArray(0x8000)
        rom[0] = 0x0B.toByte() // DEC BC
        val cpu = createCpu(rom)

        val cycles = cpu.step()

        assertEquals(8, cycles)
        assertEquals(0x00FF.toUShort(), registers.bc)
    }

    @Test
    fun decrement_16bit_registerDE() {
        registers.de = 0x0000.toUShort()
        val rom = ByteArray(0x8000)
        rom[0] = 0x1B.toByte() // DEC DE
        val cpu = createCpu(rom)

        val cycles = cpu.step()

        assertEquals(8, cycles)
        assertEquals(0xFFFF.toUShort(), registers.de)
    }

    @Test
    fun decrement_16bit_registerHL() {
        registers.hl = 0x1234.toUShort()
        val rom = ByteArray(0x8000)
        rom[0] = 0x2B.toByte() // DEC HL
        val cpu = createCpu(rom)

        val cycles = cpu.step()

        assertEquals(8, cycles)
        assertEquals(0x1233.toUShort(), registers.hl)
    }

    @Test
    fun decrement_16bit_stackPointer() {
        stackPointer.setTo(0x8000.toUShort())
        val rom = ByteArray(0x8000)
        rom[0] = 0x3B.toByte() // DEC SP
        val cpu = createCpu(rom)

        val cycles = cpu.step()

        assertEquals(8, cycles)
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
        val rom = ByteArray(0x8000)
        rom[0] = 0x2B.toByte() // DEC HL
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(originalFlags, registers.f)
    }
}
