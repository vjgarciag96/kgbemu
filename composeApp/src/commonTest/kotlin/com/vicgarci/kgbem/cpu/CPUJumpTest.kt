package com.vicgarci.kgbem.cpu

import com.vicgarci.kgbem.cartridge.RomOnlyCartridge
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

    private fun createCpu(rom: ByteArray): CPU {
        val memoryBus = MemoryBus(RomOnlyCartridge(rom))
        return CPU(registers, programCounter, memoryBus)
    }

    @Test
    fun jump_notZero_met() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(zero = false).toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0xC2.toByte() // JUMP NZ opcode
        rom[1] = 0xCD.toByte()
        rom[2] = 0xAB.toByte()
        val cpu = createCpu(rom)

        cpu.step()
        val currentPc = programCounter.getAndIncrement()

        assertEquals(0xABCD.toUShort(), currentPc)
    }

    @Test
    fun jump_notZero_notMet() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(zero = true).toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0xC2.toByte() // JP NZ opcode
        rom[1] = 0xCD.toByte()
        rom[2] = 0xAB.toByte()
        val cpu = createCpu(rom)

        cpu.step()
        val currentPc = programCounter.getAndIncrement()

        assertEquals(0x0003.toUShort(), currentPc)
    }

    @Test
    fun jump_zero_met() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(zero = true).toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0xCA.toByte() // JP Z opcode
        rom[1] = 0xCD.toByte()
        rom[2] = 0xAB.toByte()
        val cpu = createCpu(rom)

        cpu.step()
        val currentPc = programCounter.getAndIncrement()

        assertEquals(0xABCD.toUShort(), currentPc)
    }

    @Test
    fun jump_zero_notMet() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(zero = false).toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0xCA.toByte() // JP Z opcode
        rom[1] = 0xCD.toByte()
        rom[2] = 0xAB.toByte()
        val cpu = createCpu(rom)

        cpu.step()
        val currentPc = programCounter.getAndIncrement()

        assertEquals(0x0003.toUShort(), currentPc)
    }

    @Test
    fun jump_carry_met() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = true).toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0xDA.toByte() // JP C opcode
        rom[1] = 0xCD.toByte()
        rom[2] = 0xAB.toByte()
        val cpu = createCpu(rom)

        cpu.step()
        val currentPc = programCounter.getAndIncrement()

        assertEquals(0xABCD.toUShort(), currentPc)
    }

    @Test
    fun jump_carry_notMet() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = false).toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0xDA.toByte() // JP C opcode
        rom[1] = 0xCD.toByte()
        rom[2] = 0xAB.toByte()
        val cpu = createCpu(rom)

        cpu.step()
        val currentPc = programCounter.getAndIncrement()

        assertEquals(0x0003.toUShort(), currentPc)
    }

    @Test
    fun jump_notCarry_met() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = false).toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0xD2.toByte() // JP NC opcode
        rom[1] = 0xCD.toByte()
        rom[2] = 0xAB.toByte()
        val cpu = createCpu(rom)

        cpu.step()
        val currentPc = programCounter.getAndIncrement()

        assertEquals(0xABCD.toUShort(), currentPc)
    }

    @Test
    fun jump_notCarry_notMet() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = true).toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0xD2.toByte() // JP NC opcode
        rom[1] = 0xCD.toByte()
        rom[2] = 0xAB.toByte()
        val cpu = createCpu(rom)

        cpu.step()
        val currentPc = programCounter.getAndIncrement()

        assertEquals(0x0003.toUShort(), currentPc)
    }

    @Test
    fun jump_unconditional() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xC3.toByte() // JP nn opcode
        rom[1] = 0xCD.toByte()
        rom[2] = 0xAB.toByte()
        val cpu = createCpu(rom)

        cpu.step()
        val currentPc = programCounter.getAndIncrement()

        assertEquals(0xABCD.toUShort(), currentPc)
    }

    @Test
    fun jumpRelative_unconditional_positive() {
        val rom = ByteArray(0x8000)
        rom[0] = 0x18.toByte() // JR n opcode
        rom[1] = 0x05.toByte() // +5
        val cpu = createCpu(rom)

        cpu.step()

        val programCounter = programCounter.get()
        assertEquals(0x0007.toUShort(), programCounter)
    }

    @Test
    fun jumpRelative_unconditional_negative() {
        val rom = ByteArray(0x8000)
        rom[0] = 0x18.toByte() // JR n opcode
        rom[1] = 0xFB.toByte() // -5
        val cpu = createCpu(rom)

        cpu.step()

        val programCounter = programCounter.get()
        assertEquals(0xFFFD.toUShort(), programCounter)
    }

    @Test
    fun jumpRelative_notZero_met() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(zero = false).toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0x20.toByte() // JR NZ opcode
        rom[1] = 0x05.toByte() // +5
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x0007.toUShort(), programCounter.get())
    }

    @Test
    fun jumpRelative_notZero_notMet() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(zero = true).toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0x20.toByte() // JR NZ opcode
        rom[1] = 0x05.toByte()
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x0002.toUShort(), programCounter.get())
    }

    @Test
    fun jumpRelative_zero_met() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(zero = true).toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0x28.toByte() // JR Z opcode
        rom[1] = 0x05.toByte() // +5
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x0007.toUShort(), programCounter.get())
    }

    @Test
    fun jumpRelative_zero_notMet() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(zero = false).toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0x28.toByte() // JR Z opcode
        rom[1] = 0x05.toByte()
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x0002.toUShort(), programCounter.get())
    }

    @Test
    fun jumpRelative_notCarry_met() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = false).toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0x30.toByte() // JR NC opcode
        rom[1] = 0x05.toByte() // +5
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x0007.toUShort(), programCounter.get())
    }

    @Test
    fun jumpRelative_notCarry_notMet() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = true).toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0x30.toByte() // JR NC opcode
        rom[1] = 0x05.toByte()
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x0002.toUShort(), programCounter.get())
    }

    @Test
    fun jumpRelative_carry_met() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = true).toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0x38.toByte() // JR C opcode
        rom[1] = 0x05.toByte() // +5
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x0007.toUShort(), programCounter.get())
    }

    @Test
    fun jumpRelative_carry_notMet() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = false).toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0x38.toByte() // JR C opcode
        rom[1] = 0x05.toByte()
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x0002.toUShort(), programCounter.get())
    }
}
