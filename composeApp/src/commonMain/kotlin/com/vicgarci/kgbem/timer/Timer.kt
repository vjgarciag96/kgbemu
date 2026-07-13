package com.vicgarci.kgbem.timer

class Timer {
    private var internalCounter: Int = 0 // 16-bit counter
    private var tima: Int = 0
    private var tma: Int = 0
    private var tac: Int = 0
    private var overflowDelay: Int = 0
    private var interruptRequested: Boolean = false

    fun readRegister(address: Int): Int = when (address) {
        0xFF04 -> (internalCounter ushr 8) and 0xFF // DIV = upper byte
        0xFF05 -> tima
        0xFF06 -> tma
        0xFF07 -> tac or 0xF8 // upper bits always read as 1
        else -> 0xFF
    }

    fun writeRegister(address: Int, value: Int) {
        when (address) {
            0xFF04 -> resetDiv()
            0xFF05 -> {
                tima = value and 0xFF
                overflowDelay = 0
            }
            0xFF06 -> tma = value and 0xFF
            0xFF07 -> tac = value and 0x07
        }
    }

    fun step(cycles: Int) {
        repeat(cycles) { tickOneCycle() }
    }

    fun consumeInterrupt(): Boolean {
        val result = interruptRequested
        interruptRequested = false
        return result
    }

    private fun tickOneCycle() {
        if (overflowDelay > 0) {
            overflowDelay--
            if (overflowDelay == 0) {
                tima = tma
                interruptRequested = true
            }
        }

        val prevCounter = internalCounter
        internalCounter = (internalCounter + 1) and 0xFFFF

        if (timerEnabled() && fallingEdge(prevCounter, internalCounter)) {
            incrementTima()
        }
    }

    private fun resetDiv() {
        val prevCounter = internalCounter
        internalCounter = 0
        if (timerEnabled() && (prevCounter and tacBit()) != 0) {
            incrementTima()
        }
    }

    private fun incrementTima() {
        tima++
        if (tima > 0xFF) {
            tima = 0
            overflowDelay = 4
        }
    }

    private fun timerEnabled(): Boolean = (tac and 0x04) != 0

    private fun tacBit(): Int = when (tac and 0x03) {
        0b00 -> 1 shl 9
        0b01 -> 1 shl 3
        0b10 -> 1 shl 5
        0b11 -> 1 shl 7
        else -> 0
    }

    private fun fallingEdge(prev: Int, curr: Int): Boolean {
        val bit = tacBit()
        return (prev and bit) != 0 && (curr and bit) == 0
    }
}
