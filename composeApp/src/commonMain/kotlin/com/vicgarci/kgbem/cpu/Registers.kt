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

data class FlagsRegister(
    val zero: Boolean,
    val subtract: Boolean,
    val halfCarry: Boolean,
    val carry: Boolean,
) {

    companion object {

        private const val ZERO_FLAG_BYTE_POSITION = 7
        private const val SUBTRACT_FLAG_BYTE_POSITION = 6
        private const val HALF_CARRY_FLAG_BYTE_POSITION = 5
        private const val CARRY_FLAG_BYTE_POSITION = 4

        fun from(register: UByte): FlagsRegister {
            return FlagsRegister(
                zero = register.toInt().shr(ZERO_FLAG_BYTE_POSITION).and(0b1) != 0,
                subtract = register.toInt().shr(SUBTRACT_FLAG_BYTE_POSITION).and(0b1) != 0,
                halfCarry = register.toInt().shr(HALF_CARRY_FLAG_BYTE_POSITION).and(0b1) != 0,
                carry = register.toInt().shr(CARRY_FLAG_BYTE_POSITION).and(0b1) != 0,
            )
        }

        fun UByte.toFlagsRegister(): FlagsRegister {
            return from(this)
        }
    }
}