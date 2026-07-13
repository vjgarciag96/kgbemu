package com.vicgarci.kgbem.cpu

import com.vicgarci.kgbem.cartridge.RomOnlyCartridge
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
    private val stackPointer = StackPointer(0xFFFE.toUShort())

    private fun createCpuWithMemoryBus(rom: ByteArray): Pair<CPU, MemoryBus> {
        val memoryBus = MemoryBus(RomOnlyCartridge(rom))
        val cpu = CPU(registers, programCounter, memoryBus, stackPointer)
        return cpu to memoryBus
    }

    @Test
    fun call_unconditional() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xCD.toByte() // CALL opcode
        rom[1] = 0x34.toByte() // low byte of address
        rom[2] = 0x12.toByte() // high byte of address
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)

        cpu.step()

        val callProgramCounter = programCounter.get()
        assertEquals(0x1234.toUShort(), callProgramCounter)
        val savedHigh = memoryBus.readByte(0xFFFD.toUShort()).toUInt()
        val savedLow = memoryBus.readByte(0xFFFC.toUShort()).toUInt()
        val savedProgramCounter = savedHigh shl 8 or savedLow
        assertEquals(0x0003.toUShort(), savedProgramCounter.toUShort())
    }

    @Test
    fun call_notZero_met() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xC4.toByte() // CALL NZ opcode
        rom[1] = 0x34.toByte()
        rom[2] = 0x12.toByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(zero = false).toUByte()
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)

        cpu.step()

        val callProgramCounter = programCounter.get()
        assertEquals(0x1234.toUShort(), callProgramCounter)
        val savedHigh = memoryBus.readByte(0xFFFD.toUShort()).toUInt()
        val savedLow = memoryBus.readByte(0xFFFC.toUShort()).toUInt()
        val savedProgramCounter = savedHigh shl 8 or savedLow
        assertEquals(0x0003.toUShort(), savedProgramCounter.toUShort())
    }

    @Test
    fun call_notZero_notMet() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xC4.toByte() // CALL NZ opcode
        rom[1] = 0x34.toByte()
        rom[2] = 0x12.toByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(zero = true).toUByte()
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)

        cpu.step()

        val callProgramCounter = programCounter.get()
        assertEquals(0x0003.toUShort(), callProgramCounter)
        assertEquals(0.toUByte(), memoryBus.readByte(0xFFFC.toUShort()))
        assertEquals(0.toUByte(), memoryBus.readByte(0xFFFD.toUShort()))
    }

    @Test
    fun call_zero_met() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xCC.toByte() // CALL Z opcode
        rom[1] = 0x34.toByte()
        rom[2] = 0x12.toByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(zero = true).toUByte()
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)

        cpu.step()

        val callProgramCounter = programCounter.get()
        assertEquals(0x1234.toUShort(), callProgramCounter)
        val savedHigh = memoryBus.readByte(0xFFFD.toUShort()).toUInt()
        val savedLow = memoryBus.readByte(0xFFFC.toUShort()).toUInt()
        val savedProgramCounter = savedHigh shl 8 or savedLow
        assertEquals(0x0003.toUShort(), savedProgramCounter.toUShort())
    }

    @Test
    fun call_zero_notMet() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xCC.toByte() // CALL Z opcode
        rom[1] = 0x34.toByte()
        rom[2] = 0x12.toByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(zero = false).toUByte()
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)

        cpu.step()

        val callProgramCounter = programCounter.get()
        assertEquals(0x0003.toUShort(), callProgramCounter)
        assertEquals(0.toUByte(), memoryBus.readByte(0xFFFC.toUShort()))
        assertEquals(0.toUByte(), memoryBus.readByte(0xFFFD.toUShort()))
    }

    @Test
    fun call_notCarry_met() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xD4.toByte() // CALL NC opcode
        rom[1] = 0x34.toByte()
        rom[2] = 0x12.toByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = false).toUByte()
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)

        cpu.step()

        val callProgramCounter = programCounter.get()
        assertEquals(0x1234.toUShort(), callProgramCounter)
        val savedHigh = memoryBus.readByte(0xFFFD.toUShort()).toUInt()
        val savedLow = memoryBus.readByte(0xFFFC.toUShort()).toUInt()
        val savedProgramCounter = savedHigh shl 8 or savedLow
        assertEquals(0x0003.toUShort(), savedProgramCounter.toUShort())
    }

    @Test
    fun call_notCarry_notMet() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xD4.toByte() // CALL NC opcode
        rom[1] = 0x34.toByte()
        rom[2] = 0x12.toByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = true).toUByte()
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)

        cpu.step()

        val callProgramCounter = programCounter.get()
        assertEquals(0x0003.toUShort(), callProgramCounter)
        assertEquals(0.toUByte(), memoryBus.readByte(0xFFFC.toUShort()))
        assertEquals(0.toUByte(), memoryBus.readByte(0xFFFD.toUShort()))
    }

    @Test
    fun call_carry_met() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xDC.toByte() // CALL C opcode
        rom[1] = 0x34.toByte()
        rom[2] = 0x12.toByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = true).toUByte()
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)

        cpu.step()

        val callProgramCounter = programCounter.get()
        assertEquals(0x1234.toUShort(), callProgramCounter)
        val savedHigh = memoryBus.readByte(0xFFFD.toUShort()).toUInt()
        val savedLow = memoryBus.readByte(0xFFFC.toUShort()).toUInt()
        val savedProgramCounter = savedHigh shl 8 or savedLow
        assertEquals(0x0003.toUShort(), savedProgramCounter.toUShort())
    }

    @Test
    fun call_carry_notMet() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xDC.toByte() // CALL C opcode
        rom[1] = 0x34.toByte()
        rom[2] = 0x12.toByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = false).toUByte()
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)

        cpu.step()

        val callProgramCounter = programCounter.get()
        assertEquals(0x0003.toUShort(), callProgramCounter)
        assertEquals(0.toUByte(), memoryBus.readByte(0xFFFC.toUShort()))
        assertEquals(0.toUByte(), memoryBus.readByte(0xFFFD.toUShort()))
    }
}
