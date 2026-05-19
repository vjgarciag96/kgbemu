package com.vicgarci.kgbem.emulator

import com.vicgarci.kgbem.cpu.CPU
import com.vicgarci.kgbem.cpu.MemoryBus
import com.vicgarci.kgbem.cpu.ProgramCounter
import com.vicgarci.kgbem.cpu.Registers
import com.vicgarci.kgbem.ppu.PPU
import com.vicgarci.kgbem.timer.Timer

/**
 * Top-level Game Boy emulator.
 *
 * Wires together the CPU, PPU, and Timer and exposes a simple step-based API
 * that the UI layer can drive at its own frame rate.
 *
 * Cycle accuracy note: each [CPU.step] is currently counted as 4 T-cycles
 * (the minimum real instruction length). This keeps the PPU/Timer timing
 * proportionally correct without requiring per-instruction cycle tables yet.
 */
class GameBoyEmulator {

    val memoryBus = MemoryBus()
    val ppu = PPU(memoryBus)

    private val timer = Timer(memoryBus)

    private val cpu = CPU(
        registers = Registers(
            a = 0x01.toUByte(), b = 0x00.toUByte(), c = 0x13.toUByte(), d = 0x00.toUByte(),
            e = 0xD8.toUByte(), f = 0xB0.toUByte(), h = 0x01.toUByte(), l = 0x4D.toUByte(),
        ),
        programCounter = ProgramCounter(0x0100.toUShort()),
        memoryBus = memoryBus,
    )

    init {
        memoryBus.initializePostBoot()
    }

    /**
     * Load a ROM image. The cartridge entry point (0x0100) is where execution
     * starts after the boot ROM; only MBC0 (≤32 KiB, no banking) is supported.
     */
    fun loadRom(data: ByteArray) {
        memoryBus.loadRom(data)
    }

    /**
     * Execute one complete frame worth of emulation (≈70 224 T-cycles).
     *
     * Returns `true` when V-Blank occurred and [ppu]`.framebuffer` was updated.
     */
    fun runFrame(): Boolean {
        var vblankSeen = false
        var cyclesThisFrame = 0
        while (cyclesThisFrame < CYCLES_PER_FRAME) {
            cpu.step()
            ppu.tick(T_CYCLES_PER_STEP)
            timer.tick(T_CYCLES_PER_STEP)
            cyclesThisFrame += T_CYCLES_PER_STEP

            if (ppu.vblankOccurred) {
                ppu.vblankOccurred = false
                vblankSeen = true
            }
        }
        return vblankSeen
    }

    /**
     * Execute CPU+PPU+Timer steps until V-Blank starts, then return.
     * Capped at two frames worth of cycles so a disabled LCD never blocks indefinitely.
     */
    fun runUntilVBlank() {
        val maxSteps = (CYCLES_PER_FRAME / T_CYCLES_PER_STEP) * 2
        var steps = 0
        do {
            cpu.step()
            ppu.tick(T_CYCLES_PER_STEP)
            timer.tick(T_CYCLES_PER_STEP)
            steps++
        } while (!ppu.vblankOccurred && steps < maxSteps)
        ppu.vblankOccurred = false
    }

    private companion object {
        /** Approximate T-cycles counted per CPU step (real average is ~6–8). */
        private const val T_CYCLES_PER_STEP = 4

        /** Total T-cycles in one DMG frame: 154 lines × 456 dots. */
        private const val CYCLES_PER_FRAME = 70_224
    }
}
