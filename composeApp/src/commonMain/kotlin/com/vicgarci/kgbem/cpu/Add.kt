package com.vicgarci.kgbem.cpu

data class OverflowAddResult(
    val sum: UByte,
    val carry: Boolean,
    val halfCarry: Boolean,
)

fun overflowAdd(first: UByte, second: UByte): OverflowAddResult {
    val sum: UInt = first + second
    val carry = sum > 0xFF.toUByte()
    val halfCarry = (first and 0xF.toUByte()) + (second and 0xF.toUByte()) > 0xF.toUByte()

    return OverflowAddResult(
        sum.toUByte(),
        carry,
        halfCarry,
    )
}