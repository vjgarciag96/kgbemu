package com.vicgarci.kgbem.cpu

import com.vicgarci.kgbem.cpu.Instruction.Jp

object InstructionDecoder {

    private val INC_DEC_R8_OPCODES = setOf(
        0x04, 0x0C, 0x14, 0x1C, 0x24, 0x2C, 0x3C, // INC
        0x05, 0x0D, 0x15, 0x1D, 0x25, 0x2D, 0x3D, // DEC
    )

    private val INC_DEC_R16_OPCODES = setOf(
        0x03, 0x13, 0x23, 0x33, // INC
        0x0B, 0x1B, 0x2B, 0x3B, // DEC
    )

    private val LD_R8HL_R8_OPCODES = (0x40..0x7F).toSet().minus(0x76)

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
        // Bits 0..2 encode the target register (B,C,D,E,H,L,(HL),A).
        val target = instructionByte.toInt() and 0x07
        val targetRegister = when (target) {
            0b000 -> Register8.B
            0b001 -> Register8.C
            0b010 -> Register8.D
            0b011 -> Register8.E
            0b100 -> Register8.H
            0b101 -> Register8.L
            0b110 -> MemoryAtHl
            0b111 -> Register8.A
            else -> null
        } ?: return null

        // Bits 6..7 select the opcode group; bits 3..5 select op/bit index.
        val opGroup = (instructionByte.toInt() ushr 6) and 0x03
        val operation = (instructionByte.toInt() ushr 3) and 0x07

        return when (opGroup) {
            0b00 -> {
                when (operation) {
                    0b000 -> Instruction.Rlc(targetRegister)
                    0b001 -> Instruction.Rrc(targetRegister)
                    0b010 -> Instruction.Rl(targetRegister)
                    0b011 -> Instruction.Rr(targetRegister)
                    0b100 -> Instruction.Sla(targetRegister)
                    0b101 -> Instruction.Sra(targetRegister)
                    0b110 -> Instruction.Swap(targetRegister)
                    0b111 -> Instruction.Srl(targetRegister)
                    else -> null
                }
            }
            0b01 -> Instruction.Bit(operation, targetRegister)
            0b10 -> Instruction.Res(operation, targetRegister)
            0b11 -> Instruction.Set(operation, targetRegister)
            else -> null
        }
    }

    private fun decodeNotPrefixed(
        instructionByte: UByte,
    ): Instruction? {
        return when (instructionByte.toInt()) {
            0x06 -> Instruction.Ld(Data8, Register8.B)
            0x0E -> Instruction.Ld(Data8, Register8.C)
            0x16 -> Instruction.Ld(Data8, Register8.D)
            0x1E -> Instruction.Ld(Data8, Register8.E)
            0x26 -> Instruction.Ld(Data8, Register8.H)
            0x2E -> Instruction.Ld(Data8, Register8.L)
            0x3E -> Instruction.Ld(Data8, Register8.A)

            0xC1 -> Instruction.Pop(Register16.BC)
            0xD1 -> Instruction.Pop(Register16.DE)
            0xE1 -> Instruction.Pop(Register16.HL)
            0xF1 -> Instruction.Pop(Register16.AF)

            0xC5 -> Instruction.Push(Register16.BC)
            0xD5 -> Instruction.Push(Register16.DE)
            0xE5 -> Instruction.Push(Register16.HL)
            0xF5 -> Instruction.Push(Register16.AF)

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

            0xD9 -> Instruction.RetI

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
                    0b111 -> Register8.A
                    0b000 -> Register8.B
                    0b001 -> Register8.C
                    0b010 -> Register8.D
                    0b011 -> Register8.E
                    0b100 -> Register8.H
                    0b101 -> Register8.L
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

            in INC_DEC_R8_OPCODES -> {
                val opGroup = instructionByte.toInt() and 0xC7
                val target = when (val register = instructionByte.toInt() ushr 3) {
                    0b000 -> Register8.B
                    0b001 -> Register8.C
                    0b010 -> Register8.D
                    0b011 -> Register8.E
                    0b100 -> Register8.H
                    0b101 -> Register8.L
                    0b111 -> Register8.A
                    else -> error("Invalid register for INC/DEC: $register")
                }

                when (opGroup) {
                    0x04 -> Instruction.Inc(target)
                    0x05 -> Instruction.Dec(target)
                    else -> error("Invalid INC/DEC op group: $instructionByte")
                }
            }

            in INC_DEC_R16_OPCODES -> {
                val opGroup = instructionByte.toInt() and 0xCF
                val target = when (val register = (instructionByte.toInt() ushr 4) and 0x03) {
                    0b00 -> Register16.BC
                    0b01 -> Register16.DE
                    0b10 -> Register16.HL
                    0b11 -> Register16.SP
                    else -> error("Invalid register for INC/DEC: $register")
                }

                when (opGroup) {
                    0x03 -> Instruction.Inc(target)
                    0x0B -> Instruction.Dec(target)
                    else -> error("Invalid INC/DEC op group: $instructionByte")
                }
            }

            0xC3 -> Jp(JumpCondition.ALWAYS)
            0xC2 -> Jp(JumpCondition.NOT_ZERO)
            0xCA -> Jp(JumpCondition.ZERO)
            0xD2 -> Jp(JumpCondition.NOT_CARRY)
            0xDA -> Jp(JumpCondition.CARRY)

            0x01 -> Instruction.Ld(Data16, Register16.BC)
            0x11 -> Instruction.Ld(Data16, Register16.DE)
            0x21 -> Instruction.Ld(Data16, Register16.HL)
            0x31 -> Instruction.Ld(Data16, Register16.SP)

            in LD_R8HL_R8_OPCODES -> {
                val source = instructionByte.toInt() and 0x07
                val target = (instructionByte.toInt() ushr 3) and 0x07

                val sourceRegister = when (source) {
                    0b111 -> Register8.A
                    0b000 -> Register8.B
                    0b001 -> Register8.C
                    0b010 -> Register8.D
                    0b011 -> Register8.E
                    0b100 -> Register8.H
                    0b101 -> Register8.L
                    0b110 -> MemoryAtHl
                    else -> error("Invalid source register $source")
                }

                val targetRegister = when (target) {
                    0b111 -> Register8.A
                    0b000 -> Register8.B
                    0b001 -> Register8.C
                    0b010 -> Register8.D
                    0b011 -> Register8.E
                    0b100 -> Register8.H
                    0b101 -> Register8.L
                    0b110 -> MemoryAtHl
                    else -> error("Invalid target register $target")
                }

                Instruction.Ld(sourceRegister, targetRegister)
            }

            0x22 -> Instruction.LdIncHLA
            0x2A -> Instruction.LdIncAHL
            0x32 -> Instruction.LdDecHLA
            0x3A -> Instruction.LdDecAHL

            0x2F -> Instruction.Cpl

            0x3F -> Instruction.Ccf
            0x37 -> Instruction.Scf

            0x27 -> Instruction.Daa

            0x76 -> Instruction.Halt
            0xF3 -> Instruction.DisableInterrupts
            0xFB -> Instruction.EnableInterrupts

            0x00 -> Instruction.Nop

            else -> null
        }
    }
}
