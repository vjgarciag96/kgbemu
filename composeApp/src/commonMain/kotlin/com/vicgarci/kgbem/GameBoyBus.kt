package com.vicgarci.kgbem

import com.vicgarci.kgbem.cartridge.Cartridge
import com.vicgarci.kgbem.cpu.Bus
import com.vicgarci.kgbem.ppu.PPU
import com.vicgarci.kgbem.timer.Timer

class GameBoyBus(
    private val cartridge: Cartridge,
    private val ppu: PPU,
    private val timer: Timer,
) : Bus {

    private val wram = UByteArray(0x2000)
    private val hram = UByteArray(0x7F)
    private var ie: UByte = 0u
    private var interruptFlags: UByte = 0xE1u  // post-boot IF

    // I/O stubs
    private var joypad: UByte = 0xCFu
    private var sb: UByte = 0u
    private var sc: UByte = 0u

    override val interruptPendingMask: UByte
        get() = (interruptFlags.toInt() and ie.toInt() and 0x1F).toUByte()

    override fun anyInterruptPending(): Boolean = interruptPendingMask != 0.toUByte()

    override fun setInterruptFlagBit(bitPosition: Int, value: Boolean) {
        val mask = 1 shl bitPosition
        interruptFlags = if (value) (interruptFlags.toInt() or mask).toUByte()
                         else (interruptFlags.toInt() and mask.inv()).toUByte()
    }

    override fun setInterruptEnableBit(bitPosition: Int, value: Boolean) {
        val mask = 1 shl bitPosition
        ie = if (value) (ie.toInt() or mask).toUByte()
             else (ie.toInt() and mask.inv()).toUByte()
    }

    override fun readByte(address: UShort): UByte {
        val addr = address.toInt() and 0xFFFF
        return when (addr) {
            in 0x0000..0x7FFF -> cartridge.readRomByte(addr)
            in 0x8000..0x9FFF -> ppu.readVram(address)
            in 0xA000..0xBFFF -> 0xFF.toUByte()
            in 0xC000..0xDFFF -> wram[addr - 0xC000]
            in 0xE000..0xFDFF -> wram[addr - 0xE000]
            in 0xFE00..0xFE9F -> ppu.readOam(address)
            in 0xFEA0..0xFEFF -> 0xFF.toUByte()
            0xFF00 -> joypad
            0xFF01 -> sb
            0xFF02 -> sc
            0xFF04 -> timer.div
            0xFF05 -> timer.tima
            0xFF06 -> timer.tma
            0xFF07 -> timer.tac
            0xFF0F -> interruptFlags
            0xFF40 -> ppu.lcdc
            0xFF41 -> ppu.stat
            0xFF42 -> ppu.scy
            0xFF43 -> ppu.scx
            0xFF44 -> ppu.ly
            0xFF45 -> ppu.lyc
            0xFF47 -> ppu.bgp
            0xFF48 -> ppu.obp0
            0xFF49 -> ppu.obp1
            0xFF4A -> ppu.wy
            0xFF4B -> ppu.wx
            in 0xFF80..0xFFFE -> hram[addr - 0xFF80]
            0xFFFF -> ie
            else -> 0xFF.toUByte()
        }
    }

    override fun writeByte(address: UShort, value: UByte) {
        val addr = address.toInt() and 0xFFFF
        when (addr) {
            in 0x0000..0x7FFF -> { /* ROM writes ignored for MBC0 */ }
            in 0x8000..0x9FFF -> ppu.writeVram(address, value)
            in 0xA000..0xBFFF -> { /* no external RAM */ }
            in 0xC000..0xDFFF -> wram[addr - 0xC000] = value
            in 0xE000..0xFDFF -> wram[addr - 0xE000] = value
            in 0xFE00..0xFE9F -> ppu.writeOam(address, value)
            in 0xFEA0..0xFEFF -> { /* unusable */ }
            0xFF00 -> joypad = value
            0xFF01 -> sb = value
            0xFF02 -> sc = value
            0xFF04 -> timer.resetDiv()
            0xFF05 -> { timer.tima = value }
            0xFF06 -> { timer.tma = value }
            0xFF07 -> { timer.tac = value }
            0xFF0F -> interruptFlags = value
            0xFF40 -> ppu.lcdc = value
            0xFF41 -> ppu.stat = ((ppu.stat.toInt() and 0x07) or (value.toInt() and 0xF8)).toUByte()
            0xFF42 -> ppu.scy = value
            0xFF43 -> ppu.scx = value
            0xFF44 -> ppu.resetLy()
            0xFF45 -> ppu.lyc = value
            0xFF46 -> dmaTransfer(value)
            0xFF47 -> ppu.bgp = value
            0xFF48 -> ppu.obp0 = value
            0xFF49 -> ppu.obp1 = value
            0xFF4A -> ppu.wy = value
            0xFF4B -> ppu.wx = value
            in 0xFF80..0xFFFE -> hram[addr - 0xFF80] = value
            0xFFFF -> ie = value
            else -> { /* ignore */ }
        }
    }

    private fun dmaTransfer(source: UByte) {
        val srcBase = source.toInt() shl 8
        for (i in 0 until 0xA0) {
            val byte = readByte((srcBase + i).toUShort())
            ppu.writeOam((0xFE00 + i).toUShort(), byte)
        }
    }

    fun collectInterrupts() {
        if (ppu.vblankIrq) { setInterruptFlagBit(0, true); ppu.vblankIrq = false }
        if (ppu.statIrq) { setInterruptFlagBit(1, true); ppu.statIrq = false }
        if (timer.timerIrq) { setInterruptFlagBit(2, true); timer.timerIrq = false }
    }
}
