package com.vicgarci.kgbem.cpu

import kotlin.test.Test
import kotlin.test.assertEquals

class InstructionDecoderTest {

    @Test
    fun decode_add() {
        val instructions = InstructionDecoder.decode(
            instructionByte = 0x80.toUByte(),
            prefixed = false,
        )

        val expectedInstruction = Instruction.Add(Register8.B)
        assertEquals(expectedInstruction, instructions)
    }

    @Test
    fun decode_adc() {
        val instructions = InstructionDecoder.decode(
            instructionByte = 0x89.toUByte(),
            prefixed = false,
        )

        val expectedInstruction = Instruction.AddC(Register8.C)
        assertEquals(expectedInstruction, instructions)
    }

    @Test
    fun decode_sub() {
        val instructions = InstructionDecoder.decode(
            instructionByte = 0x92.toUByte(),
            prefixed = false,
        )

        val expectedInstruction = Instruction.Sub(Register8.D)
        assertEquals(expectedInstruction, instructions)
    }

    @Test
    fun decode_sbc() {
        val instructions = InstructionDecoder.decode(
            instructionByte = 0x9B.toUByte(),
            prefixed = false,
        )

        val expectedInstruction = Instruction.Sbc(Register8.E)
        assertEquals(expectedInstruction, instructions)
    }

    @Test
    fun decode_and() {
        val instructions = InstructionDecoder.decode(
            instructionByte = 0xA4.toUByte(),
            prefixed = false,
        )

        val expectedInstruction = Instruction.And(Register8.H)
        assertEquals(expectedInstruction, instructions)
    }

    @Test
    fun decode_xor() {
        val instructions = InstructionDecoder.decode(
            instructionByte = 0xAD.toUByte(),
            prefixed = false,
        )

        val expectedInstruction = Instruction.Xor(Register8.L)
        assertEquals(expectedInstruction, instructions)
    }

    @Test
    fun decode_or() {
        val instructions = InstructionDecoder.decode(
            instructionByte = 0xB7.toUByte(),
            prefixed = false,
        )

        val expectedInstruction = Instruction.Or(Register8.A)
        assertEquals(expectedInstruction, instructions)
    }

    @Test
    fun decode_cp() {
        val instructions = InstructionDecoder.decode(
            instructionByte = 0xB8.toUByte(),
            prefixed = false,
        )

        val expectedInstruction = Instruction.Cp(Register8.B)
        assertEquals(expectedInstruction, instructions)
    }

    @Test
    fun decode_inc() {
        val instructions = listOf(0x04, 0x0C, 0x14, 0x1C, 0x24, 0x2C).map { opcode ->
            InstructionDecoder.decode(
                instructionByte = opcode.toUByte(),
                prefixed = false,
            )
        }

        assertEquals(Instruction.Inc(Register8.B), instructions[0])
        assertEquals(Instruction.Inc(Register8.C), instructions[1])
        assertEquals(Instruction.Inc(Register8.D), instructions[2])
        assertEquals(Instruction.Inc(Register8.E), instructions[3])
        assertEquals(Instruction.Inc(Register8.H), instructions[4])
        assertEquals(Instruction.Inc(Register8.L), instructions[5])
    }

    @Test
    fun decode_dec() {
        val instructions = listOf(0x05, 0x0D, 0x15, 0x1D, 0x25, 0x2D).map { opcode ->
            InstructionDecoder.decode(
                instructionByte = opcode.toUByte(),
                prefixed = false,
            )
        }

        assertEquals(Instruction.Dec(Register8.B), instructions[0])
        assertEquals(Instruction.Dec(Register8.C), instructions[1])
        assertEquals(Instruction.Dec(Register8.D), instructions[2])
        assertEquals(Instruction.Dec(Register8.E), instructions[3])
        assertEquals(Instruction.Dec(Register8.H), instructions[4])
        assertEquals(Instruction.Dec(Register8.L), instructions[5])
    }

