package com.vicgarci.kgbem

import com.vicgarci.kgbem.cartridge.Cartridge
import com.vicgarci.kgbem.cpu.CPU
import com.vicgarci.kgbem.cpu.ProgramCounter
import com.vicgarci.kgbem.cpu.Registers
import com.vicgarci.kgbem.cpu.StackPointer
import com.vicgarci.kgbem.ppu.PPU
import com.vicgarci.kgbem.timer.Timer

class GameBoy(romBytes: UByteArray) {

    private val cartridge = Cartridge(romBytes)
    private val ppu = PPU()
    private val timer = Timer()
    val bus = GameBoyBus(cartridge, ppu, timer)

    // Post-boot CPU state (skipping boot ROM)
    private val registers = Registers(
        a = 0x01u, b = 0x00u, c = 0x13u, d = 0x00u,
        e = 0xD8u, f = 0xB0u, h = 0x01u, l = 0x4Du,
    )
    private val programCounter = ProgramCounter(0x0100u)
    private val stackPointer = StackPointer(0xFFFEu)
    val cpu = CPU(registers, programCounter, bus, stackPointer)

    val frameBuffer: IntArray get() = ppu.frameBuffer

    // Run enough steps for one full frame (~70224 T-cycles)
    fun runFrame() {
        var tCycles = 0
        while (tCycles < 70224) {
            val cycles = cpu.step()
            ppu.step(cycles)
            timer.step(cycles)
            bus.collectInterrupts()
            tCycles += cycles
        }
    }
}
