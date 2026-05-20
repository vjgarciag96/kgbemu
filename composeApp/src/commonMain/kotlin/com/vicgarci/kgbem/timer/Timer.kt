package com.vicgarci.kgbem.timer

import com.vicgarci.kgbem.cpu.MemoryBus

/**
 * Game Boy timer hardware.
 *
 * Registers (all in the I/O space):
 *   0xFF04 DIV  – Divider register, increments at 16 384 Hz (every 256 T-cycles).
 *                 Any write resets it to 0 (handled in MemoryBus.writeByte).
 *   0xFF05 TIMA – Timer counter, incremented at the frequency selected by TAC.
 *                 On overflow it resets to TMA and requests a timer interrupt.
 *   0xFF06 TMA  – Timer modulo; loaded into TIMA on overflow.
 *   0xFF07 TAC  – Timer control.
 *                   Bit 2 : timer enable (1 = running)
 *                   Bit 1–0 : clock select
 *                     00 → 4 096 Hz  (1 024 T-cycles per tick)
 *                     01 → 262 144 Hz (  16 T-cycles per tick)
 *                     10 →  65 536 Hz (  64 T-cycles per tick)
 *                     11 →  16 384 Hz ( 256 T-cycles per tick)
 */
class Timer(private val memoryBus: MemoryBus) {

    /** Internal 16-bit counter; upper 8 bits mirror the DIV register. */
    private var internalCounter: Int = 0

    /** Accumulated T-cycles for the TIMA increment logic. */
    private var timaAccumulator: Int = 0

    /** Advance the timer by [cycles] T-cycles. */
    fun tick(cycles: Int) {
        tickDiv(cycles)
        tickTima(cycles)
    }

    private fun tickDiv(cycles: Int) {
        val prevCounter = internalCounter
        internalCounter = (internalCounter + cycles) and 0xFFFF

        // Check if the game code reset DIV by writing to 0xFF04 (MemoryBus sets it to 0)
        val divRegister = memoryBus.memory[0xFF04].toInt()
        val expectedDiv = (internalCounter shr 8) and 0xFF
        if (divRegister == 0 && expectedDiv != 0 && prevCounter != 0) {
            // DIV was externally reset; align our counter
            internalCounter = 0
        }

        memoryBus.memory[0xFF04] = ((internalCounter shr 8) and 0xFF).toUByte()
    }

    private fun tickTima(cycles: Int) {
        val tac = memoryBus.readByte(TAC_ADDR).toInt()
        if (tac and TAC_ENABLE == 0) return  // Timer stopped

        val period = when (tac and TAC_CLOCK_MASK) {
            0 -> 1024   // 4 096 Hz
            1 -> 16     // 262 144 Hz
            2 -> 64     // 65 536 Hz
            3 -> 256    // 16 384 Hz
            else -> 1024
        }

        timaAccumulator += cycles
        while (timaAccumulator >= period) {
            timaAccumulator -= period
            val tima = memoryBus.readByte(TIMA_ADDR)
            if (tima == 0xFF.toUByte()) {
                val tma = memoryBus.readByte(TMA_ADDR)
                memoryBus.writeByte(TIMA_ADDR, tma)
                memoryBus.setInterruptFlagBit(TIMER_INTERRUPT_BIT, true)
            } else {
                memoryBus.writeByte(TIMA_ADDR, (tima + 1u).toUByte())
            }
        }
    }

    private companion object {
        private val TIMA_ADDR = 0xFF05.toUShort()
        private val TMA_ADDR = 0xFF06.toUShort()
        private val TAC_ADDR = 0xFF07.toUShort()

        private const val TAC_ENABLE = 0x04
        private const val TAC_CLOCK_MASK = 0x03
        private const val TIMER_INTERRUPT_BIT = 2
    }
}
