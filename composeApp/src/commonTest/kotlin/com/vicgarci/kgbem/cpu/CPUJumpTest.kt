package com.vicgarci.kgbem.cpu

import com.vicgarci.kgbem.cpu.FlagsRegister.Companion.toUByte
import kotlin.test.Test
import kotlin.test.assertEquals

class CPUJumpTest {

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

    private var programCounter = ProgramCounter(0.toUShort())
    private val memory = Array(3) { 0.toUByte() }
    private val memoryBus = MemoryBus(memory)

    private val cpu = CPU(
        registers,
        programCounter,
        memoryBus,
    )

    @Test
    fun jump_notZero_met() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(zero = false).toUByte()
        memory[0] = 0xC2.toUByte() // JUMP NZ opcode
        memory[1] = 0xCD.toUByte()
        memory[2] = 0xAB.toUByte()

        cpu.step()
        val currentPc = programCounter.next()

        assertEquals(0xABCD.toUShort(), currentPc)
    }

    @Test
    fun jump_notZero_notMet() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(zero = true).toUByte()
        memory[0] = 0xC2.toUByte() // JP NZ opcode
        memory[1] = 0xCD.toUByte()
        memory[2] = 0xAB.toUByte()

        cpu.step()
        val currentPc = programCounter.next()

        assertEquals(0x0003.toUShort(), currentPc)
    }

    @Test
    fun jump_zero_met() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(zero = true).toUByte()
        memory[0] = 0xCA.toUByte() // JP Z opcode
        memory[1] = 0xCD.toUByte()
        memory[2] = 0xAB.toUByte()

        cpu.step()
        val currentPc = programCounter.next()

        assertEquals(0xABCD.toUShort(), currentPc)
    }

    @Test
    fun jump_zero_notMet() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(zero = false).toUByte()
        memory[0] = 0xCA.toUByte() // JP Z opcode
        memory[1] = 0xCD.toUByte()
        memory[2] = 0xAB.toUByte()

        cpu.step()
        val currentPc = programCounter.next()

        assertEquals(0x0003.toUShort(), currentPc)
    }

    @Test
    fun jump_carry_met() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = true).toUByte()
        memory[0] = 0xDA.toUByte() // JP C opcode
        memory[1] = 0xCD.toUByte()
        memory[2] = 0xAB.toUByte()

        cpu.step()
        val currentPc = programCounter.next()

        assertEquals(0xABCD.toUShort(), currentPc)
    }

    @Test
    fun jump_carry_notMet() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = false).toUByte()
        memory[0] = 0xDA.toUByte() // JP C opcode
        memory[1] = 0xCD.toUByte()
        memory[2] = 0xAB.toUByte()

        cpu.step()
        val currentPc = programCounter.next()

        assertEquals(0x0003.toUShort(), currentPc)
    }

    @Test
    fun jump_notCarry_met() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = false).toUByte()
        memory[0] = 0xD2.toUByte() // JP NC opcode
        memory[1] = 0xCD.toUByte()
        memory[2] = 0xAB.toUByte()

        cpu.step()
        val currentPc = programCounter.next()

        assertEquals(0xABCD.toUShort(), currentPc)
    }

    @Test
    fun jump_notCarry_notMet() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = true).toUByte()
        memory[0] = 0xD2.toUByte() // JP NC opcode
        memory[1] = 0xCD.toUByte()
        memory[2] = 0xAB.toUByte()

        cpu.step()
        val currentPc = programCounter.next()

        assertEquals(0x0003.toUShort(), currentPc)
    }

    @Test
    fun jump_unconditional() {
        memory[0] = 0xC3.toUByte() // JP nn opcode
        memory[1] = 0xCD.toUByte()
        memory[2] = 0xAB.toUByte()

        cpu.step()
        val currentPc = programCounter.next()

        assertEquals(0xABCD.toUShort(), currentPc)
    }
}