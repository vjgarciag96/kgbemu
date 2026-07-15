package com.vicgarci.kgbem.cartridge

class Mbc1Cartridge(
    romData: ByteArray,
    private val typeId: Int
) : Cartridge {

    private val rom: ByteArray = romData.copyOf()

    private val romBankCount: Int = 2 shl (rom[ROM_SIZE_ADDR].toInt() and 0xFF)

    private val ramSize: Int = when (rom[RAM_SIZE_ADDR].toInt() and 0xFF) {
        0x02 -> RAM_BANK_SIZE            // 8 KB  (1 bank)
        0x03 -> RAM_BANK_SIZE * 4        // 32 KB (4 banks)
        else -> 0
    }

    private val ram: ByteArray = ByteArray(ramSize)

    private var ramEnabled: Boolean = false
    private var romBankLow: Int = 1      // lower 5 bits, 0 remapped to 1
    private var bankingRegister: Int = 0  // upper 2 bits (mode 0 = ROM high bits, mode 1 = RAM bank)
    private var mode: Int = 0            // 0 = mode 0, 1 = mode 1

    // --- Cartridge interface ---

    override fun readRom(address: Int): Int {
        return when {
            address < 0x4000 -> {
                // Bank 0 region: in mode 1 the upper bits shift bank 0 area
                val bank = if (mode == 1) (bankingRegister shl 5) % romBankCount else 0
                val physicalAddress = bank * ROM_BANK_SIZE + address
                readRomByte(physicalAddress)
            }
            else -> {
                // Bank N region (0x4000-0x7FFF)
                val bank = effectiveRomBank()
                val offset = address - 0x4000
                val physicalAddress = bank * ROM_BANK_SIZE + offset
                readRomByte(physicalAddress)
            }
        }
    }

    override fun writeRom(address: Int, value: Int) {
        when {
            address < 0x2000 -> {
                // RAM enable: 0x0A in lower nibble enables, anything else disables
                ramEnabled = (value and 0x0F) == 0x0A
            }
            address < 0x4000 -> {
                // ROM bank number (lower 5 bits)
                var bank = value and 0x1F
                if (bank == 0) bank = 1
                romBankLow = bank
            }
            address < 0x6000 -> {
                // Upper 2 bits: ROM bank high bits (mode 0) or RAM bank (mode 1)
                bankingRegister = value and 0x03
            }
            else -> {
                // Mode select (0x6000-0x7FFF)
                mode = value and 0x01
            }
        }
    }

    override fun readRam(address: Int): Int {
        if (!ramEnabled || ramSize == 0) return 0xFF
        val bank = if (mode == 1) bankingRegister else 0
        val offset = address - 0xA000
        val physicalAddress = bank * RAM_BANK_SIZE + offset
        return if (physicalAddress < ramSize) ram[physicalAddress].toInt() and 0xFF else 0xFF
    }

    override fun writeRam(address: Int, value: Int) {
        if (!ramEnabled || ramSize == 0) return
        val bank = if (mode == 1) bankingRegister else 0
        val offset = address - 0xA000
        val physicalAddress = bank * RAM_BANK_SIZE + offset
        if (physicalAddress < ramSize) {
            ram[physicalAddress] = value.toByte()
        }
    }

    override fun hasBattery(): Boolean = typeId == TYPE_MBC1_RAM_BATTERY

    override fun savableState(): ByteArray? {
        return if (ramSize > 0 && hasBattery()) ram.copyOf() else null
    }

    override fun loadState(bytes: ByteArray) {
        if (ramSize > 0 && hasBattery()) {
            bytes.copyInto(ram, endIndex = minOf(bytes.size, ram.size))
        }
    }

    // --- Helpers ---

    private fun effectiveRomBank(): Int {
        val raw = (bankingRegister shl 5) or romBankLow
        // Remap: 0x00->0x01, 0x20->0x21, 0x40->0x41, 0x60->0x61
        // Already handled by romBankLow never being 0
        return raw % romBankCount
    }

    private fun readRomByte(physicalAddress: Int): Int {
        return if (physicalAddress < rom.size) rom[physicalAddress].toInt() and 0xFF else 0xFF
    }

    companion object {
        const val TYPE_MBC1 = 0x01
        const val TYPE_MBC1_RAM = 0x02
        const val TYPE_MBC1_RAM_BATTERY = 0x03

        private const val ROM_SIZE_ADDR = 0x0148
        private const val RAM_SIZE_ADDR = 0x0149
        private const val ROM_BANK_SIZE = 0x4000   // 16 KB
        private const val RAM_BANK_SIZE = 0x2000    // 8 KB
    }
}
