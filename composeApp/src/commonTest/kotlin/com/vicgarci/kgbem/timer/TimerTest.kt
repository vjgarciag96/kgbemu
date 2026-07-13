package com.vicgarci.kgbem.timer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TimerTest {

    @Test
    fun div_incrementsEvery256Cycles() {
        val timer = Timer()
        timer.step(256)
        assertEquals(1, timer.readRegister(0xFF04))
    }

    @Test
    fun div_isUpperByteOf16BitCounter() {
        val timer = Timer()
        timer.step(512)
        assertEquals(2, timer.readRegister(0xFF04))
    }

    @Test
    fun writeToDivResetsCounter() {
        val timer = Timer()
        timer.step(300)
        timer.writeRegister(0xFF04, 0x00)
        assertEquals(0, timer.readRegister(0xFF04))
    }

    @Test
    fun tima_doesNotIncrementWhenDisabled() {
        val timer = Timer()
        timer.writeRegister(0xFF07, 0x00)
        timer.step(2048)
        assertEquals(0, timer.readRegister(0xFF05))
    }

    @Test
    fun tima_incrementsAt1024CyclesPerTick_mode00() {
        val timer = Timer()
        timer.writeRegister(0xFF07, 0x04)
        timer.step(1024)
        assertEquals(1, timer.readRegister(0xFF05))
    }

    @Test
    fun tima_incrementsAt16CyclesPerTick_mode01() {
        val timer = Timer()
        timer.writeRegister(0xFF07, 0x05)
        timer.step(16)
        assertEquals(1, timer.readRegister(0xFF05))
    }

    @Test
    fun tima_incrementsAt64CyclesPerTick_mode10() {
        val timer = Timer()
        timer.writeRegister(0xFF07, 0x06)
        timer.step(64)
        assertEquals(1, timer.readRegister(0xFF05))
    }

    @Test
    fun tima_incrementsAt256CyclesPerTick_mode11() {
        val timer = Timer()
        timer.writeRegister(0xFF07, 0x07)
        timer.step(256)
        assertEquals(1, timer.readRegister(0xFF05))
    }

    @Test
    fun tima_overflow_loadsFromTma_after4Cycles() {
        val timer = Timer()
        timer.writeRegister(0xFF07, 0x05) // enabled, mode 01 (16 T-cycles per tick)
        timer.writeRegister(0xFF06, 0x42) // TMA = 0x42
        timer.writeRegister(0xFF05, 0xFF) // TIMA = 0xFF

        // Step 16 T-cycles to trigger one TIMA increment (0xFF -> overflow)
        timer.step(16)

        // TIMA should be 0x00 during the 4-cycle delay
        assertEquals(0x00, timer.readRegister(0xFF05))

        // Step 4 more T-cycles to complete the overflow delay
        timer.step(4)

        // Now TIMA should be loaded from TMA
        assertEquals(0x42, timer.readRegister(0xFF05))
        assertTrue(timer.consumeInterrupt())
    }

    @Test
    fun tima_overflow_timaIs0_duringDelay() {
        val timer = Timer()
        timer.writeRegister(0xFF07, 0x05) // enabled, mode 01 (16 T-cycles per tick)
        timer.writeRegister(0xFF06, 0x42) // TMA = 0x42
        timer.writeRegister(0xFF05, 0xFF) // TIMA = 0xFF

        // Trigger overflow
        timer.step(16)

        // During the delay, TIMA should read 0x00
        assertEquals(0x00, timer.readRegister(0xFF05))
        assertFalse(timer.consumeInterrupt())

        // Step 2 cycles (still in delay)
        timer.step(2)
        assertEquals(0x00, timer.readRegister(0xFF05))
        assertFalse(timer.consumeInterrupt())
    }

    @Test
    fun divReset_triggersTima_whenBitWasHigh_mode00() {
        val timer = Timer()
        timer.writeRegister(0xFF07, 0x04) // enabled, mode 00 (bit 9 drives TIMA)

        // Step 512 cycles so counter = 512 = 0x0200; bit 9 is set
        timer.step(512)

        // TIMA should be 0 so far (first tick at 1024 cycles)
        assertEquals(0, timer.readRegister(0xFF05))

        // Write to DIV resets counter; bit 9 was high -> falling edge -> TIMA increments
        timer.writeRegister(0xFF04, 0x00)

        assertEquals(1, timer.readRegister(0xFF05))
    }

    @Test
    fun writingTima_cancelsPendingOverflow() {
        val timer = Timer()
        timer.writeRegister(0xFF07, 0x05) // enabled, mode 01 (16 T-cycles per tick)
        timer.writeRegister(0xFF06, 0x42) // TMA = 0x42
        timer.writeRegister(0xFF05, 0xFF) // TIMA = 0xFF

        // Trigger overflow
        timer.step(16)
        assertEquals(0x00, timer.readRegister(0xFF05))

        // Write to TIMA during the delay period -- cancels pending overflow
        timer.writeRegister(0xFF05, 0x10)

        // Step past when overflow would have resolved
        timer.step(4)

        // TIMA should be the value we wrote, NOT TMA
        assertEquals(0x10, timer.readRegister(0xFF05))
        assertFalse(timer.consumeInterrupt())
    }
}
