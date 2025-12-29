package com.vicgarci.kgbem.cpu

data class OverflowAddResult(
    val sum: UShort,
    val carry: Boolean,
    val halfCarry: Boolean,
)

fun overflowAdd(first: UByte, second: UByte, carry: UByte = 0u): OverflowAddResult {
    val sum = first.toInt() + second.toInt() + carry.toInt()
    val carryOut = sum > 0xFF
    val halfCarry = (first and 0x0F.toUByte()) + (second and 0x0F.toUByte()) + carry > 0x0F.toUByte()

    return OverflowAddResult(
        sum.toUByte().toUShort(),
        carryOut,
        halfCarry,
    )
}

fun overflowAdd(first: UShort, second: UShort, carry: UShort = 0u): OverflowAddResult {
    val sum: UInt = first.toUInt() + second.toUInt() + carry.toUInt()
    val carryOut = sum > 0xFFFF.toUInt()
    val halfCarry = (first and 0x0FFF.toUShort()) + (second and 0x0FFF.toUShort()) + carry > 0x0FFF.toUShort()

    return OverflowAddResult(
        sum.toUShort(),
        carryOut,
        halfCarry,
    )
}