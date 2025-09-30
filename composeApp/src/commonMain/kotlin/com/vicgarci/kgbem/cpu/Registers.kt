package com.vicgarci.kgbem.cpu

data class Registers(
    var a: UByte,
    var b: UByte,
    var c: UByte,
    var d: UByte,
    var e: UByte,
    var f: UByte,
    var h: UByte,
    var l: UByte,
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

/**
 * @param zero true if the result of the last operation is equal to 0.
 * @param subtract true if the last operation was a subtraction.
 * @param carry true if the last operation resulted in an overflow.
 * @param halfCarry true if there is an overflow from the lower nibble (the lower 4 bits) to the
 * upper nibble (the upper 4 bits).
 */
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
                zero = register.toInt().ushr(ZERO_FLAG_BYTE_POSITION).and(0b1) != 0,
                subtract = register.toInt().ushr(SUBTRACT_FLAG_BYTE_POSITION).and(0b1) != 0,
                halfCarry = register.toInt().ushr(HALF_CARRY_FLAG_BYTE_POSITION).and(0b1) != 0,
                carry = register.toInt().ushr(CARRY_FLAG_BYTE_POSITION).and(0b1) != 0,
            )
        }

        fun UByte.toFlagsRegister(): FlagsRegister {
            return from(this)
        }

        fun FlagsRegister.toUByte(): UByte {
            var register = 0x00
            register = register or (if (zero) 0b1 else 0b0).shl(ZERO_FLAG_BYTE_POSITION)
            register = register or (if (subtract) 0b1 else 0b0).shl(SUBTRACT_FLAG_BYTE_POSITION)
            register = register or (if (halfCarry) 0b1 else 0b0).shl(HALF_CARRY_FLAG_BYTE_POSITION)
            register = register or (if (carry) 0b1 else 0b0).shl(CARRY_FLAG_BYTE_POSITION)
            return register.toUByte()
        }
    }
}