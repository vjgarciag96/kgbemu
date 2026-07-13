package com.vicgarci.kgbem.cpu

import com.vicgarci.kgbem.cartridge.RomOnlyCartridge
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
    private val stackPointer = StackPointer(0xFFFC.toUShort())

    private fun createCpuWithMemoryBus(rom: ByteArray): Pair<CPU, MemoryBus> {
        val memoryBus = MemoryBus(RomOnlyCartridge(rom))
        val cpu = CPU(registers, programCounter, memoryBus, stackPointer)
        return cpu to memoryBus
    }

    @Test
    fun ret_unconditional() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xC9.toByte() // RET opcode
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)
        memoryBus.writeByte(0xFFFC.toUShort(), 0x34.toUByte()) // low byte
        memoryBus.writeByte(0xFFFD.toUShort(), 0x12.toUByte()) // high byte

        cpu.step()

        val retProgramCounter = programCounter.get()
        assertEquals(0x1234.toUShort(), retProgramCounter)
        assertEquals(0xFFFE.toUShort(), stackPointer.getAndIncrement())
    }

    @Test
    fun ret_notZero_met() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xC0.toByte() // RET NZ opcode
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.toUByte()
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)
        memoryBus.writeByte(0xFFFC.toUShort(), 0x34.toUByte()) // low byte
        memoryBus.writeByte(0xFFFD.toUShort(), 0x12.toUByte()) // high byte

        cpu.step()

        assertEquals(0x1234.toUShort(), programCounter.get())
        assertEquals(0xFFFE.toUShort(), stackPointer.getAndIncrement())
    }

    @Test
    fun ret_notZero_notMet() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xC0.toByte() // RET NZ opcode
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(zero = true).toUByte()
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)
        memoryBus.writeByte(0xFFFC.toUShort(), 0x34.toUByte())
        memoryBus.writeByte(0xFFFD.toUShort(), 0x12.toUByte())

        cpu.step()

        assertEquals(0x0001.toUShort(), programCounter.get())
        assertEquals(0xFFFC.toUShort(), stackPointer.getAndIncrement())
    }

    @Test
    fun ret_zero_met() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xC8.toByte() // RET Z opcode
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(zero = true).toUByte()
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)
        memoryBus.writeByte(0xFFFC.toUShort(), 0x34.toUByte())
        memoryBus.writeByte(0xFFFD.toUShort(), 0x12.toUByte())

        cpu.step()

        assertEquals(0x1234.toUShort(), programCounter.get())
        assertEquals(0xFFFE.toUShort(), stackPointer.getAndIncrement())
    }

    @Test
    fun ret_zero_notMet() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xC8.toByte() // RET Z opcode
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.toUByte()
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)
        memoryBus.writeByte(0xFFFC.toUShort(), 0x34.toUByte())
        memoryBus.writeByte(0xFFFD.toUShort(), 0x12.toUByte())

        cpu.step()

        assertEquals(0x0001.toUShort(), programCounter.get())
        assertEquals(0xFFFC.toUShort(), stackPointer.getAndIncrement())
    }

    @Test
    fun ret_notCarry_met() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xD0.toByte() // RET NC opcode
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.toUByte()
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)
        memoryBus.writeByte(0xFFFC.toUShort(), 0x34.toUByte())
        memoryBus.writeByte(0xFFFD.toUShort(), 0x12.toUByte())

        cpu.step()

        assertEquals(0x1234.toUShort(), programCounter.get())
        assertEquals(0xFFFE.toUShort(), stackPointer.getAndIncrement())
    }

    @Test
    fun ret_notCarry_notMet() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xD0.toByte() // RET NC opcode
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = true).toUByte()
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)
        memoryBus.writeByte(0xFFFC.toUShort(), 0x34.toUByte())
        memoryBus.writeByte(0xFFFD.toUShort(), 0x12.toUByte())

        cpu.step()

        assertEquals(0x0001.toUShort(), programCounter.get())
        assertEquals(0xFFFC.toUShort(), stackPointer.getAndIncrement())
    }

    @Test
    fun ret_carry_met() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xD8.toByte() // RET C opcode
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = true).toUByte()
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)
        memoryBus.writeByte(0xFFFC.toUShort(), 0x34.toUByte())
        memoryBus.writeByte(0xFFFD.toUShort(), 0x12.toUByte())

        cpu.step()

        assertEquals(0x1234.toUShort(), programCounter.get())
        assertEquals(0xFFFE.toUShort(), stackPointer.getAndIncrement())
    }

    @Test
    fun ret_carry_notMet() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xD8.toByte() // RET C opcode
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.toUByte()
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)
        memoryBus.writeByte(0xFFFC.toUShort(), 0x34.toUByte())
        memoryBus.writeByte(0xFFFD.toUShort(), 0x12.toUByte())

        cpu.step()

        assertEquals(0x0001.toUShort(), programCounter.get())
        assertEquals(0xFFFC.toUShort(), stackPointer.getAndIncrement())
    }
}
