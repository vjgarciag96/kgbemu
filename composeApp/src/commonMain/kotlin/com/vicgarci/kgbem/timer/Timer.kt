package com.vicgarci.kgbem.timer

class Timer {
    private var internalCounter: Int = 0xABCC  // post-boot value

    val div: UByte get() = ((internalCounter shr 8) and 0xFF).toUByte()

    var tima: UByte = 0u
    var tma: UByte = 0u
    var tac: UByte = 0u

    var timerIrq: Boolean = false

    fun resetDiv() {
        internalCounter = 0
    }

    fun step(cycles: Int) {
        repeat(cycles) {
            val prev = internalCounter
            internalCounter = (internalCounter + 1) and 0xFFFF

            if (!timerEnabled) return@repeat
            val bit = timerBit
            val fallingEdge = ((prev shr bit) and 1) == 1 && ((internalCounter shr bit) and 1) == 0
            if (fallingEdge) {
                val next = tima.toInt() + 1
                if (next > 0xFF) {
                    tima = tma
                    timerIrq = true
                } else {
                    tima = next.toUByte()
                }
            }
        }
    }

    private val timerEnabled: Boolean get() = (tac.toInt() and 0x04) != 0
    private val timerBit: Int get() = when (tac.toInt() and 0x03) {
        0b00 -> 9
        0b01 -> 3
        0b10 -> 5
        0b11 -> 7
        else -> 9
    }
}
