package com.vicgarci.kgbem.cpu

data class OverflowAddResult(
    val sum: UShort,
    val carry: Boolean,
    val halfCarry: Boolean,
)

fun overflowAdd(first: UByte, second: UByte): OverflowAddResult {
    return overflowAdd(first.toUShort(), second.toUShort())
}

fun overflowAdd(first: UShort, second: UShort): OverflowAddResult {
    val sum: UInt = first + second
    val carry = sum > 0xFF.toUByte()
    val halfCarry = (first and 0xF.toUShort()) + (second and 0xF.toUShort()) > 0xF.toUByte()

    return OverflowAddResult(
        sum.toUShort(),
        carry,
        halfCarry,
    )
}