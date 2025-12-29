package com.vicgarci.kgbem.cpu

data class SubtractResult(
    val value: UShort,
    val halfBorrow: Boolean,
    val borrow: Boolean,
)

fun sub(first: UByte, second: UByte, carry: UByte = 0u): SubtractResult {
    val result = first.toInt() - second.toInt() - carry.toInt()
    return SubtractResult(
        value = result.toUByte().toUShort(),
        halfBorrow = (first and 0x0F.toUByte()) < (second and 0x0F.toUByte()) + carry,
        borrow = result < 0,
    )
}

fun sub(first: UShort, second: UShort, carry: UShort = 0u): SubtractResult {
    val result = first.toInt() - second.toInt() - carry.toInt()
    return SubtractResult(
        value = result.toUShort(),
        halfBorrow = (first and 0x0FFF.toUShort()) < (second and 0x0FFF.toUShort()) + carry,
        borrow = result < 0,
    )
}