package com.vicgarci.kgbem.cpu

import com.vicgarci.kgbem.cartridge.RomOnlyCartridge
import kotlin.test.Test
import kotlin.test.assertEquals

class LoadCPUTest {

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
    fun load_constant_intoRegisterB() {
        val rom = ByteArray(0x8000)
        rom[0] = 0x06.toByte() // LD B, n opcode
        rom[1] = 0x42.toByte() // value to load into register B
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x42.toUByte(), registers.b)
    }

    @Test
    fun load_constant_intoRegisterC() {
        val rom = ByteArray(0x8000)
        rom[0] = 0x0E.toByte() // LD C, n opcode
        rom[1] = 0x43.toByte() // value to load into register C
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x43.toUByte(), registers.c)
    }

    @Test
    fun load_constant_intoRegisterD() {
        val rom = ByteArray(0x8000)
        rom[0] = 0x16.toByte() // LD D, n opcode
        rom[1] = 0x44.toByte() // value to load into register D
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x44.toUByte(), registers.d)
    }

    @Test
    fun load_constant_intoRegisterE() {
        val rom = ByteArray(0x8000)
        rom[0] = 0x1E.toByte() // LD E, n opcode
        rom[1] = 0x45.toByte() // value to load into register E
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x45.toUByte(), registers.e)
    }

    @Test
    fun load_constant_intoRegisterH() {
        val rom = ByteArray(0x8000)
        rom[0] = 0x26.toByte() // LD H, n opcode
        rom[1] = 0x46.toByte() // value to load into register H
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x46.toUByte(), registers.h)
    }

    @Test
    fun load_constant_intoRegisterL() {
        val rom = ByteArray(0x8000)
        rom[0] = 0x2E.toByte() // LD L, n opcode
        rom[1] = 0x47.toByte() // value to load into register L
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x47.toUByte(), registers.l)
    }

    @Test
    fun load_constant_intoRegisterA() {
        val rom = ByteArray(0x8000)
        rom[0] = 0x3E.toByte() // LD A, n opcode
        rom[1] = 0x48.toByte() // value to load into register A
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x48.toUByte(), registers.a)
    }

    @Test
    fun load_constant_into_memoryAtHL() {
        registers.hl = 0xC000.toUShort() // Use WRAM, not ROM range
        val rom = ByteArray(0x8000)
        rom[0] = 0x36.toByte() // LD (HL), n opcode
        rom[1] = 0x99.toByte() // value to store at HL
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)

        cpu.step()

        assertEquals(0x99.toUByte(), memoryBus.readByte(0xC000.toUShort()))
    }

    @Test
    fun load_constant_intoRegisterBC() {
        val rom = ByteArray(0x8000)
        rom[0] = 0x01.toByte() // LD BC, nn opcode
        rom[1] = 0x34.toByte() // low byte
        rom[2] = 0x12.toByte() // high byte
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x1234.toUShort(), registers.bc)
    }

    @Test
    fun load_constant_intoRegisterDE() {
        val rom = ByteArray(0x8000)
        rom[0] = 0x11.toByte() // LD DE, nn opcode
        rom[1] = 0x56.toByte() // low byte
        rom[2] = 0x34.toByte() // high byte
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x3456.toUShort(), registers.de)
    }

    @Test
    fun load_constant_intoRegisterHL() {
        val rom = ByteArray(0x8000)
        rom[0] = 0x21.toByte() // LD HL, nn opcode
        rom[1] = 0x78.toByte() // low byte
        rom[2] = 0x56.toByte() // high byte
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x5678.toUShort(), registers.hl)
    }

    @Test
    fun load_constant_intoRegisterSP() {
        val rom = ByteArray(0x8000)
        rom[0] = 0x31.toByte() // LD SP, nn opcode
        rom[1] = 0x9A.toByte() // low byte
        rom[2] = 0x78.toByte() // high byte
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x789A.toUShort(), stackPointer.get())
    }

    @Test
    fun loadIncrement_A_from_HL() {
        registers.hl = 0xC009.toUShort() // Use WRAM
        val rom = ByteArray(0x8000)
        rom[0] = 0x2A.toByte() // LD A, (HL+) opcode
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)
        memoryBus.writeByte(0xC009.toUShort(), 0x42.toUByte()) // Value at HL

        cpu.step()

        assertEquals(0x42.toUByte(), registers.a) // A should now hold the value at HL
        assertEquals(0xC00A.toUShort(), registers.hl) // HL should increment by 1
    }

    @Test
    fun loadIncrement_HL_from_A() {
        registers.hl = 0xC009.toUShort() // Use WRAM
        registers.a = 0x42.toUByte() // Value in A
        val rom = ByteArray(0x8000)
        rom[0] = 0x22.toByte() // LD (HL+), A opcode
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)

        cpu.step()

        assertEquals(0x42.toUByte(), memoryBus.readByte(0xC009.toUShort())) // Memory at HL should now hold the value of A
        assertEquals(0xC00A.toUShort(), registers.hl) // HL should increment by 1
    }

    @Test
    fun loadDecrement_A_from_HL() {
        registers.hl = 0xC009.toUShort() // Use WRAM
        val rom = ByteArray(0x8000)
        rom[0] = 0x3A.toByte() // LD A, (HL-) opcode
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)
        memoryBus.writeByte(0xC009.toUShort(), 0x42.toUByte()) // Value at HL

        cpu.step()

        assertEquals(0x42.toUByte(), registers.a) // A should now hold the value at HL
        assertEquals(0xC008.toUShort(), registers.hl) // HL should decrement by 1
    }

    @Test
    fun loadDecrement_HL_from_A() {
        registers.hl = 0xC009.toUShort() // Use WRAM
        registers.a = 0x42.toUByte() // Value in A
        val rom = ByteArray(0x8000)
        rom[0] = 0x32.toByte() // LD (HL-), A opcode
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)

        cpu.step()

        assertEquals(0x42.toUByte(), memoryBus.readByte(0xC009.toUShort())) // Memory at HL should now hold the value of A
        assertEquals(0xC008.toUShort(), registers.hl) // HL should decrement by 1
    }

    @Test
    fun loadIncrement_wrapAround() {
        registers.hl = 0xFFFF.toUShort()
        val rom = ByteArray(0x8000)
        rom[0] = 0x22.toByte() // LD (HL+), A opcode
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x0000.toUShort(), registers.hl)
    }

    @Test
    fun loadDecrement_wrapAround() {
        registers.hl = 0x0000.toUShort()
        val rom = ByteArray(0x8000)
        rom[0] = 0x32.toByte() // LD (HL-), A opcode
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0xFFFF.toUShort(), registers.hl)
    }

    @Test
    fun load_registerB_into_registerA() {
        registers.b = 0x45.toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0x78.toByte() // LD A, B opcode
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x45.toUByte(), registers.a) // Value from B should be loaded into A
    }

    @Test
    fun load_registerC_into_registerB() {
        registers.c = 0x42.toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0x41.toByte() // LD B, C opcode
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x42.toUByte(), registers.b) // Value from C should be loaded into B
    }

    @Test
    fun load_registerD_into_registerB() {
        registers.d = 0x50.toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0x42.toByte() // LD B, D opcode
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x50.toUByte(), registers.b) // Value from D should be loaded into B
    }

    @Test
    fun load_registerE_into_registerD() {
        registers.e = 0x43.toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0x53.toByte() // LD D, E opcode
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x43.toUByte(), registers.d) // Value from E should be loaded into D
    }
    @Test
    fun load_registerH_into_registerE() {
        registers.h = 0x47.toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0x5C.toByte() // LD E, H opcode
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x47.toUByte(), registers.e) // Value from H should be loaded into E
    }

    @Test
    fun load_registerL_into_registerH() {
        registers.l = 0x44.toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0x65.toByte() // LD H, L opcode
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x44.toUByte(), registers.h) // Value from L should be loaded into H
    }

    @Test
    fun load_registerA_into_registerC() {
        registers.a = 0x46.toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0x4F.toByte() // LD C, A opcode
        val cpu = createCpu(rom)

        cpu.step()

        assertEquals(0x46.toUByte(), registers.c) // Value from A should be loaded into C
    }

    @Test
    fun load_memoryAtHL_into_registerA() {
        registers.hl = 0xC000.toUShort() // Use WRAM
        val rom = ByteArray(0x8000)
        rom[0] = 0x7E.toByte() // LD A, (HL) opcode
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)
        memoryBus.writeByte(0xC000.toUShort(), 0x42.toUByte()) // Value at HL

        cpu.step()

        assertEquals(0x42.toUByte(), registers.a) // Value at HL should be loaded into A
    }

    @Test
    fun load_registerA_into_memoryAtHL() {
        registers.hl = 0xC000.toUShort() // Use WRAM
        registers.a = 0x42.toUByte() // Value in A
        val rom = ByteArray(0x8000)
        rom[0] = 0x77.toByte() // LD (HL), A opcode
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)

        cpu.step()

        assertEquals(0x42.toUByte(), memoryBus.readByte(0xC000.toUShort())) // Value in A should be stored at HL
    }

    @Test
    fun load_registerA_into_memoryAtBC() {
        registers.bc = 0xC000.toUShort() // Use WRAM
        registers.a = 0x77.toUByte() // Value in A
        val rom = ByteArray(0x8000)
        rom[0] = 0x02.toByte() // LD (BC), A opcode
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)

        cpu.step()

        assertEquals(0x77.toUByte(), memoryBus.readByte(0xC000.toUShort()))
    }

    @Test
    fun load_registerA_into_memoryAtDE() {
        registers.de = 0xC001.toUShort() // Use WRAM
        registers.a = 0x88.toUByte() // Value in A
        val rom = ByteArray(0x8000)
        rom[0] = 0x12.toByte() // LD (DE), A opcode
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)

        cpu.step()

        assertEquals(0x88.toUByte(), memoryBus.readByte(0xC001.toUShort()))
    }

    @Test
    fun load_memoryAtBC_into_registerA() {
        registers.bc = 0xC002.toUShort() // Use WRAM
        val rom = ByteArray(0x8000)
        rom[0] = 0x0A.toByte() // LD A, (BC) opcode
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)
        memoryBus.writeByte(0xC002.toUShort(), 0x55.toUByte()) // Value at BC

        cpu.step()

        assertEquals(0x55.toUByte(), registers.a)
    }

    @Test
    fun load_memoryAtDE_into_registerA() {
        registers.de = 0xC003.toUShort() // Use WRAM
        val rom = ByteArray(0x8000)
        rom[0] = 0x1A.toByte() // LD A, (DE) opcode
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)
        memoryBus.writeByte(0xC003.toUShort(), 0x66.toUByte()) // Value at DE

        cpu.step()

        assertEquals(0x66.toUByte(), registers.a)
    }

    @Test
    fun load_registerA_into_memoryAtImmediate() {
        registers.a = 0x5A.toUByte() // Value in A
        val rom = ByteArray(0x8000)
        rom[0] = 0xEA.toByte() // LD (nn), A opcode
        rom[1] = 0x34.toByte() // low byte
        rom[2] = 0xC0.toByte() // high byte -> 0xC034 (WRAM)
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)

        cpu.step()

        assertEquals(0x5A.toUByte(), memoryBus.readByte(0xC034.toUShort()))
    }

    @Test
    fun load_memoryAtImmediate_into_registerA() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xFA.toByte() // LD A, (nn) opcode
        rom[1] = 0x78.toByte() // low byte
        rom[2] = 0xC0.toByte() // high byte -> 0xC078 (WRAM)
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)
        memoryBus.writeByte(0xC078.toUShort(), 0xAB.toUByte()) // Value at nn

        cpu.step()

        assertEquals(0xAB.toUByte(), registers.a)
    }

    @Test
    fun load_registerA_into_highMemoryAtImmediate() {
        registers.a = 0x1A.toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0xE0.toByte() // LDH (n), A opcode
        rom[1] = 0x10.toByte()
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)

        cpu.step()

        assertEquals(0x1A.toUByte(), memoryBus.readByte(0xFF10.toUShort()))
    }

    @Test
    fun load_highMemoryAtImmediate_into_registerA() {
        val rom = ByteArray(0x8000)
        rom[0] = 0xF0.toByte() // LDH A, (n) opcode
        rom[1] = 0x20.toByte()
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)
        memoryBus.writeByte(0xFF20.toUShort(), 0x2B.toUByte())

        cpu.step()

        assertEquals(0x2B.toUByte(), registers.a)
    }

    @Test
    fun load_registerA_into_highMemoryAtC() {
        registers.a = 0x3C.toUByte()
        registers.c = 0x30.toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0xE2.toByte() // LD (C), A opcode
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)

        cpu.step()

        assertEquals(0x3C.toUByte(), memoryBus.readByte(0xFF30.toUShort()))
    }

    @Test
    fun load_highMemoryAtC_into_registerA() {
        registers.c = 0x40.toUByte()
        val rom = ByteArray(0x8000)
        rom[0] = 0xF2.toByte() // LD A, (C) opcode
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)
        memoryBus.writeByte(0xFF40.toUShort(), 0x4D.toUByte())

        cpu.step()

        assertEquals(0x4D.toUByte(), registers.a)
    }

    @Test
    fun load_memoryAtHL_into_registerB() {
        registers.hl = 0xC000.toUShort() // Use WRAM
        val rom = ByteArray(0x8000)
        rom[0] = 0x46.toByte() // LD B, (HL) opcode
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)
        memoryBus.writeByte(0xC000.toUShort(), 0x55.toUByte()) // Value at HL

        cpu.step()

        assertEquals(0x55.toUByte(), registers.b) // Value at HL should be loaded into B
    }

    @Test
    fun load_registerC_into_memoryAtHL() {
        registers.hl = 0xC000.toUShort() // Use WRAM
        registers.c = 0x66.toUByte() // Value in C
        val rom = ByteArray(0x8000)
        rom[0] = 0x71.toByte() // LD (HL), C opcode
        val (cpu, memoryBus) = createCpuWithMemoryBus(rom)

        cpu.step()

        assertEquals(0x66.toUByte(), memoryBus.readByte(0xC000.toUShort())) // Value in C should be stored at HL
    }
}
