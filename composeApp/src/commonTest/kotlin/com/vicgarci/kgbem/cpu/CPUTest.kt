package com.vicgarci.kgbem.cpu

import com.vicgarci.kgbem.cpu.FlagsRegister.Companion.toFlagsRegister
import com.vicgarci.kgbem.cpu.FlagsRegister.Companion.toUByte
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CPUTest {

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

    private val cpu = CPU(registers)

    @Test
    fun addWithCarry_carryFalse() {
        cpu.execute(Instruction.AddC(ArithmeticTarget.D))

        assertEquals(
            0x00.toUByte(),
            registers.a,
        )
        assertTrue(registers.f.toFlagsRegister().carry)
    }

    @Test
    fun addWithCarry_carryTrue() {
        registers.f = FlagsRegister(
            zero = false,
            subtract = false,
            halfCarry = false,
            carry = true,
        ).toUByte()

        cpu.execute(Instruction.AddC(ArithmeticTarget.D))

        assertEquals(
            0x01.toUByte(),
            registers.a,
        )
        assertTrue(registers.f.toFlagsRegister().carry)
    }

    @Test
    fun subtractWithCarry_carryFalse() {
        registers.a = 0xFF.toUByte()
        registers.d = 0x0F.toUByte()

        cpu.execute(Instruction.Sbc(ArithmeticTarget.D))

        assertEquals(
            0xF0.toUByte(),
            registers.a,
        )
    }

    @Test
    fun subtractWithCarry_carryTrue() {
        registers.a = 0xFF.toUByte()
        registers.d = 0x0F.toUByte()
        registers.f = FlagsRegister(
            zero = false,
            subtract = false,
            halfCarry = false,
            carry = true,
        ).toUByte()

        cpu.execute(Instruction.Sbc(ArithmeticTarget.D))

        assertEquals(
            0xEF.toUByte(),
            registers.a,
        )
    }

    @Test
    fun increment() {
        cpu.execute(Instruction.Inc(ArithmeticTarget.B))

        assertEquals(0x01.toUByte(), registers.b)
        val flags = registers.f.toFlagsRegister()
        assertEquals(false, flags.zero)
        assertFalse(flags.subtract)
        assertFalse(flags.halfCarry)
    }

    @Test
    fun increment_preservesPrevCarry() {
        registers.f = FlagsRegister(
            zero = false,
            subtract = true,
            halfCarry = false,
            carry = true,
        ).toUByte()

        cpu.execute(Instruction.Inc(ArithmeticTarget.B))

        assertEquals(0x01.toUByte(), registers.b)
        val flags = registers.f.toFlagsRegister()
        assertEquals(false, flags.zero)
        assertFalse(flags.halfCarry)
        assertTrue(flags.carry)
    }

    @Test
    fun decrement() {
        cpu.execute(Instruction.Dec(ArithmeticTarget.D))

        assertEquals(0xFE.toUByte(), registers.d)
        val flags = registers.f.toFlagsRegister()
        assertEquals(false, flags.zero)
        assertTrue(flags.subtract)
        assertFalse(flags.halfCarry)
    }

    @Test
    fun rightRotateA_carryTrue_leastSignificantBit1() {
        registers.a = 0b1.toUByte()
        registers.f = FlagsRegister(
            zero = false,
            subtract = false,
            halfCarry = false,
            carry = true,
        ).toUByte()

        cpu.execute(Instruction.Rra)

        assertEquals(0b10000000.toUByte(), registers.a)
        assertTrue(registers.f.toFlagsRegister().carry)
    }

    @Test
    fun rightRotateA_carryTrue_leastSignificantBit0() {
        registers.a = 0xF0.toUByte()
        registers.f = FlagsRegister(
            zero = false,
            subtract = false,
            halfCarry = false,
            carry = true,
        ).toUByte()

        cpu.execute(Instruction.Rra)

        assertEquals(0b11111000.toUByte(), registers.a)
        assertFalse(registers.f.toFlagsRegister().carry)
    }

    @Test
    fun leftRotateA_carryTrue_mostSignificantBit1() {
        registers.a = 0xF0.toUByte()
        registers.f = FlagsRegister(
            zero = false,
            subtract = false,
            halfCarry = false,
            carry = true,
        ).toUByte()

        cpu.execute(Instruction.Rla)

        assertEquals(0b11100001.toUByte(), registers.a)
        assertTrue(registers.f.toFlagsRegister().carry)
    }

    @Test
    fun leftRotateA_carryTrue_mostSignificantBit0() {
        registers.a = 0x0F.toUByte()
        registers.f = FlagsRegister(
            zero = false,
            subtract = false,
            halfCarry = false,
            carry = true,
        ).toUByte()

        cpu.execute(Instruction.Rla)

        assertEquals(0b00011111.toUByte(), registers.a)
        assertFalse(registers.f.toFlagsRegister().carry)
    }
}