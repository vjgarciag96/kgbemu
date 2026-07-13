package com.vicgarci.kgbem.emulator

import com.vicgarci.kgbem.cartridge.RomOnlyCartridge
import com.vicgarci.kgbem.cpu.MemoryBus
import com.vicgarci.kgbem.cpu.ProgramCounter
import com.vicgarci.kgbem.cpu.Registers
import com.vicgarci.kgbem.cpu.StackPointer
import kotlin.test.Test
import kotlin.test.assertEquals

class CpuInitialiserTest {

    private val registers = Registers(
        a = 0u, b = 0u, c = 0u, d = 0u,
        e = 0u, f = 0u, h = 0u, l = 0u,
    )
    private val programCounter = ProgramCounter(0x0000.toUShort())
    private val stackPointer = StackPointer(0x0000.toUShort())
    private val bus = MemoryBus(RomOnlyCartridge(ByteArray(0x8000)))

    private fun applyPostBoot() {
        CpuInitialiser.applyPostBootState(registers, programCounter, stackPointer, bus)
    }

    @Test
    fun a_is_0x01() {
        applyPostBoot()
        assertEquals(0x01.toUByte(), registers.a)
    }

    @Test
    fun f_is_0xB0() {
        applyPostBoot()
        assertEquals(0xB0.toUByte(), registers.f)
    }

    @Test
    fun b_is_0x00() {
        applyPostBoot()
        assertEquals(0x00.toUByte(), registers.b)
    }

    @Test
    fun c_is_0x13() {
        applyPostBoot()
        assertEquals(0x13.toUByte(), registers.c)
    }

    @Test
    fun d_is_0x00() {
        applyPostBoot()
        assertEquals(0x00.toUByte(), registers.d)
    }

    @Test
    fun e_is_0xD8() {
        applyPostBoot()
        assertEquals(0xD8.toUByte(), registers.e)
    }

    @Test
    fun h_is_0x01() {
        applyPostBoot()
        assertEquals(0x01.toUByte(), registers.h)
    }

    @Test
    fun l_is_0x4D() {
        applyPostBoot()
        assertEquals(0x4D.toUByte(), registers.l)
    }

    @Test
    fun sp_is_0xFFFE() {
        applyPostBoot()
        assertEquals(0xFFFE.toUShort(), stackPointer.get())
    }

    @Test
    fun pc_is_0x0100() {
        applyPostBoot()
        assertEquals(0x0100.toUShort(), programCounter.get())
    }

    @Test
    fun lcdc_is_0x91() {
        applyPostBoot()
        assertEquals(0x91.toUByte(), bus.readByte(0xFF40.toUShort()))
    }

    @Test
    fun bgp_is_0xFC() {
        applyPostBoot()
        assertEquals(0xFC.toUByte(), bus.readByte(0xFF47.toUShort()))
    }

    @Test
    fun obp0_is_0xFF() {
        applyPostBoot()
        assertEquals(0xFF.toUByte(), bus.readByte(0xFF48.toUShort()))
    }

    @Test
    fun obp1_is_0xFF() {
        applyPostBoot()
        assertEquals(0xFF.toUByte(), bus.readByte(0xFF49.toUShort()))
    }

    @Test
    fun ie_is_0x00() {
        applyPostBoot()
        assertEquals(0x00.toUByte(), bus.readByte(0xFFFF.toUShort()))
    }
}