    @Test
    fun decode_load_indirects() {
        val ldHlImmediate = InstructionDecoder.decode(
            instructionByte = 0x36.toUByte(),
            prefixed = false,
        )
        val ldBcA = InstructionDecoder.decode(
            instructionByte = 0x02.toUByte(),
            prefixed = false,
        )
        val ldDeA = InstructionDecoder.decode(
            instructionByte = 0x12.toUByte(),
            prefixed = false,
        )
        val ldABc = InstructionDecoder.decode(
            instructionByte = 0x0A.toUByte(),
            prefixed = false,
        )
        val ldADe = InstructionDecoder.decode(
            instructionByte = 0x1A.toUByte(),
            prefixed = false,
        )
        val ldAbsoluteA = InstructionDecoder.decode(
            instructionByte = 0xEA.toUByte(),
            prefixed = false,
        )
        val ldAAbsolute = InstructionDecoder.decode(
            instructionByte = 0xFA.toUByte(),
            prefixed = false,
        )

        assertEquals(Instruction.Ld(Data8, MemoryAtHl), ldHlImmediate)
        assertEquals(Instruction.Ld(Register8.A, MemoryAtRegister16(Register16.BC)), ldBcA)
        assertEquals(Instruction.Ld(Register8.A, MemoryAtRegister16(Register16.DE)), ldDeA)
        assertEquals(Instruction.Ld(MemoryAtRegister16(Register16.BC), Register8.A), ldABc)
        assertEquals(Instruction.Ld(MemoryAtRegister16(Register16.DE), Register8.A), ldADe)
        assertEquals(Instruction.Ld(Register8.A, MemoryAtData16), ldAbsoluteA)
        assertEquals(Instruction.Ld(MemoryAtData16, Register8.A), ldAAbsolute)
    }

    @Test
    fun decode_cb_rotate_shift_group() {
        val rlc = InstructionDecoder.decode(
            instructionByte = 0x00.toUByte(),
            prefixed = true,
        )
        val rrc = InstructionDecoder.decode(
            instructionByte = 0x09.toUByte(),
            prefixed = true,
        )
        val rl = InstructionDecoder.decode(
            instructionByte = 0x12.toUByte(),
            prefixed = true,
        )
        val rr = InstructionDecoder.decode(
            instructionByte = 0x1B.toUByte(),
            prefixed = true,
        )
        val sla = InstructionDecoder.decode(
            instructionByte = 0x24.toUByte(),
            prefixed = true,
        )
        val sra = InstructionDecoder.decode(
            instructionByte = 0x2D.toUByte(),
            prefixed = true,
        )
        val swap = InstructionDecoder.decode(
            instructionByte = 0x35.toUByte(),
            prefixed = true,
        )
        val srl = InstructionDecoder.decode(
            instructionByte = 0x3F.toUByte(),
            prefixed = true,
        )

        assertEquals(Instruction.Rlc(Register8.B), rlc)
        assertEquals(Instruction.Rrc(Register8.C), rrc)
        assertEquals(Instruction.Rl(Register8.D), rl)
        assertEquals(Instruction.Rr(Register8.E), rr)
        assertEquals(Instruction.Sla(Register8.H), sla)
        assertEquals(Instruction.Sra(Register8.L), sra)
        assertEquals(Instruction.Swap(Register8.L), swap)
        assertEquals(Instruction.Srl(Register8.A), srl)
    }

    @Test
    fun decode_cb_bit_res_set_groups() {
        val bit = InstructionDecoder.decode(
            instructionByte = 0x7C.toUByte(),
            prefixed = true,
        )
        val res = InstructionDecoder.decode(
            instructionByte = 0x84.toUByte(),
            prefixed = true,
        )
        val set = InstructionDecoder.decode(
            instructionByte = 0xFF.toUByte(),
            prefixed = true,
        )

        assertEquals(Instruction.Bit(7, Register8.H), bit)
        assertEquals(Instruction.Res(0, Register8.H), res)
        assertEquals(Instruction.Set(7, Register8.A), set)
    }

    @Test
    fun decode_cb_hl_target() {
        val instruction = InstructionDecoder.decode(
            instructionByte = 0x06.toUByte(),
            prefixed = true,
        )

        assertEquals(Instruction.Rlc(MemoryAtHl), instruction)
    }
}
