package com.vicgarci.kgbem.cpu

import com.vicgarci.kgbem.Cartridge

class MemoryBus(
    private val memory: Array<UByte> = Array(0x10000) { 0b0.toUByte() },
    private var cartridge: Cartridge? = null,
) {

    fun loadCartridge(cartridge: Cartridge) {
        this.cartridge = cartridge
    }

    /**
     * Get the interrupt pending mask, which indicates which interrupts are both
     * enabled and requested.
     */
    val interruptPendingMask: UByte
        get() = interruptEnable and interruptFlags and 0x1F.toUByte()

    /**
     * Get the interrupt flags register (IF, located at address 0xFF0F). This register
     * indicates which interrupts are requested.
     *
     * The bits in this register correspond to the following interrupts:
     * - Bit 0: V-Blank Interrupt
     * - Bit 1: LCD STAT Interrupt
     * - Bit 2: Timer Interrupt
     * - Bit 3: Serial Interrupt
     * - Bit 4: Joypad Interrupt
     * - Bit 5-7: Unused (always read as 0)
     */
    private val interruptFlags: UByte
        get() = memory[0xFF0F]

    /**
     * Get the interrupt enable register (IE, located at address 0xFFFF). This register
     * enables or disables the individual interrupt sources.
     */
    private val interruptEnable: UByte
        get() = memory[0xFFFF]

    // DIV register (0xFF04) is tracked separately so Timer can increment without reset
    private var divRegister: UByte = 0u

    fun readDiv(): UByte = divRegister
    fun incrementDiv() { divRegister = (divRegister + 1u).toUByte() }
    fun resetDiv() { divRegister = 0u }

    fun readByte(address: UShort): UByte {
        val addr = address.toInt() and 0xFFFF
        if (addr == 0xFF04) return divRegister
        val cart = cartridge
        return if (cart != null && addr < 0x8000) cart.read(address) else memory[addr]
    }

    fun writeByte(address: UShort, value: UByte) {
        val addr = address.toInt() and 0xFFFF
        when {
            addr == 0xFF04 -> divRegister = 0u // any write to DIV resets it to 0
            addr < 0x8000 && cartridge != null -> { /* ROM is read-only for MBC0 */ }
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