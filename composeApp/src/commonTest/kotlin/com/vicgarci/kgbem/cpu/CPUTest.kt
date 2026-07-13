package com.vicgarci.kgbem.cpu

import com.vicgarci.kgbem.cartridge.RomOnlyCartridge
import com.vicgarci.kgbem.cpu.FlagsRegister.Companion.toFlagsRegister
import com.vicgarci.kgbem.cpu.FlagsRegister.Companion.toUByte
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CPUTest {

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
    private val stackPointer = StackPointer(0xFFFF.toUShort())

    private fun createCpu(rom: ByteArray): CPU {
        val memoryBus = MemoryBus(RomOnlyCartridge(rom))
        return CPU(registers, programCounter, memoryBus, stackPointer)
    }

    private fun createCpuWithMemoryBus(rom: ByteArray): Pair<CPU, MemoryBus> {
        val memoryBus = MemoryBus(RomOnlyCartridge(rom))
        val cpu = CPU(registers, programCounter, memoryBus, stackPointer)
        return cpu to memoryBus
    }

    @Test
    fun add_immediate() {
        registers.a = 0x10.toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0xC6.toByte() // ADD A, n opcode
        rom[1] = 0x05.toByte()
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x15.toUByte(), registers.a)
    }

    @Test
    fun adc_immediate() {
        registers.a = 0x10.toUByte()
        registers.f = FlagsRegister(
            zero = false,
            subtract = false,
            halfCarry = false,
            carry = true,
        ).toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0xCE.toByte() // ADC A, n opcode
        rom[1] = 0x01.toByte()
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x12.toUByte(), registers.a)
    }

    @Test
    fun sub_immediate() {
        registers.a = 0x10.toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0xD6.toByte() // SUB n opcode
        rom[1] = 0x01.toByte()
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x0F.toUByte(), registers.a)
    }

    @Test
    fun sbc_immediate() {
        registers.a = 0x10.toUByte()
        registers.f = FlagsRegister(
            zero = false,
            subtract = false,
            halfCarry = false,
            carry = true,
        ).toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0xDE.toByte() // SBC A, n opcode
        rom[1] = 0x01.toByte()
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x0E.toUByte(), registers.a)
    }

    @Test
    fun and_immediate() {
        registers.a = 0xF0.toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0xE6.toByte() // AND n opcode
        rom[1] = 0x0F.toByte()
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x00.toUByte(), registers.a)
    }

    @Test
    fun xor_immediate() {
        registers.a = 0xFF.toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0xEE.toByte() // XOR n opcode
        rom[1] = 0x0F.toByte()
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0xF0.toUByte(), registers.a)
    }

    @Test
    fun or_immediate() {
        registers.a = 0xF0.toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0xF6.toByte() // OR n opcode
        rom[1] = 0x0F.toByte()
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0xFF.toUByte(), registers.a)
    }

    @Test
    fun cp_immediate() {
        registers.a = 0x10.toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0xFE.toByte() // CP n opcode
        rom[1] = 0x10.toByte()
        val cpu = createCpu(rom)

        cpu.step()

        assertTrue(registers.f.toFlagsRegister().zero)
        assertEquals(0x10.toUByte(), registers.a)
    }

    @Test
    fun add_sp_signedOffset() {
        stackPointer.setTo(0xFFF0.toUShort())
        val rom = ByteArray(0x8000)
        rom[0] = 0xE8.toByte() // ADD SP, e8 opcode
        rom[1] = 0x10.toByte() // +16
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x0000.toUShort(), stackPointer.get())
        assertFalse(registers.f.toFlagsRegister().zero)
        assertFalse(registers.f.toFlagsRegister().subtract)
        assertTrue(registers.f.toFlagsRegister().carry)
        assertFalse(registers.f.toFlagsRegister().halfCarry)
    }

    @Test
    fun add_sp_wrapAround_setsCarryAndHalfCarry() {
        stackPointer.setTo(0xFFFE.toUShort())
        val rom = ByteArray(0x8000)
        rom[0] = 0xE8.toByte() // ADD SP, e8 opcode
        rom[1] = 0x0F.toByte() // +15
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x000D.toUShort(), stackPointer.get())
        assertFalse(registers.f.toFlagsRegister().zero)
        assertFalse(registers.f.toFlagsRegister().subtract)
        assertTrue(registers.f.toFlagsRegister().carry)
        assertTrue(registers.f.toFlagsRegister().halfCarry)
    }

    @Test
    fun load_hl_sp_signedOffset() {
        stackPointer.setTo(0x0001.toUShort())
        val rom = ByteArray(0x8000)
        rom[0] = 0xF8.toByte() // LD HL, SP+e8 opcode
        rom[1] = 0xFE.toByte() // -2
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0xFFFF.toUShort(), registers.hl)
        assertEquals(0x0001.toUShort(), stackPointer.get())
        assertFalse(registers.f.toFlagsRegister().zero)
        assertFalse(registers.f.toFlagsRegister().subtract)
        assertFalse(registers.f.toFlagsRegister().carry)
        assertFalse(registers.f.toFlagsRegister().halfCarry)
    }

    @Test
    fun load_hl_sp_halfCarry_set() {
        stackPointer.setTo(0xFFFE.toUShort())
        val rom = ByteArray(0x8000)
        rom[0] = 0xF8.toByte() // LD HL, SP+e8 opcode
        rom[1] = 0x0F.toByte() // +15
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x000D.toUShort(), registers.hl)
        assertFalse(registers.f.toFlagsRegister().zero)
        assertFalse(registers.f.toFlagsRegister().subtract)
        assertTrue(registers.f.toFlagsRegister().carry)
        assertTrue(registers.f.toFlagsRegister().halfCarry)
    }

    @Test
    fun addHlBc_halfCarry() {
        registers.hl = 0x0FFF.toUShort()
        registers.bc = 0x0001.toUShort()
        val rom = ByteArray(0x8000)
        rom[0] = 0x09.toByte() // ADD HL, BC opcode
        registers.f = FlagsRegister(
            zero = true,
            subtract = true,
            halfCarry = false,
            carry = false,
        ).toUByte()
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x1000.toUShort(), registers.hl)
        val flags = registers.f.toFlagsRegister()
        assertTrue(flags.zero)
        assertFalse(flags.subtract)
        assertTrue(flags.halfCarry)
        assertFalse(flags.carry)
    }

    @Test
    fun addHlSp_carry() {
        registers.hl = 0xFFFF.toUShort()
        stackPointer.setTo(0x0001.toUShort())
        val rom = ByteArray(0x8000)
        rom[0] = 0x39.toByte() // ADD HL, SP opcode
        registers.f = FlagsRegister(
            zero = true,
            subtract = true,
            halfCarry = false,
            carry = false,
        ).toUByte()
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x0000.toUShort(), registers.hl)
        val flags = registers.f.toFlagsRegister()
        assertTrue(flags.zero)
        assertFalse(flags.subtract)
        assertTrue(flags.halfCarry)
        assertTrue(flags.carry)
    }

    @Test
    fun loadSpHl() {
        registers.hl = 0x2468.toUShort()
        val rom = ByteArray(0x8000)
        rom[0] = 0xF9.toByte() // LD SP, HL opcode
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x2468.toUShort(), stackPointer.get())
    }

    @Test
    fun storeSpAtImmediate() {
        stackPointer.setTo(0xBEEF.toUShort())
        val rom = ByteArray(0x8000)
        rom[0] = 0x08.toByte() // LD (nn), SP opcode
        rom[1] = 0x00.toByte() // low byte
        rom[2] = 0xC0.toByte() // high byte -> target 0xC000 (WRAM, non-ROM)
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)

        cpu.step()

        assertEquals(0xEF.toUByte(), memoryBus.readByte(0xC000.toUShort()))
        assertEquals(0xBE.toUByte(), memoryBus.readByte(0xC001.toUShort()))
    }

    @Test
    fun storeSpAtImmediate_wrapsAddress() {
        stackPointer.setTo(0x1234.toUShort())
        val rom = ByteArray(0x8000)
        rom[0] = 0x08.toByte() // LD (nn), SP opcode
        rom[1] = 0xFF.toByte() // low byte
        rom[2] = 0xFF.toByte() // high byte -> target 0xFFFF
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)

        cpu.step()

        assertEquals(0x34.toUByte(), memoryBus.readByte(0xFFFF.toUShort()))
        // Address 0x0000 wraps around and is in ROM range — write goes to cartridge (no-op for RomOnly).
        // So we verify the 0xFFFF write succeeded instead of checking the wrapped write at 0x0000.
    }

    @Test
    fun jump_hl_setsProgramCounter() {
        registers.hl = 0x2345.toUShort()
        val rom = ByteArray(0x8000)
        rom[0] = 0xE9.toByte() // JP (HL) opcode
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x2345.toUShort(), programCounter.get())
    }

    @Test
    fun addWithCarry_carryFalse() {
        val cpu = createCpu(ByteArray(0x8000))
        cpu.execute(Instruction.AddC(Register8.D))

        assertEquals(
            0x00.toUByte(),
            registers.a,
        )
        assertTrue(registers.f.toFlagsRegister().carry)
    }

    @Test
    fun addWithCarry_carryTrue() {
        registers.f = FlagsRegister(
            zero = false,
            subtract = false,
            halfCarry = false,
            carry = true,
        ).toUByte()
        val cpu = createCpu(ByteArray(0x8000))

        cpu.execute(Instruction.AddC(Register8.D))

        assertEquals(
            0x01.toUByte(),
            registers.a,
        )
        assertTrue(registers.f.toFlagsRegister().carry)
    }

    @Test
    fun subtractWithCarry_carryFalse() {
        registers.a = 0xFF.toUByte()
        registers.d = 0x0F.toUByte()
        val cpu = createCpu(ByteArray(0x8000))

        cpu.execute(Instruction.Sbc(Register8.D))

        assertEquals(
            0xF0.toUByte(),
            registers.a,
        )
    }

    @Test
    fun subtractWithCarry_carryTrue() {
        registers.a = 0xFF.toUByte()
        registers.d = 0x0F.toUByte()
        registers.f = FlagsRegister(
            zero = false,
            subtract = false,
            halfCarry = false,
            carry = true,
        ).toUByte()
        val cpu = createCpu(ByteArray(0x8000))

        cpu.execute(Instruction.Sbc(Register8.D))

        assertEquals(
            0xEF.toUByte(),
            registers.a,
        )
    }


    @Test
    fun rightRotateAThroughCarry_carryTrue_leastSignificantBit1() {
        registers.a = 0b1.toUByte()
        registers.f = FlagsRegister(
            zero = false,
            subtract = false,
            halfCarry = false,
            carry = true,
        ).toUByte()
        val cpu = createCpu(ByteArray(0x8000))

        cpu.execute(Instruction.Rra)

        assertEquals(0b10000000.toUByte(), registers.a)
        assertTrue(registers.f.toFlagsRegister().carry)
    }

    @Test
    fun rightRotateAThroughCarry_carryTrue_leastSignificantBit0() {
        registers.a = 0xF0.toUByte()
        registers.f = FlagsRegister(
            zero = false,
            subtract = false,
            halfCarry = false,
            carry = true,
        ).toUByte()
        val cpu = createCpu(ByteArray(0x8000))

        cpu.execute(Instruction.Rra)

        assertEquals(0b11111000.toUByte(), registers.a)
        assertFalse(registers.f.toFlagsRegister().carry)
    }

    @Test
    fun leftRotateAThroughCarry_carryTrue_mostSignificantBit1() {
        registers.a = 0xF0.toUByte()
        registers.f = FlagsRegister(
            zero = false,
            subtract = false,
            halfCarry = false,
            carry = true,
        ).toUByte()
        val cpu = createCpu(ByteArray(0x8000))

        cpu.execute(Instruction.Rla)

        assertEquals(0b11100001.toUByte(), registers.a)
        assertTrue(registers.f.toFlagsRegister().carry)
    }

    @Test
    fun leftRotateAThroughCarry_carryTrue_mostSignificantBit0() {
        registers.a = 0x0F.toUByte()
        registers.f = FlagsRegister(
            zero = false,
            subtract = false,
            halfCarry = false,
            carry = true,
        ).toUByte()
        val cpu = createCpu(ByteArray(0x8000))

        cpu.execute(Instruction.Rla)

        assertEquals(0b00011111.toUByte(), registers.a)
        assertFalse(registers.f.toFlagsRegister().carry)
    }

    @Test
    fun rightRotateA_carryTrue_leastSignificantBit0() {
        registers.a = 0xF0.toUByte()
        registers.f = FlagsRegister(
            zero = false,
            subtract = false,
            halfCarry = false,
            carry = true,
        ).toUByte()
        val cpu = createCpu(ByteArray(0x8000))

        cpu.execute(Instruction.Rrca)

        assertEquals(0b01111000.toUByte(), registers.a)
        assertFalse(registers.f.toFlagsRegister().carry)
    }

    @Test
    fun rightRotateA_carryFalse_leastSignificantBit1() {
        registers.a = 0x0F.toUByte()
        registers.f = FlagsRegister(
            zero = false,
            subtract = false,
            halfCarry = false,
            carry = false,
        ).toUByte()
        val cpu = createCpu(ByteArray(0x8000))

        cpu.execute(Instruction.Rrca)

        assertEquals(0b10000111.toUByte(), registers.a)
        assertTrue(registers.f.toFlagsRegister().carry)
    }

    @Test
    fun leftRotateA_carryTrue_mostSignificantBit0() {
        registers.a = 0x0F.toUByte()
        registers.f = FlagsRegister(
            zero = false,
            subtract = false,
            halfCarry = false,
            carry = true,
        ).toUByte()
        val cpu = createCpu(ByteArray(0x8000))

        cpu.execute(Instruction.Rlca)

        assertEquals(0b00011110.toUByte(), registers.a)
        assertFalse(registers.f.toFlagsRegister().carry)
    }

    @Test
    fun leftRotateA_carryFalse_mostSignificantBit1() {
        registers.a = 0xF0.toUByte()
        registers.f = FlagsRegister(
            zero = false,
            subtract = false,
            halfCarry = false,
            carry = false,
        ).toUByte()
        val cpu = createCpu(ByteArray(0x8000))

        cpu.execute(Instruction.Rlca)

        assertEquals(0b11100001.toUByte(), registers.a)
        assertTrue(registers.f.toFlagsRegister().carry)
    }

    @Test
    fun cpl() {
        registers.a = 0xF0.toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0x2F.toByte() // CPL opcode
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x0F.toUByte(), registers.a)
    }

    @Test
    fun scf() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(
            carry = false,
            subtract = true,
            halfCarry = true,
        ).toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0x37.toByte() // SCF opcode
        val cpu = createCpu(rom)

        cpu.step()

        val flags = registers.f.toFlagsRegister()
        assertTrue(flags.carry)
        assertFalse(flags.subtract)
        assertFalse(flags.halfCarry)
    }

    @Test
    fun ccf() {
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(
            carry = true,
            subtract = true,
            halfCarry = true,
        ).toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0x3F.toByte() // CCF opcode
        val cpu = createCpu(rom)

        cpu.step()

        val flags = registers.f.toFlagsRegister()
        assertFalse(flags.carry)
        assertFalse(flags.subtract)
        assertFalse(flags.halfCarry)
    }

    @Test
    fun bit_bitSet() {
        registers.d = 0xF0.toUByte()
        registers.f = FlagsRegister(
            zero = false,
            subtract = true,
            halfCarry = false,
            carry = false,
        ).toUByte()
        val cpu = createCpu(ByteArray(0x8000))

        cpu.execute(
            Instruction.Bit(
                index = 3,
                target = Register8.D,
            )
        )

        val flags = registers.f.toFlagsRegister()
        assertTrue(flags.zero)
        assertFalse(flags.subtract)
        assertTrue(flags.halfCarry)
    }

    @Test
    fun bit_bitNotSet() {
        registers.e = 0x0F.toUByte()
        registers.f = FlagsRegister(
            zero = false,
            subtract = true,
            halfCarry = false,
            carry = false,
        ).toUByte()
        val cpu = createCpu(ByteArray(0x8000))

        cpu.execute(
            Instruction.Bit(
                index = 3,
                target = Register8.E,
            )
        )

        val flags = registers.f.toFlagsRegister()
        assertFalse(flags.zero)
        assertFalse(flags.subtract)
        assertTrue(flags.halfCarry)
    }

    @Test
    fun bit_incorrectBitCheckBug() {
        registers.e = 0b10001111.toUByte()
        registers.f = FlagsRegister(
            zero = false,
            subtract = true,
            halfCarry = false,
            carry = false,
        ).toUByte()
        val cpu = createCpu(ByteArray(0x8000))

        cpu.execute(
            Instruction.Bit(
                index = 3,
                target = Register8.E,
            )
        )

        val flags = registers.f.toFlagsRegister()
        assertFalse(flags.zero)
        assertFalse(flags.subtract)
        assertTrue(flags.halfCarry)
    }

    @Test
    fun reset() {
        registers.c = 0xFF.toUByte()
        val cpu = createCpu(ByteArray(0x8000))

        cpu.execute(Instruction.Res(index = 7, Register8.C))
        cpu.execute(Instruction.Res(index = 2, Register8.C))

        assertEquals(0b01111011.toUByte(), registers.c)
    }

    @Test
    fun set() {
        registers.e = 0xF0.toUByte()
        val cpu = createCpu(ByteArray(0x8000))

        cpu.execute(Instruction.Set(index = 0, Register8.E))
        cpu.execute(Instruction.Set(index = 6, Register8.E))

        assertEquals(0b11110001.toUByte(), registers.e)
    }

    @Test
    fun shiftRightLogically_leastSignificantBit1() {
        registers.b = 0x0F.toUByte()
        registers.f = FlagsRegister(
            zero = false,
            subtract = false,
            halfCarry = false,
            carry = false,
        ).toUByte()
        val cpu = createCpu(ByteArray(0x8000))

        cpu.execute(Instruction.Srl(Register8.B))

        assertEquals(0b00000111.toUByte(), registers.b)
        val flags = registers.f.toFlagsRegister()
        assertTrue(flags.carry)
    }

    @Test
    fun shiftRightLogically_leastSignificantBit0() {
        registers.b = 0xF0.toUByte()
        registers.f = FlagsRegister(
            zero = false,
            subtract = false,
            halfCarry = false,
            carry = false,
        ).toUByte()
        val cpu = createCpu(ByteArray(0x8000))

        cpu.execute(Instruction.Srl(Register8.B))

        assertEquals(0b01111000.toUByte(), registers.b)
        val flags = registers.f.toFlagsRegister()
        assertFalse(flags.carry)
    }

    @Test
    fun shiftRightLogically_shiftsToZero() {
        registers.b = 0b1.toUByte()
        registers.f = FlagsRegister(
            zero = false,
            subtract = false,
            halfCarry = false,
            carry = false,
        ).toUByte()
        val cpu = createCpu(ByteArray(0x8000))

        cpu.execute(Instruction.Srl(Register8.B))

        assertEquals(0b0.toUByte(), registers.b)
        val flags = registers.f.toFlagsRegister()
        assertTrue(flags.zero)
    }

    @Test
    fun rotateRight_carryFalse_leastSignificantBit1() {
        registers.c = 0x0F.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.toUByte()
        val cpu = createCpu(ByteArray(0x8000))

        cpu.execute(Instruction.Rr(Register8.C))

        assertEquals(0b00000111.toUByte(), registers.c)
        val flags = registers.f.toFlagsRegister()
        assertTrue(flags.carry)
    }

    @Test
    fun rotateRight_carryTrue_leastSignificantBit0() {
        registers.c = 0xF0.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = true).toUByte()
        val cpu = createCpu(ByteArray(0x8000))

        cpu.execute(Instruction.Rr(Register8.C))

        assertEquals(0b11111000.toUByte(), registers.c)
        val flags = registers.f.toFlagsRegister()
        assertFalse(flags.carry)
    }

    @Test
    fun rotateRight_rotatesToZero() {
        registers.c = 0b1.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.toUByte()
        val cpu = createCpu(ByteArray(0x8000))

        cpu.execute(Instruction.Rr(Register8.C))

        assertEquals(0b0.toUByte(), registers.c)
        val flags = registers.f.toFlagsRegister()
        assertTrue(flags.zero)
    }

    @Test
    fun rotateLeft_carryFalse_mostSignificantBit1() {
        registers.c = 0xF0.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.toUByte()
        val cpu = createCpu(ByteArray(0x8000))

        cpu.execute(Instruction.Rl(Register8.C))

        assertEquals(0b11100000.toUByte(), registers.c)
        val flags = registers.f.toFlagsRegister()
        assertTrue(flags.carry)
    }

    @Test
    fun rotateLeft_carryTrue_mostSignificantBit0() {
        registers.c = 0x0F.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = true).toUByte()
        val cpu = createCpu(ByteArray(0x8000))

        cpu.execute(Instruction.Rl(Register8.C))

        assertEquals(0b00011111.toUByte(), registers.c)
        val flags = registers.f.toFlagsRegister()
        assertFalse(flags.carry)
    }

    @Test
    fun rotateLeft_rotatesToZero() {
        registers.c = 0b10000000.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.toUByte()
        val cpu = createCpu(ByteArray(0x8000))

        cpu.execute(Instruction.Rl(Register8.C))

        assertEquals(0b0.toUByte(), registers.c)
        val flags = registers.f.toFlagsRegister()
        assertTrue(flags.zero)
    }

    @Test
    fun rotateRightCircularly_leastSignificantBit1() {
        registers.c = 0x0F.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.toUByte()
        val cpu = createCpu(ByteArray(0x8000))

        cpu.execute(Instruction.Rrc(Register8.C))

        assertEquals(0b10000111.toUByte(), registers.c)
        val flags = registers.f.toFlagsRegister()
        assertTrue(flags.carry)
    }

    @Test
    fun rotateRightCircularly_leastSignificantBit0() {
        registers.c = 0xF0.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = true).toUByte()
        val cpu = createCpu(ByteArray(0x8000))

        cpu.execute(Instruction.Rrc(Register8.C))

        assertEquals(0b01111000.toUByte(), registers.c)
        val flags = registers.f.toFlagsRegister()
        assertFalse(flags.carry)
    }

    @Test
    fun rotateRightCircularly_rotatesToZero() {
        registers.c = 0b0.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.toUByte()
        val cpu = createCpu(ByteArray(0x8000))

        cpu.execute(Instruction.Rrc(Register8.C))

        assertEquals(0b0.toUByte(), registers.c)
        val flags = registers.f.toFlagsRegister()
        assertTrue(flags.zero)
    }

    @Test
    fun rotateLeftCircularly_mostSignificantBit1() {
        registers.c = 0xF0.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.toUByte()
        val cpu = createCpu(ByteArray(0x8000))

        cpu.execute(Instruction.Rlc(Register8.C))

        assertEquals(0b11100001.toUByte(), registers.c)
        val flags = registers.f.toFlagsRegister()
        assertTrue(flags.carry)
    }

    @Test
    fun rotateLeftCircularly_mostSignificantBit0() {
        registers.c = 0x0F.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = true).toUByte()
        val cpu = createCpu(ByteArray(0x8000))

        cpu.execute(Instruction.Rlc(Register8.C))

        assertEquals(0b00011110.toUByte(), registers.c)
        val flags = registers.f.toFlagsRegister()
        assertFalse(flags.carry)
    }

    @Test
    fun rotateLeftCircularly_rotatesToZero() {
        registers.c = 0b0.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.toUByte()
        val cpu = createCpu(ByteArray(0x8000))

        cpu.execute(Instruction.Rlc(Register8.C))

        assertEquals(0b0.toUByte(), registers.c)
        val flags = registers.f.toFlagsRegister()
        assertTrue(flags.zero)
    }

    @Test
    fun shiftRightArithmetically_leastSignificantBit0_signBit1() {
        registers.d = 0b10010100.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.toUByte()
        val cpu = createCpu(ByteArray(0x8000))

        cpu.execute(Instruction.Sra(Register8.D))

        assertEquals(0b11001010.toUByte(), registers.d)
        val flags = registers.f.toFlagsRegister()
        assertFalse(flags.carry)
    }

    @Test
    fun shiftRightArithmetically_leastSignificantBit1_signBit0() {
        registers.d = 0b00010101.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.toUByte()
        val cpu = createCpu(ByteArray(0x8000))

        cpu.execute(Instruction.Sra(Register8.D))

        assertEquals(0b00001010.toUByte(), registers.d)
        val flags = registers.f.toFlagsRegister()
        assertTrue(flags.carry)
    }

    @Test
    fun shiftRightArithmetically_shiftsToZero() {
        registers.d = 0b00000001.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.toUByte()
        val cpu = createCpu(ByteArray(0x8000))

        cpu.execute(Instruction.Sra(Register8.D))

        val flags = registers.f.toFlagsRegister()
        assertTrue(flags.zero)
    }

    @Test
    fun shiftLeftArithmetically_mostSignificantBit0() {
        registers.b = 0x0F.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.toUByte()
        val cpu = createCpu(ByteArray(0x8000))

        cpu.execute(Instruction.Sla(Register8.B))

        assertEquals(0b00011110.toUByte(), registers.b)
        val flags = registers.f.toFlagsRegister()
        assertFalse(flags.carry)
    }

    @Test
    fun shiftLeftArithmetically_mostSignificantBit1() {
        registers.b = 0xF0.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.toUByte()
        val cpu = createCpu(ByteArray(0x8000))

        cpu.execute(Instruction.Sla(Register8.B))

        assertEquals(0b11100000.toUByte(), registers.b)
        val flags = registers.f.toFlagsRegister()
        assertTrue(flags.carry)
    }

    @Test
    fun shiftLeftArithmetically_shiftsToZero() {
        registers.b = 0b100000000.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.toUByte()
        val cpu = createCpu(ByteArray(0x8000))

        cpu.execute(Instruction.Sla(Register8.B))

        val flags = registers.f.toFlagsRegister()
        assertTrue(flags.zero)
    }

    @Test
    fun swap() {
        registers.a = 0x0F.toUByte()
        registers.b = 0xF0.toUByte()
        registers.c = 0b01010010.toUByte()
        val cpu = createCpu(ByteArray(0x8000))

        cpu.execute(Instruction.Swap(Register8.A))
        cpu.execute(Instruction.Swap(Register8.B))
        cpu.execute(Instruction.Swap(Register8.C))

        assertEquals(0xF0.toUByte(), registers.a)
        assertEquals(0x0F.toUByte(), registers.b)
        assertEquals(0b00100101.toUByte(), registers.c)
    }

    @Test
    fun daa_noAdjustmentsNeeded() {
        registers.a = 0x45.toUByte()
        registers.f = FlagsRegister(
            zero = false,
            subtract = false,
            halfCarry = false,
            carry = false,
        ).toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0x27.toByte() // DAA opcode
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x45.toUByte(), registers.a)
        val flags = registers.f.toFlagsRegister()
        assertFalse(flags.carry)
        assertFalse(flags.halfCarry)
        assertFalse(flags.zero)
    }

    @Test
    fun daa_adjustForLowerNibble() {
        registers.a = 0x0A.toUByte()
        registers.f = FlagsRegister(
            zero = false,
            subtract = false,
            halfCarry = true,
            carry = false,
        ).toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0x27.toByte() // DAA opcode
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x10.toUByte(), registers.a)
        val flags = registers.f.toFlagsRegister()
        assertFalse(flags.carry)
        assertFalse(flags.halfCarry)
        assertFalse(flags.zero)
    }

    @Test
    fun daa_adjustForUpperNibble() {
        registers.a = 0x9A.toUByte()
        registers.f = FlagsRegister(
            zero = false,
            subtract = false,
            halfCarry = false,
            carry = true,
        ).toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0x27.toByte() // DAA opcode
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x00.toUByte(), registers.a)
        val flags = registers.f.toFlagsRegister()
        assertTrue(flags.carry)
        assertTrue(flags.zero)
        assertFalse(flags.halfCarry)
    }

    @Test
    fun daa_adjustForBothNibbles() {
        registers.a = 0x9A.toUByte()
        registers.f = FlagsRegister(
            zero = false,
            subtract = false,
            halfCarry = true,
            carry = true,
        ).toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0x27.toByte() // DAA opcode
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x00.toUByte(), registers.a)
        val flags = registers.f.toFlagsRegister()
        assertTrue(flags.carry)
        assertTrue(flags.zero)
        assertFalse(flags.halfCarry)
    }

    @Test
    fun daa_subtractionMode() {
        registers.a = 0x66.toUByte()
        registers.f = FlagsRegister(
            zero = false,
            subtract = true,
            halfCarry = true,
            carry = true,
        ).toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0x27.toByte() // DAA opcode
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x00.toUByte(), registers.a)
        val flags = registers.f.toFlagsRegister()
        assertTrue(flags.carry)
        assertTrue(flags.zero)
        assertFalse(flags.halfCarry)
    }

    @Test
    fun halt_stopsExecution() {
        val rom = ByteArray(0x8000)
        rom[0] = 0x76.toByte() // HALT opcode
        val cpu = createCpu(rom)

        cpu.step()
        cpu.step()
        cpu.step()

        assertEquals(0x01.toUShort(), programCounter.get())
    }

    @Test
    fun halt_stopsExecutionUntilInterrupt() {
        val rom = ByteArray(0x8000)
        rom[0] = 0x76.toByte() // HALT opcode
        rom[1] = 0x00.toByte() // NOP opcode
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)

        cpu.step()
        assertEquals(0x01.toUShort(), programCounter.get())
        cpu.step()
        assertEquals(0x01.toUShort(), programCounter.get())

        memoryBus.setInterruptFlagBit(0, true) // V-Blank interrupt
        memoryBus.setInterruptEnableBit(0, true)
        cpu.step()

        // PC jumps to V-Blank handler
        assertEquals(0x40.toUShort(), programCounter.get())
    }

    @Test
    fun disableInterrupts() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xF3.toByte() // DI opcode
        rom[1] = 0x76.toByte() // HALT opcode
        rom[2] = 0x00.toByte() // NOP opcode
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)

        cpu.step()

        memoryBus.setInterruptFlagBit(0, true) // V-Blank interrupt
        memoryBus.setInterruptEnableBit(0, true)
        cpu.step()

        assertEquals(0x2.toUShort(), programCounter.get())
    }

    @Test
    fun enableInterrupts() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xF3.toByte() // DI opcode
        rom[1] = 0xFB.toByte() // EI opcode
        rom[2] = 0x00.toByte() // NOP opcode
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)

        // disable interrupts
        cpu.step()
        // V-Blank interrupt
        memoryBus.setInterruptFlagBit(0, true)
        memoryBus.setInterruptEnableBit(0, true)

        // enable interrupts
        cpu.step()
        assertEquals(0x2.toUShort(), programCounter.get())

        // PC jumps to V-Blank handler
        cpu.step()
        assertEquals(0x40.toUShort(), programCounter.get())
    }

    @Test
    fun interrupt_servicesHighestPriorityInterrupt() {
        val rom = ByteArray(0x8000)
        rom[0] = 0x00.toByte() // NOP opcode
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)
        // Timer interrupt
        memoryBus.setInterruptFlagBit(2, true)
        memoryBus.setInterruptEnableBit(2, true)
        // V-Blank interrupt (higher priority)
        memoryBus.setInterruptFlagBit(0, true)
        memoryBus.setInterruptEnableBit(0, true)

        cpu.step()

        // PC jumps to V-Blank handler
        assertEquals(0x40.toUShort(), programCounter.get())
    }

    @Test
    fun reti_enablesInterruptsAndReturns() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xF3.toByte() // DI opcode
        rom[1] = 0x00.toByte() // NOP opcode
        rom[2] = 0xD9.toByte() // RETI opcode
        rom[3] = 0x00.toByte() // NOP opcode
        rom[0x1234] = 0x00.toByte() // NOP opcode at interrupt handler
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)
        memoryBus.writeByte(0xFFFD.toUShort(), 0x34.toUByte())
        memoryBus.writeByte(0xFFFE.toUShort(), 0x12.toUByte())
        stackPointer.setTo(0xFFFD.toUShort())

        // disable interrupts
        cpu.step()
        memoryBus.setInterruptFlagBit(0, true) // V-Blank interrupt
        memoryBus.setInterruptEnableBit(0, true)

        // interrupt is not serviced because interrupts are disabled
        cpu.step()

        // return from subroutine + enabling interrupts
        cpu.step()
        assertEquals(0x1234.toUShort(), programCounter.get())

        // interrupts are now enabled
        cpu.step()
        // PC jumps to V-Blank handler
        assertEquals(0x40.toUShort(), programCounter.get())
    }
}
