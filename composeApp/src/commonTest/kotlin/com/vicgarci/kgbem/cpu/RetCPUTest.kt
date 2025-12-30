package com.vicgarci.kgbem.cpu

import com.vicgarci.kgbem.cpu.FlagsRegister.Companion.toUByte
import kotlin.test.Test
import kotlin.test.assertEquals

class RetCPUTest {

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
    fun ret_unconditional() {
        memory[0] = 0xC9.toUByte() // RET opcode
        memory[8] = 0x34.toUByte() // low byte
        memory[9] = 0x12.toUByte() // high byte

        cpu.step()

        val retProgramCounter = programCounter.get()
        assertEquals(0x1234.toUShort(), retProgramCounter)
        assertEquals(10.toUShort(), stackPointer.getAndIncrement())
    }

    @Test
    fun ret_notZero_met() {
        memory[0] = 0xC0.toUByte() // RET NZ opcode
        memory[8] = 0x34.toUByte() // low byte
        memory[9] = 0x12.toUByte() // high byte
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.toUByte()

        cpu.step()

        assertEquals(0x1234.toUShort(), programCounter.get())
        assertEquals(10.toUShort(), stackPointer.getAndIncrement())
    }

    @Test
    fun ret_notZero_notMet() {
        memory[0] = 0xC0.toUByte() // RET NZ opcode
        memory[8] = 0x34.toUByte()
        memory[9] = 0x12.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(zero = true).toUByte()

        cpu.step()

        assertEquals(0x0001.toUShort(), programCounter.get())
        assertEquals(8.toUShort(), stackPointer.getAndIncrement())
    }

    @Test
    fun ret_zero_met() {
        memory[0] = 0xC8.toUByte() // RET Z opcode
        memory[8] = 0x34.toUByte()
        memory[9] = 0x12.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(zero = true).toUByte()

        cpu.step()

        assertEquals(0x1234.toUShort(), programCounter.get())
        assertEquals(10.toUShort(), stackPointer.getAndIncrement())
    }

    @Test
    fun ret_zero_notMet() {
        memory[0] = 0xC8.toUByte() // RET Z opcode
        memory[8] = 0x34.toUByte()
        memory[9] = 0x12.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.toUByte()

        cpu.step()

        assertEquals(0x0001.toUShort(), programCounter.get())
        assertEquals(8.toUShort(), stackPointer.getAndIncrement())
    }

    @Test
    fun ret_notCarry_met() {
        memory[0] = 0xD0.toUByte() // RET NC opcode
        memory[8] = 0x34.toUByte()
        memory[9] = 0x12.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.toUByte()

        cpu.step()

        assertEquals(0x1234.toUShort(), programCounter.get())
        assertEquals(10.toUShort(), stackPointer.getAndIncrement())
    }

    @Test
    fun ret_notCarry_notMet() {
        memory[0] = 0xD0.toUByte() // RET NC opcode
        memory[8] = 0x34.toUByte()
        memory[9] = 0x12.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = true).toUByte()

        cpu.step()

        assertEquals(0x0001.toUShort(), programCounter.get())
        assertEquals(8.toUShort(), stackPointer.getAndIncrement())
    }

    @Test
    fun ret_carry_met() {
        memory[0] = 0xD8.toUByte() // RET C opcode
        memory[8] = 0x34.toUByte()
        memory[9] = 0x12.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = true).toUByte()

        cpu.step()

        assertEquals(0x1234.toUShort(), programCounter.get())
        assertEquals(10.toUShort(), stackPointer.getAndIncrement())
    }

    @Test
    fun ret_carry_notMet() {
        memory[0] = 0xD8.toUByte() // RET C opcode
        memory[8] = 0x34.toUByte()
        memory[9] = 0x12.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.toUByte()

        cpu.step()

        assertEquals(0x0001.toUShort(), programCounter.get())
        assertEquals(8.toUShort(), stackPointer.getAndIncrement())
    }
}