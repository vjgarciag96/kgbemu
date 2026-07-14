package com.vicgarci.kgbem.emulator

import com.vicgarci.kgbem.cartridge.Cartridge
import com.vicgarci.kgbem.cpu.CPU
import com.vicgarci.kgbem.cpu.MemoryBus
import com.vicgarci.kgbem.cpu.ProgramCounter
import com.vicgarci.kgbem.cpu.Registers
import com.vicgarci.kgbem.cpu.StackPointer
import com.vicgarci.kgbem.ppu.FrameSink

/**
 * Drives emulation one frame at a time.
 *
 * Each call to [runFrame] ticks the CPU until at least [CYCLES_PER_FRAME]
 * T-cycles have elapsed, then delivers a blank pixel buffer to the
 * [FrameSink] (PPU is not wired yet).
 */
class EmulatorLoop(
    cartridge: Cartridge,
    private val frameSink: FrameSink,
) {
    private val registers = Registers(
        a = 0u, b = 0u, c = 0u, d = 0u,
        e = 0u, f = 0u, h = 0u, l = 0u,
    )
    private val programCounter = ProgramCounter(0x0000.toUShort())
    private val stackPointer = StackPointer()
    private val bus = MemoryBus(cartridge)
    private val cpu = CPU(registers, programCounter, bus, stackPointer)

    init {
        CpuInitialiser.applyPostBootState(registers, programCounter, stackPointer, bus)
    }

    /**
     * Execute CPU steps until at least [CYCLES_PER_FRAME] T-cycles have
     * been consumed, then notify the [FrameSink] with a blank frame.
     */
    fun runFrame() {
        var elapsed = 0
        while (elapsed < CYCLES_PER_FRAME) {
            cpu.step()
            elapsed += NOP_CYCLES
        }
        frameSink.onFrame(IntArray(SCREEN_WIDTH * SCREEN_HEIGHT))
    }

    companion object {
        /** T-cycles in one Game Boy frame: 154 scanlines x 456 dots. */
        const val CYCLES_PER_FRAME = 70_224

        /** T-cycles consumed by a NOP instruction. */
        private const val NOP_CYCLES = 4

        private const val SCREEN_WIDTH = 160
        private const val SCREEN_HEIGHT = 144
    }
}
