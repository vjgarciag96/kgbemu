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
            is Instruction.And -> and(instruction.target)
            is Instruction.Or -> or(instruction.target)
            is Instruction.Xor -> xor(instruction.target)
            is Instruction.Cp -> cp(instruction.target)
            is Instruction.Inc -> inc(instruction.target)
            is Instruction.Dec -> dec(instruction.target)
            Instruction.Ccf -> ccf()
            Instruction.Scf -> scf()
            Instruction.Rra -> rra()
            Instruction.Rla -> rla()
            Instruction.Cpl -> cpl()
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

    private fun and(target: ArithmeticTarget) {
        val targetValue = getArithmeticTargetValue(target)
        registers.a = registers.a and targetValue
    }

    private fun or(target: ArithmeticTarget) {
        val targetValue = getArithmeticTargetValue(target)
        registers.a = registers.a or targetValue
    }

    private fun xor(target: ArithmeticTarget) {
        val targetValue = getArithmeticTargetValue(target)
        registers.a = registers.a xor targetValue
    }

    private fun cp(target: ArithmeticTarget) {
        val targetValue = getArithmeticTargetValue(target)

        val (sub, halfBorrow, borrow) = sub(registers.a, targetValue)
        val flags = FlagsRegister(
            zero = sub == 0.toUShort(),
            subtract = true,
            halfCarry = halfBorrow,
            carry = borrow,
        )
        registers.f = flags.toUByte()
    }

    private fun inc(target: ArithmeticTarget) {
        updateArithmeticTarget(target) { register ->
            val (sum, _, halfCarry) = overflowAdd(register, 0x1.toUByte())

            val flags = registers.f.toFlagsRegister().copy(
                zero = sum == 0x0.toUShort(),
                subtract = false,
                halfCarry = halfCarry,
            )
            registers.f = flags.toUByte()

            sum.toUByte()
        }
    }

    private fun dec(target: ArithmeticTarget) {
        updateArithmeticTarget(target) { register ->
            val (sub, halfBorrow, _) = sub(register, 0x1.toUByte())

            val flags = registers.f.toFlagsRegister().copy(
                zero = sub == 0x0.toUShort(),
                subtract = true,
                halfCarry = halfBorrow,
            )
            registers.f = flags.toUByte()

            sub.toUByte()
        }
    }

    private fun ccf() {
        val flags = registers.f.toFlagsRegister()
        registers.f = flags.copy(
            subtract = false,
            halfCarry = false,
            carry = !flags.carry,
        ).toUByte()
    }

    private fun scf() {
        val flags = registers.f.toFlagsRegister()
        registers.f = flags.copy(
            subtract = false,
            halfCarry = false,
            carry = true,
        ).toUByte()
    }

    private fun rra() {
        val leastSignificantBit = registers.a and 0b1.toUByte()
        val flags = registers.f.toFlagsRegister()
        val carryBit = if (flags.carry) 0b1 else 0b0
        val rotatedA = registers.a.toInt() ushr 1
        val rotatedCarry = carryBit shl 7
        registers.a = ((rotatedA or rotatedCarry) and 0xFF).toUByte()
        registers.f = flags.copy(
            carry = leastSignificantBit == 0b1.toUByte(),
        ).toUByte()
    }

    private fun rla() {
        val mostSignificantBit = (registers.a and (0b1 shl 7).toUByte()).toInt() ushr 7
        val flags = registers.f.toFlagsRegister()
        val carryBit = if (flags.carry) 0b1 else 0b0
        val rotatedA = registers.a.toInt() shl 1
        registers.a = ((rotatedA or carryBit) and 0xFF).toUByte()
        registers.f = flags.copy(
            carry = (mostSignificantBit and 0xFF).toUByte() == 0b1.toUByte(),
        ).toUByte()
    }

    private fun cpl() {
        registers.a = registers.a.inv()
    }

    private fun updateArithmeticTarget(
        target: ArithmeticTarget,
        update: (UByte) -> UByte,
    ) {
        when (target) {
            ArithmeticTarget.A -> registers.a = update(registers.a)
            ArithmeticTarget.B -> registers.b = update(registers.b)
            ArithmeticTarget.C -> registers.c = update(registers.c)
            ArithmeticTarget.D -> registers.d = update(registers.d)
            ArithmeticTarget.E -> registers.e = update(registers.e)
            ArithmeticTarget.H -> registers.h = update(registers.h)
            ArithmeticTarget.L -> registers.l = update(registers.l)
        }
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