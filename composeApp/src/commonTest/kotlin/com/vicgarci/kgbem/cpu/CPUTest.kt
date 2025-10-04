package com.vicgarci.kgbem.cpu

import com.vicgarci.kgbem.cpu.FlagsRegister.Companion.toFlagsRegister
import com.vicgarci.kgbem.cpu.FlagsRegister.Companion.toUByte
import kotlin.test.Test
import kotlin.test.assertEquals
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
}