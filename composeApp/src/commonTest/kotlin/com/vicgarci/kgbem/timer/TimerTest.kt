package com.vicgarci.kgbem.timer

import com.vicgarci.kgbem.cpu.MemoryBus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TimerTest {

    private fun makeBus(): MemoryBus = MemoryBus().also { it.initializePostBoot() }

    // ── DIV register ──────────────────────────────────────────────────────

    @Test
    fun div_increments_every_256_cycles() {
        val bus = makeBus()
        val timer = Timer(bus)

        timer.tick(256)

        assertEquals(1, bus.readByte(0xFF04.toUShort()).toInt(), "DIV should be 1 after 256 T-cycles")
    }

    @Test
    fun div_increments_multiple_times() {
        val bus = makeBus()
        val timer = Timer(bus)

        timer.tick(256 * 3)

        assertEquals(3, bus.readByte(0xFF04.toUShort()).toInt(), "DIV should be 3 after 768 T-cycles")
    }

    @Test
    fun div_wraps_at_255() {
        val bus = makeBus()
        val timer = Timer(bus)

        timer.tick(256 * 256)

        assertEquals(0, bus.readByte(0xFF04.toUShort()).toInt(), "DIV should wrap to 0 after 256 increments")
    }

    @Test
    fun writing_to_div_resets_it_to_zero() {
        val bus = makeBus()
        val timer = Timer(bus)

        timer.tick(256 * 10)  // DIV = 10
        bus.writeByte(0xFF04.toUShort(), 0x42.toUByte())  // Any write resets DIV to 0

        assertEquals(0, bus.readByte(0xFF04.toUShort()).toInt(), "DIV must be reset to 0 on any write")
    }

    // ── TIMA / TAC ────────────────────────────────────────────────────────

    @Test
    fun tima_does_not_increment_when_timer_disabled() {
        val bus = makeBus()
        bus.writeByte(0xFF07.toUShort(), 0x00.toUByte())  // TAC: timer off
        val timer = Timer(bus)

        timer.tick(1024 * 5)

        assertEquals(0, bus.readByte(0xFF05.toUShort()).toInt(), "TIMA must not change when timer is stopped")
    }

    @Test
    fun tima_increments_at_4096_hz() {
        val bus = makeBus()
        bus.writeByte(0xFF07.toUShort(), 0x04.toUByte())  // TAC: timer on, clock select 00 → 4 096 Hz
        val timer = Timer(bus)

        timer.tick(1024)

        assertEquals(1, bus.readByte(0xFF05.toUShort()).toInt(), "TIMA should be 1 after 1 024 T-cycles at 4 096 Hz")
    }

    @Test
    fun tima_increments_at_262144_hz() {
        val bus = makeBus()
        bus.writeByte(0xFF07.toUShort(), 0x05.toUByte())  // TAC: timer on, clock select 01 → 262 144 Hz
        val timer = Timer(bus)

        timer.tick(16)

        assertEquals(1, bus.readByte(0xFF05.toUShort()).toInt(), "TIMA should be 1 after 16 T-cycles at 262 144 Hz")
    }

    @Test
    fun tima_overflow_loads_tma_and_fires_interrupt() {
        val bus = makeBus()
        bus.writeByte(0xFF05.toUShort(), 0xFF.toUByte())  // TIMA = 255 (about to overflow)
        bus.writeByte(0xFF06.toUShort(), 0x42.toUByte())  // TMA = 0x42
        bus.writeByte(0xFF07.toUShort(), 0x05.toUByte())  // TAC: on, 262 144 Hz

        val timer = Timer(bus)
        timer.tick(16)

        assertEquals(0x42, bus.readByte(0xFF05.toUShort()).toInt(), "TIMA should reload from TMA on overflow")
        val interruptFlag = bus.readByte(0xFF0F.toUShort()).toInt()
        assertTrue(interruptFlag and 0x04 != 0, "Timer interrupt flag (bit 2 of IF) must be set on TIMA overflow")
    }

    @Test
    fun tima_overflow_resets_to_tma_and_keeps_counting() {
        val bus = makeBus()
        bus.writeByte(0xFF05.toUShort(), 0xFE.toUByte())  // TIMA = 254
        bus.writeByte(0xFF06.toUShort(), 0x0A.toUByte())  // TMA = 10
        bus.writeByte(0xFF07.toUShort(), 0x05.toUByte())  // TAC: on, 262 144 Hz

        val timer = Timer(bus)
        // 3 ticks: 254→255, 255→overflow(=10), 10→11
        timer.tick(16 * 3)

        assertEquals(11, bus.readByte(0xFF05.toUShort()).toInt(), "After overflow, timer counts from TMA upward")
    }
}
