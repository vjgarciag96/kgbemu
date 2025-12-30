package com.vicgarci.kgbem.cpu

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
    private val memory = Array(0x1) { 0.toUByte() }
    private val memoryBus = MemoryBus(memory)

    private val cpu = CPU(
        registers,
        programCounter,
        memoryBus,
    )

    @Test
    fun addWithCarry_carryFalse() {
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

        cpu.execute(Instruction.Rlca)

        assertEquals(0b11100001.toUByte(), registers.a)
        assertTrue(registers.f.toFlagsRegister().carry)
    }

    @Test
    fun cpl() {
        registers.a = 0xF0.toUByte()
        memory[0] = 0x2F.toUByte() // CPL opcode

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
        memory[0] = 0x37.toUByte() // SCF opcode

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
        memory[0] = 0x3F.toUByte() // CCF opcode

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

        cpu.execute(Instruction.Res(index = 7, Register8.C))
        cpu.execute(Instruction.Res(index = 2, Register8.C))

        assertEquals(0b01111011.toUByte(), registers.c)
    }

    @Test
    fun set() {
        registers.e = 0xF0.toUByte()

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

        cpu.execute(Instruction.Srl(Register8.B))

        assertEquals(0b0.toUByte(), registers.b)
        val flags = registers.f.toFlagsRegister()
        assertTrue(flags.zero)
    }

    @Test
    fun rotateRight_carryFalse_leastSignificantBit1() {
        registers.c = 0x0F.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.toUByte()

        cpu.execute(Instruction.Rr(Register8.C))

        assertEquals(0b00000111.toUByte(), registers.c)
        val flags = registers.f.toFlagsRegister()
        assertTrue(flags.carry)
    }

    @Test
    fun rotateRight_carryTrue_leastSignificantBit0() {
        registers.c = 0xF0.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = true).toUByte()

        cpu.execute(Instruction.Rr(Register8.C))

        assertEquals(0b11111000.toUByte(), registers.c)
        val flags = registers.f.toFlagsRegister()
        assertFalse(flags.carry)
    }

    @Test
    fun rotateRight_rotatesToZero() {
        registers.c = 0b1.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.toUByte()

        cpu.execute(Instruction.Rr(Register8.C))

        assertEquals(0b0.toUByte(), registers.c)
        val flags = registers.f.toFlagsRegister()
        assertTrue(flags.zero)
    }

    @Test
    fun rotateLeft_carryFalse_mostSignificantBit1() {
        registers.c = 0xF0.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.toUByte()

        cpu.execute(Instruction.Rl(Register8.C))

        assertEquals(0b11100000.toUByte(), registers.c)
        val flags = registers.f.toFlagsRegister()
        assertTrue(flags.carry)
    }

    @Test
    fun rotateLeft_carryTrue_mostSignificantBit0() {
        registers.c = 0x0F.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = true).toUByte()

        cpu.execute(Instruction.Rl(Register8.C))

        assertEquals(0b00011111.toUByte(), registers.c)
        val flags = registers.f.toFlagsRegister()
        assertFalse(flags.carry)
    }

    @Test
    fun rotateLeft_rotatesToZero() {
        registers.c = 0b10000000.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.toUByte()

        cpu.execute(Instruction.Rl(Register8.C))

        assertEquals(0b0.toUByte(), registers.c)
        val flags = registers.f.toFlagsRegister()
        assertTrue(flags.zero)
    }

    @Test
    fun rotateRightCircularly_leastSignificantBit1() {
        registers.c = 0x0F.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.toUByte()

        cpu.execute(Instruction.Rrc(Register8.C))

        assertEquals(0b10000111.toUByte(), registers.c)
        val flags = registers.f.toFlagsRegister()
        assertTrue(flags.carry)
    }

    @Test
    fun rotateRightCircularly_leastSignificantBit0() {
        registers.c = 0xF0.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = true).toUByte()

        cpu.execute(Instruction.Rrc(Register8.C))

        assertEquals(0b01111000.toUByte(), registers.c)
        val flags = registers.f.toFlagsRegister()
        assertFalse(flags.carry)
    }

    @Test
    fun rotateRightCircularly_rotatesToZero() {
        registers.c = 0b0.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.toUByte()

        cpu.execute(Instruction.Rrc(Register8.C))

        assertEquals(0b0.toUByte(), registers.c)
        val flags = registers.f.toFlagsRegister()
        assertTrue(flags.zero)
    }

    @Test
    fun rotateLeftCircularly_mostSignificantBit1() {
        registers.c = 0xF0.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.toUByte()

        cpu.execute(Instruction.Rlc(Register8.C))

        assertEquals(0b11100001.toUByte(), registers.c)
        val flags = registers.f.toFlagsRegister()
        assertTrue(flags.carry)
    }

    @Test
    fun rotateLeftCircularly_mostSignificantBit0() {
        registers.c = 0x0F.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.copy(carry = true).toUByte()

        cpu.execute(Instruction.Rlc(Register8.C))

        assertEquals(0b00011110.toUByte(), registers.c)
        val flags = registers.f.toFlagsRegister()
        assertFalse(flags.carry)
    }

    @Test
    fun rotateLeftCircularly_rotatesToZero() {
        registers.c = 0b0.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.toUByte()

        cpu.execute(Instruction.Rlc(Register8.C))

        assertEquals(0b0.toUByte(), registers.c)
        val flags = registers.f.toFlagsRegister()
        assertTrue(flags.zero)
    }

    @Test
    fun shiftRightArithmetically_leastSignificantBit0_signBit1() {
        registers.d = 0b10010100.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.toUByte()

        cpu.execute(Instruction.Sra(Register8.D))

        assertEquals(0b11001010.toUByte(), registers.d)
        val flags = registers.f.toFlagsRegister()
        assertFalse(flags.carry)
    }

    @Test
    fun shiftRightArithmetically_leastSignificantBit1_signBit0() {
        registers.d = 0b00010101.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.toUByte()

        cpu.execute(Instruction.Sra(Register8.D))

        assertEquals(0b00001010.toUByte(), registers.d)
        val flags = registers.f.toFlagsRegister()
        assertTrue(flags.carry)
    }

    @Test
    fun shiftRightArithmetically_shiftsToZero() {
        registers.d = 0b00000001.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.toUByte()

        cpu.execute(Instruction.Sra(Register8.D))

        val flags = registers.f.toFlagsRegister()
        assertTrue(flags.zero)
    }

    @Test
    fun shiftLeftArithmetically_mostSignificantBit0() {
        registers.b = 0x0F.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.toUByte()

        cpu.execute(Instruction.Sla(Register8.B))

        assertEquals(0b00011110.toUByte(), registers.b)
        val flags = registers.f.toFlagsRegister()
        assertFalse(flags.carry)
    }

    @Test
    fun shiftLeftArithmetically_mostSignificantBit1() {
        registers.b = 0xF0.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.toUByte()

        cpu.execute(Instruction.Sla(Register8.B))

        assertEquals(0b11100000.toUByte(), registers.b)
        val flags = registers.f.toFlagsRegister()
        assertTrue(flags.carry)
    }

    @Test
    fun shiftLeftArithmetically_shiftsToZero() {
        registers.b = 0b100000000.toUByte()
        registers.f = FlagsRegisterFixtures.FLAGS_NOT_SET.toUByte()

        cpu.execute(Instruction.Sla(Register8.B))

        val flags = registers.f.toFlagsRegister()
        assertTrue(flags.zero)
    }

    @Test
    fun swap() {
        registers.a = 0x0F.toUByte()
        registers.b = 0xF0.toUByte()
        registers.c = 0b01010010.toUByte()

        cpu.execute(Instruction.Swap(Register8.A))
        cpu.execute(Instruction.Swap(Register8.B))
        cpu.execute(Instruction.Swap(Register8.C))

        assertEquals(0xF0.toUByte(), registers.a)
        assertEquals(0x0F.toUByte(), registers.b)
        assertEquals(0b00100101.toUByte(), registers.c)
    }
}