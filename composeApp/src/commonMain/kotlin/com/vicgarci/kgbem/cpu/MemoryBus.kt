package com.vicgarci.kgbem.cpu

import com.vicgarci.kgbem.cartridge.Cartridge

class MemoryBus(
    private val cartridge: Cartridge,
    private val memory: Array<UByte> = Array(0x10000) { 0b0.toUByte() },
) {
    val interruptPendingMask: UByte
        get() = interruptEnable and interruptFlags and 0x1F.toUByte()

    private val interruptFlags: UByte
        get() = memory[0xFF0F]

    private val interruptEnable: UByte
        get() = memory[0xFFFF]

    fun readByte(address: UShort): UByte {
        val addr = address.toInt()
        return when {
            addr <= 0x7FFF -> cartridge.readRom(addr).toUByte()
            addr in 0xA000..0xBFFF -> cartridge.readRam(addr).toUByte()
            else -> memory[addr]
        }
    }

    fun writeByte(address: UShort, value: UByte) {
        val addr = address.toInt()
        val v = value.toInt()
        when {
            addr <= 0x7FFF -> cartridge.writeRom(addr, v)
            addr in 0xA000..0xBFFF -> cartridge.writeRam(addr, v)
            else -> memory[addr] = value
        }
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
