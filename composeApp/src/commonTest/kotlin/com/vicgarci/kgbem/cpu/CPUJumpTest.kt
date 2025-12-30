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
    private val memory = Array(0x10000) { 0.toUByte() }
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
        val currentPc = programCounter.getAndIncrement()

        assertEquals(0xABCD.toUShort(), currentPc)
    }

    @Test
    fun jump_notZero_notMet() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(zero = true).toUByte()
        memory[0] = 0xC2.toUByte() // JP NZ opcode
        memory[1] = 0xCD.toUByte()
        memory[2] = 0xAB.toUByte()

        cpu.step()
        val currentPc = programCounter.getAndIncrement()

        assertEquals(0x0003.toUShort(), currentPc)
    }

    @Test
    fun jump_zero_met() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(zero = true).toUByte()
        memory[0] = 0xCA.toUByte() // JP Z opcode
        memory[1] = 0xCD.toUByte()
        memory[2] = 0xAB.toUByte()

        cpu.step()
        val currentPc = programCounter.getAndIncrement()

        assertEquals(0xABCD.toUShort(), currentPc)
    }

    @Test
    fun jump_zero_notMet() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(zero = false).toUByte()
        memory[0] = 0xCA.toUByte() // JP Z opcode
        memory[1] = 0xCD.toUByte()
        memory[2] = 0xAB.toUByte()

        cpu.step()
        val currentPc = programCounter.getAndIncrement()

        assertEquals(0x0003.toUShort(), currentPc)
    }

    @Test
    fun jump_carry_met() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = true).toUByte()
        memory[0] = 0xDA.toUByte() // JP C opcode
        memory[1] = 0xCD.toUByte()
        memory[2] = 0xAB.toUByte()

        cpu.step()
        val currentPc = programCounter.getAndIncrement()

        assertEquals(0xABCD.toUShort(), currentPc)
    }

    @Test
    fun jump_carry_notMet() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = false).toUByte()
        memory[0] = 0xDA.toUByte() // JP C opcode
        memory[1] = 0xCD.toUByte()
        memory[2] = 0xAB.toUByte()

        cpu.step()
        val currentPc = programCounter.getAndIncrement()

        assertEquals(0x0003.toUShort(), currentPc)
    }

    @Test
    fun jump_notCarry_met() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = false).toUByte()
        memory[0] = 0xD2.toUByte() // JP NC opcode
        memory[1] = 0xCD.toUByte()
        memory[2] = 0xAB.toUByte()

        cpu.step()
        val currentPc = programCounter.getAndIncrement()

        assertEquals(0xABCD.toUShort(), currentPc)
    }

    @Test
    fun jump_notCarry_notMet() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = true).toUByte()
        memory[0] = 0xD2.toUByte() // JP NC opcode
        memory[1] = 0xCD.toUByte()
        memory[2] = 0xAB.toUByte()

        cpu.step()
        val currentPc = programCounter.getAndIncrement()

        assertEquals(0x0003.toUShort(), currentPc)
    }

    @Test
    fun jump_unconditional() {
        memory[0] = 0xC3.toUByte() // JP nn opcode
        memory[1] = 0xCD.toUByte()
        memory[2] = 0xAB.toUByte()

        cpu.step()
        val currentPc = programCounter.getAndIncrement()

        assertEquals(0xABCD.toUShort(), currentPc)
    }

    @Test
    fun jumpRelative_unconditional_positive() {
        memory[0] = 0x18.toUByte() // JR n opcode
        memory[1] = 0x05.toUByte() // +5

        cpu.step()

        val programCounter = programCounter.get()
        assertEquals(0x0007.toUShort(), programCounter)
    }

    @Test
    fun jumpRelative_unconditional_negative() {
        memory[0] = 0x18.toUByte() // JR n opcode
        memory[1] = 0xFB.toUByte() // -5

        cpu.step()

        val programCounter = programCounter.get()
        assertEquals(0xFFFD.toUShort(), programCounter)
    }

    @Test
    fun jumpRelative_notZero_met() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(zero = false).toUByte()
        memory[0] = 0x20.toUByte() // JR NZ opcode
        memory[1] = 0x05.toUByte() // +5

        cpu.step()

        assertEquals(0x0007.toUShort(), programCounter.get())
    }

    @Test
    fun jumpRelative_notZero_notMet() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(zero = true).toUByte()
        memory[0] = 0x20.toUByte() // JR NZ opcode
        memory[1] = 0x05.toUByte()

        cpu.step()

        assertEquals(0x0002.toUShort(), programCounter.get())
    }

    @Test
    fun jumpRelative_zero_met() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(zero = true).toUByte()
        memory[0] = 0x28.toUByte() // JR Z opcode
        memory[1] = 0x05.toUByte() // +5

        cpu.step()

        assertEquals(0x0007.toUShort(), programCounter.get())
    }

    @Test
    fun jumpRelative_zero_notMet() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(zero = false).toUByte()
        memory[0] = 0x28.toUByte() // JR Z opcode
        memory[1] = 0x05.toUByte()

        cpu.step()

        assertEquals(0x0002.toUShort(), programCounter.get())
    }

    @Test
    fun jumpRelative_notCarry_met() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = false).toUByte()
        memory[0] = 0x30.toUByte() // JR NC opcode
        memory[1] = 0x05.toUByte() // +5

        cpu.step()

        assertEquals(0x0007.toUShort(), programCounter.get())
    }

    @Test
    fun jumpRelative_notCarry_notMet() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = true).toUByte()
        memory[0] = 0x30.toUByte() // JR NC opcode
        memory[1] = 0x05.toUByte()

        cpu.step()

        assertEquals(0x0002.toUShort(), programCounter.get())
    }

    @Test
    fun jumpRelative_carry_met() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = true).toUByte()
        memory[0] = 0x38.toUByte() // JR C opcode
        memory[1] = 0x05.toUByte() // +5

        cpu.step()

        assertEquals(0x0007.toUShort(), programCounter.get())
    }

    @Test
    fun jumpRelative_carry_notMet() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = false).toUByte()
        memory[0] = 0x38.toUByte() // JR C opcode
        memory[1] = 0x05.toUByte()

        cpu.step()

        assertEquals(0x0002.toUShort(), programCounter.get())
    }
}