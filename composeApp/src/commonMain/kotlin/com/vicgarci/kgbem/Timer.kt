package com.vicgarci.kgbem

import com.vicgarci.kgbem.cpu.MemoryBus

class Timer(private val memoryBus: MemoryBus) {

    private var divCycles = 0
    private var timaCycles = 0

    var timerInterrupt = false
        private set

    fun tick(cycles: Int) {
        timerInterrupt = false

        // DIV increments every 256 T-cycles (16384 Hz)
        divCycles += cycles
        if (divCycles >= DIV_PERIOD) {
            divCycles -= DIV_PERIOD
            memoryBus.incrementDiv()
        }

        val tac = memoryBus.readByte(TAC_ADDR).toInt()
        if (tac and 0x04 == 0) return // timer disabled

        val threshold = when (tac and 0x03) {
            0 -> 1024 // 4096 Hz
            1 -> 16   // 262144 Hz
            2 -> 64   // 65536 Hz
            else -> 256 // 16384 Hz
        }

        timaCycles += cycles
        while (timaCycles >= threshold) {
            timaCycles -= threshold
            val tima = memoryBus.readByte(TIMA_ADDR).toInt()
            if (tima == 0xFF) {
                memoryBus.writeByte(TIMA_ADDR, memoryBus.readByte(TMA_ADDR))
                timerInterrupt = true
            } else {
                memoryBus.writeByte(TIMA_ADDR, (tima + 1).toUByte())
            }
        }
    }

    companion object {
        val TIMA_ADDR = 0xFF05.toUShort()
        val TMA_ADDR = 0xFF06.toUShort()
        val TAC_ADDR = 0xFF07.toUShort()
        private const val DIV_PERIOD = 256
    }
}
