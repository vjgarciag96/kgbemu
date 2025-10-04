package com.vicgarci.kgbem.cpu

data class SubtractResult(
    val value: UShort,
    val halfBorrow: Boolean,
    val borrow: Boolean,
)

fun sub(first: UByte, second: UByte): SubtractResult {
    val value: UShort = if (second >= first) {
        0x0.toUShort()
    } else {
        (first - second).toUShort()
    }

    return SubtractResult(
        value = value,
        halfBorrow = (first and 0x0F.toUByte()) < (second and 0x0F.toUByte()),
        borrow = second >= first,
    )
}