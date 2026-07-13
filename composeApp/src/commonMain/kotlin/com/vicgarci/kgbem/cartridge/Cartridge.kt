package com.vicgarci.kgbem.cartridge

/**
 * Represents a Game Boy cartridge (ROM + optional RAM + optional mapper).
 *
 * Address pre-conditions: all address parameters are pre-validated by [MemoryBus].
 * - [readRom] and [writeRom] receive addresses in 0x0000–0x7FFF only.
 * - [readRam] and [writeRam] receive addresses in 0xA000–0xBFFF only.
 * Implementations may assume these ranges without additional bounds checking.
 */
interface Cartridge {
    fun readRom(address: Int): Int
    fun writeRom(address: Int, value: Int)
    fun readRam(address: Int): Int
    fun writeRam(address: Int, value: Int)
    fun hasBattery(): Boolean
    fun savableState(): ByteArray?
    fun loadState(bytes: ByteArray)
}
