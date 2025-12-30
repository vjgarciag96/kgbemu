package com.vicgarci.kgbem.cpu

import com.vicgarci.kgbem.cpu.FlagsRegister.Companion.toUByte
import kotlin.test.Test
import kotlin.test.assertEquals

class CallCPUTest {

    private val registers = Registers(
        0.toUByte(),
        0.toUByte(),
        0.toUByte(),
        0.toUByte(),
        0.toUByte(),
        0.toUByte(),
        0.toUByte(),
        0.toUByte(),
    )

    private var programCounter = ProgramCounter(0.toUShort())
    private val memory = Array(0x10000) { 0.toUByte() }
    private val memoryBus = MemoryBus(memory)
    private val stackPointer = StackPointer(8.toUShort())

    private val cpu = CPU(
        registers,
        programCounter,
        memoryBus,
        stackPointer,
    )

    @Test
    fun call_unconditional() {
        memory[0] = 0xCD.toUByte() // CALL opcode
        memory[1] = 0x34.toUByte() // low byte of address
        memory[2] = 0x12.toUByte() // high byte of address

        cpu.step()

        val callProgramCounter = programCounter.get()
        assertEquals(0x1234.toUShort(), callProgramCounter)
        val savedProgramCounter = memory[7].toUInt() shl 8 or memory[6].toUInt()
        assertEquals(0x0003.toUShort(), savedProgramCounter.toUShort())
    }

    @Test
    fun call_notZero_met() {
        memory[0] = 0xC4.toUByte() // CALL NZ opcode
        memory[1] = 0x34.toUByte()
        memory[2] = 0x12.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(zero = false).toUByte()

        cpu.step()

        val callProgramCounter = programCounter.get()
        assertEquals(0x1234.toUShort(), callProgramCounter)
        val savedProgramCounter = memory[7].toUInt() shl 8 or memory[6].toUInt()
        assertEquals(0x0003.toUShort(), savedProgramCounter.toUShort())
    }

    @Test
    fun call_notZero_notMet() {
        memory[0] = 0xC4.toUByte() // CALL NZ opcode
        memory[1] = 0x34.toUByte()
        memory[2] = 0x12.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(zero = true).toUByte()

        cpu.step()

        val callProgramCounter = programCounter.get()
        assertEquals(0x0003.toUShort(), callProgramCounter)
        assertEquals(0.toUByte(), memory[6])
        assertEquals(0.toUByte(), memory[7])
    }

    @Test
    fun call_zero_met() {
        memory[0] = 0xCC.toUByte() // CALL Z opcode
        memory[1] = 0x34.toUByte()
        memory[2] = 0x12.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(zero = true).toUByte()

        cpu.step()

        val callProgramCounter = programCounter.get()
        assertEquals(0x1234.toUShort(), callProgramCounter)
        val savedProgramCounter = memory[7].toUInt() shl 8 or memory[6].toUInt()
        assertEquals(0x0003.toUShort(), savedProgramCounter.toUShort())
    }

    @Test
    fun call_zero_notMet() {
        memory[0] = 0xCC.toUByte() // CALL Z opcode
        memory[1] = 0x34.toUByte()
        memory[2] = 0x12.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(zero = false).toUByte()

        cpu.step()

        val callProgramCounter = programCounter.get()
        assertEquals(0x0003.toUShort(), callProgramCounter)
        assertEquals(0.toUByte(), memory[6])
        assertEquals(0.toUByte(), memory[7])
    }

    @Test
    fun call_notCarry_met() {
        memory[0] = 0xD4.toUByte() // CALL NC opcode
        memory[1] = 0x34.toUByte()
        memory[2] = 0x12.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = false).toUByte()

        cpu.step()

        val callProgramCounter = programCounter.get()
        assertEquals(0x1234.toUShort(), callProgramCounter)
        val savedProgramCounter = memory[7].toUInt() shl 8 or memory[6].toUInt()
        assertEquals(0x0003.toUShort(), savedProgramCounter.toUShort())
    }

    @Test
    fun call_notCarry_notMet() {
        memory[0] = 0xD4.toUByte() // CALL NC opcode
        memory[1] = 0x34.toUByte()
        memory[2] = 0x12.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = true).toUByte()

        cpu.step()

        val callProgramCounter = programCounter.get()
        assertEquals(0x0003.toUShort(), callProgramCounter)
        assertEquals(0.toUByte(), memory[6])
        assertEquals(0.toUByte(), memory[7])
    }

    @Test
    fun call_carry_met() {
        memory[0] = 0xDC.toUByte() // CALL C opcode
        memory[1] = 0x34.toUByte()
        memory[2] = 0x12.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = true).toUByte()

        cpu.step()

        val callProgramCounter = programCounter.get()
        assertEquals(0x1234.toUShort(), callProgramCounter)
        val savedProgramCounter = memory[7].toUInt() shl 8 or memory[6].toUInt()
        assertEquals(0x0003.toUShort(), savedProgramCounter.toUShort())
    }

    @Test
    fun call_carry_notMet() {
        memory[0] = 0xDC.toUByte() // CALL C opcode
        memory[1] = 0x34.toUByte()
        memory[2] = 0x12.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = false).toUByte()

        cpu.step()

        val callProgramCounter = programCounter.get()
        assertEquals(0x0003.toUShort(), callProgramCounter)
        assertEquals(0.toUByte(), memory[6])
        assertEquals(0.toUByte(), memory[7])
    }
}