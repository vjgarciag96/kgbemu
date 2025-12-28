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
}