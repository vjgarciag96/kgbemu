package com.vicgarci.kgbem.cpu

class MemoryBus(
    internal val memory: Array<UByte> = Array(0x10000) { 0b0.toUByte() },
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
        when (address.toInt()) {
            0xFF04 -> memory[0xFF04] = 0.toUByte()  // Writing any value to DIV resets it to 0
            0xFF44 -> {}                    // LY is read-only from the game perspective
            0xFF46 -> { memory[0xFF46] = value; triggerDmaTransfer(value) }
            else -> memory[address.toInt()] = value
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

    /** Write LY directly — only the PPU should call this. */
    internal fun writeLY(value: UByte) {
        memory[0xFF44] = value
    }

    /** Load up to 32 KiB of cartridge ROM into the lower memory bank. */
    fun loadRom(data: ByteArray) {
        val limit = minOf(data.size, 0x8000)
        for (i in 0 until limit) {
            memory[i] = data[i].toUByte()
        }
    }

    /**
     * Set the hardware registers to their post-boot-ROM state so that cartridges
     * can run without the DMG boot ROM.
     */
    fun initializePostBoot() {
        memory[0xFF05] = 0x00.toUByte()  // TIMA
        memory[0xFF06] = 0x00.toUByte()  // TMA
        memory[0xFF07] = 0x00.toUByte()  // TAC
        memory[0xFF40] = 0x91.toUByte()  // LCDC – LCD on, BG tile data 0x8000, BG map 0x9800
        memory[0xFF41] = 0x85.toUByte()  // STAT
        memory[0xFF42] = 0x00.toUByte()  // SCY
        memory[0xFF43] = 0x00.toUByte()  // SCX
        memory[0xFF44] = 0x00.toUByte()  // LY
        memory[0xFF45] = 0x00.toUByte()  // LYC
        memory[0xFF47] = 0xFC.toUByte()  // BGP
        memory[0xFF48] = 0xFF.toUByte()  // OBP0
        memory[0xFF49] = 0xFF.toUByte()  // OBP1
        memory[0xFF4A] = 0x00.toUByte()  // WY
        memory[0xFF4B] = 0x00.toUByte()  // WX
        memory[0xFF0F] = 0xE1.toUByte()  // IF
        memory[0xFFFF] = 0x00.toUByte()  // IE
    }

    private fun triggerDmaTransfer(sourceHighByte: UByte) {
        val srcBase = sourceHighByte.toInt() shl 8
        for (i in 0 until 0xA0) {
            memory[0xFE00 + i] = memory[srcBase + i]
        }
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