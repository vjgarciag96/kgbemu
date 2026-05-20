package com.vicgarci.kgbem

import com.vicgarci.kgbem.cpu.MemoryBus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TimerTest {

    private val memoryBus = MemoryBus()
    private val timer = Timer(memoryBus)

    @Test
    fun div_increments_every_256_cycles() {
        assertEquals(0u, memoryBus.readDiv())
        timer.tick(256)
        assertEquals(1u, memoryBus.readDiv())
        timer.tick(512)
        assertEquals(3u, memoryBus.readDiv())
    }

    @Test
    fun div_wraps_at_256() {
        repeat(256) { timer.tick(256) }
        assertEquals(0u, memoryBus.readDiv())
    }

    @Test
    fun writing_to_div_address_resets_div() {
        timer.tick(256) // DIV becomes 1
        assertEquals(1u, memoryBus.readDiv())
        memoryBus.writeByte(0xFF04.toUShort(), 0xFFu) // any write resets DIV
        assertEquals(0u, memoryBus.readDiv())
    }

    @Test
    fun timer_disabled_does_not_increment_tima() {
        memoryBus.writeByte(Timer.TAC_ADDR, 0x00u) // timer off (bit 2 = 0)
        timer.tick(10000)
        assertEquals(0u, memoryBus.readByte(Timer.TIMA_ADDR))
        assertFalse(timer.timerInterrupt)
    }

    @Test
    fun tima_increments_at_4096Hz_when_tac_is_00() {
        // 4096 Hz = every 1024 T-cycles
        memoryBus.writeByte(Timer.TAC_ADDR, 0x04u) // timer on, clock select = 00
        timer.tick(1024)
        assertEquals(1u, memoryBus.readByte(Timer.TIMA_ADDR))
        timer.tick(1024)
        assertEquals(2u, memoryBus.readByte(Timer.TIMA_ADDR))
    }

    @Test
    fun tima_increments_at_262144Hz_when_tac_is_01() {
        // 262144 Hz = every 16 T-cycles
        memoryBus.writeByte(Timer.TAC_ADDR, 0x05u) // timer on, clock select = 01
        timer.tick(16)
        assertEquals(1u, memoryBus.readByte(Timer.TIMA_ADDR))
    }

    @Test
    fun tima_overflow_fires_interrupt_and_reloads_tma() {
        memoryBus.writeByte(Timer.TMA_ADDR, 0x10u)   // TMA = 16
        memoryBus.writeByte(Timer.TIMA_ADDR, 0xFFu)  // TIMA = 255 (one more overflows)
        memoryBus.writeByte(Timer.TAC_ADDR, 0x04u)   // timer on, 4096 Hz

        timer.tick(1024) // advance one TIMA increment → overflow

        assertTrue(timer.timerInterrupt)
        assertEquals(0x10u, memoryBus.readByte(Timer.TIMA_ADDR)) // reloaded from TMA
    }

    @Test
    fun timer_interrupt_clears_on_next_tick() {
        memoryBus.writeByte(Timer.TIMA_ADDR, 0xFFu)
        memoryBus.writeByte(Timer.TAC_ADDR, 0x04u)
        timer.tick(1024)
        assertTrue(timer.timerInterrupt)
        timer.tick(1) // next tick with no overflow
        assertFalse(timer.timerInterrupt)
    }
}
