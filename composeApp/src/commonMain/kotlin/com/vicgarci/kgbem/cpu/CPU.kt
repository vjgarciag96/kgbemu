package com.vicgarci.kgbem.cpu

import com.vicgarci.kgbem.cpu.FlagsRegister.Companion.toFlagsRegister
import com.vicgarci.kgbem.cpu.FlagsRegister.Companion.toUByte

class CPU(
    private val registers: Registers,
) {

    fun execute(instruction: Instruction) {
        when (instruction) {
            is Instruction.Add -> add(instruction.target)
            is Instruction.AddHl -> addHl(instruction.target)
            is Instruction.AddC -> addC(instruction.target)
            is Instruction.Sub -> sub(instruction.target)
            is Instruction.Sbc -> sbc(instruction.target)
        }
    }

    private fun add(target: ArithmeticTarget) {
        val targetValue = getArithmeticTargetValue(target)
        val (sum, carry, halfCarry) = overflowAdd(
            registers.a,
            targetValue,
        )
        registers.a = sum.toUByte()
        val flags = FlagsRegister(
            zero = sum == 0.toUShort(),
            subtract = false,
            halfCarry = halfCarry,
            carry = carry,
        )
        registers.f = flags.toUByte()
    }

    private fun addHl(target: ArithmeticTarget) {
        val targetValue = getArithmeticTargetValue(target)
        val (sum, carry, halfCarry) = overflowAdd(
            registers.hl,
            targetValue.toUShort(),
        )

        registers.hl = sum
        val flags = FlagsRegister(
            zero = sum == 0.toUShort(),
            subtract = false,
            halfCarry = halfCarry,
            carry = carry,
        )
        registers.f = flags.toUByte()
    }

    private fun addC(
        target: ArithmeticTarget
    ) {
        val targetValue = getArithmeticTargetValue(target)
        val carryToAdd = if (registers.f.toFlagsRegister().carry) 0b1.toUByte() else 0b0.toUByte()
        val (sum, carry, halfCarry) = overflowAdd(
            registers.a.toUShort(),
            (targetValue + carryToAdd).toUShort(),
        )

        registers.a = sum.toUByte()
        val flags = FlagsRegister(
            zero = sum == 0.toUShort(),
            subtract = false,
            halfCarry = halfCarry,
            carry = carry,
        )
        registers.f = flags.toUByte()
    }

    private fun sub(target: ArithmeticTarget) {
        val targetValue = getArithmeticTargetValue(target)
        val (sub, halfBorrow, borrow) = sub(registers.a, targetValue)

        registers.a = sub.toUByte()
        val flags = FlagsRegister(
            zero = sub == 0.toUShort(),
            subtract = true,
            halfCarry = halfBorrow,
            carry = borrow,
        )
        registers.f = flags.toUByte()
    }

    private fun sbc(target: ArithmeticTarget) {
        val targetValue = getArithmeticTargetValue(target)
        val carryToAdd = if (registers.f.toFlagsRegister().carry) 0b1.toUByte() else 0b0.toUByte()

        val (sub, halfBorrow, borrow) = sub(
            registers.a.toUShort(),
            (targetValue + carryToAdd).toUShort(),
        )

        registers.a = sub.toUByte()
        val flags = FlagsRegister(
            zero = sub == 0.toUShort(),
            subtract = true,
            halfCarry = halfBorrow,
            carry = borrow,
        )
        registers.f = flags.toUByte()
    }

    private fun getArithmeticTargetValue(
        target: ArithmeticTarget,
    ): UByte {
        return when (target) {
            ArithmeticTarget.A -> registers.a
            ArithmeticTarget.B -> registers.b
            ArithmeticTarget.C -> registers.c
            ArithmeticTarget.D -> registers.d
            ArithmeticTarget.E -> registers.e
            ArithmeticTarget.H -> registers.h
            ArithmeticTarget.L -> registers.l
        }
    }
}