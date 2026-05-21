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
        a = 0x01.toUByte(), b = 0x00.toUByte(), c = 0x13.toUByte(), d = 0x00.toUByte(),
        e = 0xD8.toUByte(), f = 0xB0.toUByte(), h = 0x01.toUByte(), l = 0x4D.toUByte(),
    )
    private val programCounter = ProgramCounter(0x0100.toUShort())
    private val stackPointer = StackPointer(0xFFFE.toUShort())
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
