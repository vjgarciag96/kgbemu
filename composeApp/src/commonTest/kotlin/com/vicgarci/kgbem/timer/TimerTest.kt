package com.vicgarci.kgbem.timer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TimerTest {

    private val timer = Timer()

    @Test
    fun div_incrementsEvery256Cycles() {
        // Reset DIV to get a known starting state
        timer.resetDiv()
        val initialDiv = timer.div.toInt()
        // Step 256 T-cycles
        timer.step(256)
        assertEquals((initialDiv + 1) and 0xFF, timer.div.toInt())
    }

    @Test
    fun resetDiv_setsToZero() {
        timer.step(512)  // advance a bit
        timer.resetDiv()
        assertEquals(0.toUByte(), timer.div)
    }

    @Test
    fun timerDisabled_timaDoesNotIncrement() {
        timer.tac = 0x00u  // disabled (bit 2 = 0)
        timer.tima = 0u
        timer.step(1024)
        assertEquals(0.toUByte(), timer.tima)
        assertFalse(timer.timerIrq)
    }

    @Test
    fun timerEnabled_mode0_4096Hz_incrementsCorrectly() {
        timer.resetDiv()
        timer.tac = 0x04u  // enabled, mode 0 (4096 Hz = every 1024 T-cycles, bit 9 falls every 512)
        timer.tima = 0u
        // At 4096 Hz with 4194304 Hz clock: 4194304/4096 = 1024 T-cycles per TIMA increment
        // Bit 9 of internal counter transitions from 1->0 every 512 cycles (falling edge)
        // Each falling edge = 1 TIMA tick. At mode 0 (bit 9), TIMA ticks at 4096 Hz = every 1024 cycles
        timer.step(1024)
        assertEquals(1.toUByte(), timer.tima)
    }

    @Test
    fun timerEnabled_mode1_262144Hz_incrementsCorrectly() {
        timer.resetDiv()
        timer.tac = 0x05u  // enabled, mode 1 (262144 Hz, bit 3)
        timer.tima = 0u
        // Mode 1: bit 3, frequency = 262144 Hz = every 16 T-cycles
        timer.step(16)
        assertEquals(1.toUByte(), timer.tima)
    }

    @Test
    fun timerOverflow_setsTimaToTmaAndFiresIrq() {
        timer.resetDiv()
        timer.tac = 0x05u  // enabled, mode 1 (fastest)
        timer.tima = 0xFFu
        timer.tma = 0x10u
        // One more tick should overflow
        timer.step(16)
        assertEquals(0x10.toUByte(), timer.tima)
        assertTrue(timer.timerIrq)
    }

    @Test
    fun timerIrq_canBeCleared() {
        timer.resetDiv()
        timer.tac = 0x05u
        timer.tima = 0xFFu
        timer.tma = 0u
        timer.step(16)
        assertTrue(timer.timerIrq)
        timer.timerIrq = false
        assertFalse(timer.timerIrq)
    }
}
