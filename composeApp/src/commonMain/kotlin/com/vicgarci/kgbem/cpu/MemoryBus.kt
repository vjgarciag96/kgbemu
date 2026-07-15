package com.vicgarci.kgbem.cpu

import com.vicgarci.kgbem.cartridge.Cartridge
import com.vicgarci.kgbem.joypad.JoypadRegister

class MemoryBus(
    private val cartridge: Cartridge,
    private val memory: Array<UByte> = Array(0x10000) { 0b0.toUByte() },
    private val joypadRegister: JoypadRegister? = null,
) {
    /** True while an OAM DMA transfer is in progress. */
    var dmaActive: Boolean = false
        private set

    /** Remaining T-cycles until the current DMA transfer completes. */
    var dmaRemainingCycles: Int = 0
        private set

    val interruptPendingMask: UByte
        get() = interruptEnable and interruptFlags and 0x1F.toUByte()

    private val interruptFlags: UByte
        get() = memory[0xFF0F]

    private val interruptEnable: UByte
        get() = memory[0xFFFF]

    /**
     * Advance the DMA transfer by [cycles] T-cycles.
     * When [dmaRemainingCycles] reaches zero the transfer is complete
     * and [dmaActive] is cleared.
     */
    fun advanceDma(cycles: Int) {
        if (!dmaActive) return
        dmaRemainingCycles -= cycles
        if (dmaRemainingCycles <= 0) {
            dmaRemainingCycles = 0
            dmaActive = false
        }
    }

    fun readByte(address: UShort): UByte {
        val addr = address.toInt()
        // During DMA, only HRAM (0xFF80-0xFFFE) is accessible
        if (dmaActive && addr !in 0xFF80..0xFFFE) {
            return 0xFF.toUByte()
        }
        return when {
            addr <= 0x7FFF -> cartridge.readRom(addr).toUByte()
            addr in 0xA000..0xBFFF -> cartridge.readRam(addr).toUByte()
            addr == 0xFF00 && joypadRegister != null -> joypadRegister.read().toUByte()
            else -> memory[addr]
        }
    }

    fun writeByte(address: UShort, value: UByte) {
        val addr = address.toInt()
        val v = value.toInt()
        // During DMA, only HRAM (0xFF80-0xFFFE) writes are allowed
        // (except the DMA trigger itself at 0xFF46)
        if (dmaActive && addr !in 0xFF80..0xFFFE && addr != 0xFF46) {
            return
        }
        when {
            addr == 0xFF46 -> triggerOamDma(value)
            addr <= 0x7FFF -> cartridge.writeRom(addr, v)
            addr in 0xA000..0xBFFF -> cartridge.writeRam(addr, v)
            addr == 0xFF00 && joypadRegister != null -> joypadRegister.write(v)
            else -> memory[addr] = value
        }
    }

    /**
     * Trigger an OAM DMA transfer: copy 160 bytes from the source page
     * into OAM (0xFE00-0xFE9F) and mark the bus as DMA-active for 640 T-cycles.
     */
    private fun triggerOamDma(value: UByte) {
        val sourceBase = value.toInt() shl 8
        for (i in 0 until 160) {
            val srcAddr = sourceBase + i
            val byte = when {
                srcAddr <= 0x7FFF -> cartridge.readRom(srcAddr).toUByte()
                srcAddr in 0xA000..0xBFFF -> cartridge.readRam(srcAddr).toUByte()
                else -> memory[srcAddr]
            }
            memory[0xFE00 + i] = byte
        }
        memory[0xFF46] = value
        dmaActive = true
        dmaRemainingCycles = 640
    }

    fun anyInterruptPending(): Boolean {
        return interruptPendingMask != 0b0.toUByte()
    }

    fun setInterruptFlagBit(bitPosition: Int, value: Boolean) {
        setBit(0xFF0F.toUShort(), bitPosition, value)
    }

    fun setInterruptEnableBit(bitPosition: Int, value: Boolean) {
        setBit(0xFFFF.toUShort(), bitPosition, value)
    }

    private fun MemoryBus.setBit(address: UShort, bitPosition: Int, value: Boolean) {
        val currentValue = readByte(address).toInt()
        val mask = 1 shl bitPosition
        val newValue = if (value) {
            currentValue or mask
        } else {
            currentValue and mask.inv()
        }
        writeByte(address, newValue.toUByte())
    }
}
