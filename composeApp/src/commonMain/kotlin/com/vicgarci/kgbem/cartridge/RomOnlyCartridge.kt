package com.vicgarci.kgbem.cartridge

class RomOnlyCartridge(romData: ByteArray) : Cartridge {

    private val rom: ByteArray = romData.copyOf()

    override fun readRom(address: Int): Int = rom[address].toInt() and 0xFF

    override fun writeRom(address: Int, value: Int) {
        // No-op: ROM-only cartridge does not support writes
    }

    override fun readRam(address: Int): Int = 0xFF

    override fun writeRam(address: Int, value: Int) {
        // No-op: ROM-only cartridge has no external RAM
    }

    override fun hasBattery(): Boolean = false

    override fun savableState(): ByteArray? = null

    override fun loadState(bytes: ByteArray) {
        // No-op: ROM-only cartridge has no savable state
    }
}
