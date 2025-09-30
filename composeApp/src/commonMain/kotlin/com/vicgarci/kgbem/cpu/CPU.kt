package com.vicgarci.kgbem.cpu

import com.vicgarci.kgbem.cpu.FlagsRegister.Companion.toUByte

class CPU(
    private val registers: Registers,
) {

    fun execute(instruction: Instruction) {
        when (instruction) {
            is Instruction.Add -> add(instruction.target)
        }
    }

    private fun add(target: ArithmeticTarget) {
        val targetValue = getArithmeticTargetValue(target)
        val (sum, carry, halfCarry) = overflowAdd(
            registers.a,
            targetValue,
        )
        registers.a = sum
        val flags = FlagsRegister(
            zero = sum == 0.toUByte(),
            subtract = false,
            halfCarry = halfCarry,
            carry = carry,
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