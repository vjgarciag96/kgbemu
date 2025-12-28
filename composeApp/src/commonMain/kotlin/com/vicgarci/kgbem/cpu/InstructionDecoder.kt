package com.vicgarci.kgbem.cpu

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

            0x18 -> Instruction.Jr(JumpCondition.ALWAYS)
            0x20 -> Instruction.Jr(JumpCondition.NOT_ZERO)
            0x28 -> Instruction.Jr(JumpCondition.ZERO)
            0x30 -> Instruction.Jr(JumpCondition.NOT_CARRY)
            0x38 -> Instruction.Jr(JumpCondition.CARRY)

            0xC7 -> Instruction.Rst(0x00.toUByte())
            0xCF -> Instruction.Rst(0x08.toUByte())
            0xD7 -> Instruction.Rst(0x10.toUByte())
            0xDF -> Instruction.Rst(0x18.toUByte())
            0xE7 -> Instruction.Rst(0x20.toUByte())
            0xEF -> Instruction.Rst(0x28.toUByte())
            0xF7 -> Instruction.Rst(0x30.toUByte())
            0xFF -> Instruction.Rst(0x38.toUByte())

            in 0x80..0xBF -> {
                val opGroup = (instructionByte.toInt() shr 3) and 0x07
                val target = instructionByte.toInt() and 0x07

                val register = when (target) {
                    0b111 -> ArithmeticTarget.A
                    0b000 -> ArithmeticTarget.B
                    0b001 -> ArithmeticTarget.C
                    0b010 -> ArithmeticTarget.D
                    0b011 -> ArithmeticTarget.E
                    0b100 -> ArithmeticTarget.H
                    0b101 -> ArithmeticTarget.L
                    else -> error("Invalid register $target")
                }

                when (opGroup) {
                    0b000 -> Instruction.Add(register)
                    0b001 -> Instruction.AddC(register)
                    0b010 -> Instruction.Sub(register)
                    0b011 -> Instruction.Sbc(register)
                    0b100 -> Instruction.And(register)
                    0b101 -> Instruction.Xor(register)
                    0b110 -> Instruction.Or(register)
                    0b111 -> Instruction.Cp(register)
                    else -> error("Invalid operation group $opGroup")
                }
            }
            0xA8 -> Instruction.Xor(ArithmeticTarget.B)
            0xA9 -> Instruction.Xor(ArithmeticTarget.C)
            0xAA -> Instruction.Xor(ArithmeticTarget.D)
            0xAB -> Instruction.Xor(ArithmeticTarget.E)
            0xAC -> Instruction.Xor(ArithmeticTarget.H)
            0xAD -> Instruction.Xor(ArithmeticTarget.L)

            0xC3 -> Jp(JumpCondition.ALWAYS)
            0xC2 -> Jp(JumpCondition.NOT_ZERO)
            0xCA -> Jp(JumpCondition.ZERO)
            0xD2 -> Jp(JumpCondition.NOT_CARRY)
            0xDA -> Jp(JumpCondition.CARRY)

            else -> null
        }
    }
}