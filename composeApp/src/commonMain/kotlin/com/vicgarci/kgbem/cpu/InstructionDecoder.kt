package com.vicgarci.kgbem.cpu

import com.vicgarci.kgbem.cpu.Instruction.Dec
import com.vicgarci.kgbem.cpu.Instruction.Inc
import com.vicgarci.kgbem.cpu.Instruction.Jp

object InstructionDecoder {

    fun decode(
        instructionByte: UByte,
        prefixed: Boolean,
    ): Instruction? {
        return if (prefixed) {
            decodePrefixed(instructionByte)
        } else {
            decodeNotPrefixed(instructionByte)
        }
    }

    private fun decodePrefixed(instructionByte: UByte): Instruction? {
        return null
    }

    private fun decodeNotPrefixed(
        instructionByte: UByte,
    ): Instruction? {
        return when (instructionByte.toInt()) {
            0x06 -> Instruction.Ld(ArithmeticTarget.B)
            0x0E -> Instruction.Ld(ArithmeticTarget.C)
            0x16 -> Instruction.Ld(ArithmeticTarget.D)
            0x1E -> Instruction.Ld(ArithmeticTarget.E)
            0x26 -> Instruction.Ld(ArithmeticTarget.H)
            0x2E -> Instruction.Ld(ArithmeticTarget.L)
            0x3E -> Instruction.Ld(ArithmeticTarget.A)

            0xC1 -> Instruction.Pop(StackTarget.BC)
            0xD1 -> Instruction.Pop(StackTarget.DE)
            0xE1 -> Instruction.Pop(StackTarget.HL)
            0xF1 -> Instruction.Pop(StackTarget.AF)

            0xC5 -> Instruction.Push(StackTarget.BC)
            0xD5 -> Instruction.Push(StackTarget.DE)
            0xE5 -> Instruction.Push(StackTarget.HL)
            0xF5 -> Instruction.Push(StackTarget.AF)

            0xCD -> Instruction.Call(JumpCondition.ALWAYS)
            0xC4 -> Instruction.Call(JumpCondition.NOT_ZERO)
            0xCC -> Instruction.Call(JumpCondition.ZERO)
            0xD4 -> Instruction.Call(JumpCondition.NOT_CARRY)
            0xDC -> Instruction.Call(JumpCondition.CARRY)

            0xC9 -> Instruction.Ret(JumpCondition.ALWAYS)
            0xC0 -> Instruction.Ret(JumpCondition.NOT_ZERO)
            0xC8 -> Instruction.Ret(JumpCondition.ZERO)
            0xD0 -> Instruction.Ret(JumpCondition.NOT_CARRY)
            0xD8 -> Instruction.Ret(JumpCondition.CARRY)

            0x3C,
            0x04,
            0x0C,
            0x14,
            0x1C,
            0x24,
            0x34,
            0x2C,
                -> Inc(getRegister(instructionByte))

            0x3D,
            0x05,
            0x0D,
            0x15,
            0x1D,
            0x25,
            0x2D,
            0x35,
                -> Dec(getRegister(instructionByte))

            0xC3 -> Jp(JumpCondition.ALWAYS)
            0xC2 -> Jp(JumpCondition.NOT_ZERO)
            0xCA -> Jp(JumpCondition.ZERO)
            0xD2 -> Jp(JumpCondition.NOT_CARRY)
            0xDA -> Jp(JumpCondition.CARRY)

            else -> null
        }
    }

    private fun getRegister(
        instructionByte: UByte,
    ): ArithmeticTarget {
        val register = (instructionByte.toInt() shr 3) and 0b111

        return when (register) {
            0b111 -> ArithmeticTarget.A
            0b000 -> ArithmeticTarget.B
            0b001 -> ArithmeticTarget.C
            0b010 -> ArithmeticTarget.D
            0b011 -> ArithmeticTarget.E
            0b100 -> ArithmeticTarget.H
            0b101 -> ArithmeticTarget.L
            else -> error("Invalid register value $register")
        }
    }
}