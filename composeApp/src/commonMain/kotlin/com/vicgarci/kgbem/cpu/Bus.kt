package com.vicgarci.kgbem.cpu

interface Bus {
    fun readByte(address: UShort): UByte
    fun writeByte(address: UShort, value: UByte)
    val interruptPendingMask: UByte
    fun anyInterruptPending(): Boolean
    fun setInterruptFlagBit(bitPosition: Int, value: Boolean)
    fun setInterruptEnableBit(bitPosition: Int, value: Boolean)
}
