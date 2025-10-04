package com.vicgarci.kgbem.cpu

data class SubtractResult(
    val value: UShort,
    val halfBorrow: Boolean,
    val borrow: Boolean,
)

fun sub(first: UByte, second: UByte): SubtractResult {
    return sub(first.toUShort(), second.toUShort())
}

fun sub(first: UShort, second: UShort): SubtractResult {
    val value: UShort = if (second >= first) {
        0x0.toUShort()
    } else {
        (first - second).toUShort()
    }

    return SubtractResult(
        value = value,
        halfBorrow = (first and 0x0F.toUShort()) < (second and 0x0F.toUShort()),
        borrow = second >= first,
    )
}