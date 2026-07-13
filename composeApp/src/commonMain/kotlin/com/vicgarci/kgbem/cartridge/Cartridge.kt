package com.vicgarci.kgbem.cartridge

interface Cartridge {
    fun readRom(address: Int): Int
    fun writeRom(address: Int, value: Int)
    fun readRam(address: Int): Int
    fun writeRam(address: Int, value: Int)
    fun hasBattery(): Boolean
    fun savableState(): ByteArray?
    fun loadState(bytes: ByteArray)
}
