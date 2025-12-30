package com.vicgarci.kgbem.cpu

class MemoryBus(
    private val memory: Array<UByte> = Array(0x10000) { 0b0.toUByte() },
) {

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

    fun readByte(address: UShort): UByte {
        return memory[address.toInt()]
    }

    fun writeByte(address: UShort, value: UByte) {
        memory[address.toInt()] = value
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