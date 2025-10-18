package com.vicgarci.kgbem.cpu

class MemoryBus(
    private val memory: Array<UByte> = Array(0xFFFF) { 0b0.toUByte() },
) {

    fun readByte(address: UShort): UByte {
        return memory[address.toInt()]
    }
}