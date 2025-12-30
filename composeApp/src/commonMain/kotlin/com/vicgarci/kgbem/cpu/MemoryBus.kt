package com.vicgarci.kgbem.cpu

class MemoryBus(
    private val memory: Array<UByte> = Array(0x10000) { 0b0.toUByte() },
) {

    fun readByte(address: UShort): UByte {
        return memory[address.toInt()]
    }

    fun writeByte(address: UShort, value: UByte) {
        memory[address.toInt()] = value
    }
}