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

        val expectedInstruction = Instruction.Add(ArithmeticTarget.B)
        assertEquals(expectedInstruction, instructions)
    }

    @Test
    fun decode_adc() {
        val instructions = InstructionDecoder.decode(
            instructionByte = 0x89.toUByte(),
            prefixed = false,
        )

        val expectedInstruction = Instruction.AddC(ArithmeticTarget.C)
        assertEquals(expectedInstruction, instructions)
    }

    @Test
    fun decode_sub() {
        val instructions = InstructionDecoder.decode(
            instructionByte = 0x92.toUByte(),
            prefixed = false,
        )

        val expectedInstruction = Instruction.Sub(ArithmeticTarget.D)
        assertEquals(expectedInstruction, instructions)
    }

    @Test
    fun decode_sbc() {
        val instructions = InstructionDecoder.decode(
            instructionByte = 0x9B.toUByte(),
            prefixed = false,
        )

        val expectedInstruction = Instruction.Sbc(ArithmeticTarget.E)
        assertEquals(expectedInstruction, instructions)
    }

    @Test
    fun decode_and() {
        val instructions = InstructionDecoder.decode(
            instructionByte = 0xA4.toUByte(),
            prefixed = false,
        )

        val expectedInstruction = Instruction.And(ArithmeticTarget.H)
        assertEquals(expectedInstruction, instructions)
    }

    @Test
    fun decode_xor() {
        val instructions = InstructionDecoder.decode(
            instructionByte = 0xAD.toUByte(),
            prefixed = false,
        )

        val expectedInstruction = Instruction.Xor(ArithmeticTarget.L)
        assertEquals(expectedInstruction, instructions)
    }

    @Test
    fun decode_or() {
        val instructions = InstructionDecoder.decode(
            instructionByte = 0xB7.toUByte(),
            prefixed = false,
        )

        val expectedInstruction = Instruction.Or(ArithmeticTarget.A)
        assertEquals(expectedInstruction, instructions)
    }

    @Test
    fun decode_cp() {
        val instructions = InstructionDecoder.decode(
            instructionByte = 0xB8.toUByte(),
            prefixed = false,
        )

        val expectedInstruction = Instruction.Cp(ArithmeticTarget.B)
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

        assertEquals(Instruction.Inc(ArithmeticTarget.B), instructions[0])
        assertEquals(Instruction.Inc(ArithmeticTarget.C), instructions[1])
        assertEquals(Instruction.Inc(ArithmeticTarget.D), instructions[2])
        assertEquals(Instruction.Inc(ArithmeticTarget.E), instructions[3])
        assertEquals(Instruction.Inc(ArithmeticTarget.H), instructions[4])
        assertEquals(Instruction.Inc(ArithmeticTarget.L), instructions[5])
    }

    @Test
    fun decode_dec() {
        val instructions = listOf(0x05, 0x0D, 0x15, 0x1D, 0x25, 0x2D).map { opcode ->
            InstructionDecoder.decode(
                instructionByte = opcode.toUByte(),
                prefixed = false,
            )
        }

        assertEquals(Instruction.Dec(ArithmeticTarget.B), instructions[0])
        assertEquals(Instruction.Dec(ArithmeticTarget.C), instructions[1])
        assertEquals(Instruction.Dec(ArithmeticTarget.D), instructions[2])
        assertEquals(Instruction.Dec(ArithmeticTarget.E), instructions[3])
        assertEquals(Instruction.Dec(ArithmeticTarget.H), instructions[4])
        assertEquals(Instruction.Dec(ArithmeticTarget.L), instructions[5])
    }
}