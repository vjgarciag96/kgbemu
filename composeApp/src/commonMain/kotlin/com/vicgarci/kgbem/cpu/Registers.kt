package com.vicgarci.kgbem.cpu

data class Registers(
    val a: UByte,
    val b: UByte,
    val c: UByte,
    val d: UByte,
    val e: UByte,
    val f: UByte,
    val h: UByte,
    val l: UByte,
) {

    val af: UShort
        get() = getVirtual16BitRegister(a, f)

    val bc: UShort
        get() = getVirtual16BitRegister(b, c)

    val de: UShort
        get() = getVirtual16BitRegister(d, e)

    val hl: UShort
        get() = getVirtual16BitRegister(h, l)

    private fun getVirtual16BitRegister(left: UByte, right: UByte): UShort {
        return ((left.toInt().shl(8)) or (right.toInt())).and(0xFFFF).toUShort()
    }
}