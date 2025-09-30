package com.vicgarci.kgbem.cpu

import com.vicgarci.kgbem.cpu.FlagsRegister.Companion.toFlagsRegister
import com.vicgarci.kgbem.cpu.FlagsRegister.Companion.toUByte
import kotlin.test.Test
import kotlin.test.assertEquals

private val ALL_ZERO = Registers(
    0.toUByte(),
    0.toUByte(),
    0.toUByte(),
    0.toUByte(),
    0.toUByte(),
    0.toUByte(),
    0.toUByte(),
    0.toUByte(),
)

class RegistersTest {

    @Test
    fun getBc() {
        assertEquals(
            0xFFFF.toUShort(),
            ALL_ZERO.copy(
                b = 0xFF.toUByte(),
                c = 0xFF.toUByte(),
            ).bc,
        )

        assertEquals(
            0x1010.toUShort(),
            ALL_ZERO.copy(
                b = 0x10.toUByte(),
                c = 0x10.toUByte(),
            ).bc,
        )

        assertEquals(
            0x1001.toUShort(),
            ALL_ZERO.copy(
                b = 0x10.toUByte(),
                c = 0x01.toUByte(),
            ).bc,
        )
    }

    @Test
    fun flagsRegisterFromRegister() {
        assertEquals(
            FlagsRegister(
                zero = true,
                subtract = false,
                halfCarry = false,
                carry = false,
            ),
            ALL_ZERO.copy(f = 0b10000000.toUByte()).f.toFlagsRegister(),
        )

        assertEquals(
            FlagsRegister(
                zero = false,
                subtract = true,
                halfCarry = false,
                carry = false,
            ),
            ALL_ZERO.copy(f = 0b01000000.toUByte()).f.toFlagsRegister(),
        )

        assertEquals(
            FlagsRegister(
                zero = false,
                subtract = false,
                halfCarry = true,
                carry = false,
            ),
            ALL_ZERO.copy(f = 0b00100000.toUByte()).f.toFlagsRegister(),
        )

        assertEquals(
            FlagsRegister(
                zero = false,
                subtract = false,
                halfCarry = false,
                carry = true,
            ),
            ALL_ZERO.copy(f = 0b00010000.toUByte()).f.toFlagsRegister(),
        )

        assertEquals(
            FlagsRegister(
                zero = true,
                subtract = true,
                halfCarry = true,
                carry = true,
            ),
            ALL_ZERO.copy(f = 0b11110000.toUByte()).f.toFlagsRegister(),
        )
    }

    @Test
    fun flagsRegisterToUByte() {
        assertEquals(
            0xF0.toUByte(),
            FlagsRegister(
                zero = true,
                subtract = true,
                halfCarry = true,
                carry = true,
            ).toUByte(),
        )

        assertEquals(
            0x00.toUByte(),
            FlagsRegister(
                zero = false,
                subtract = false,
                halfCarry = false,
                carry = false,
            ).toUByte(),
        )

        assertEquals(
            0b11100000.toUByte(),
            FlagsRegister(
                zero = true,
                subtract = true,
                halfCarry = true,
                carry = false,
            ).toUByte(),
        )

        assertEquals(
            0b11010000.toUByte(),
            FlagsRegister(
                zero = true,
                subtract = true,
                halfCarry = false,
                carry = true,
            ).toUByte(),
        )

        assertEquals(
            0b10110000.toUByte(),
            FlagsRegister(
                zero = true,
                subtract = false,
                halfCarry = true,
                carry = true,
            ).toUByte(),
        )

        assertEquals(
            0b01110000.toUByte(),
            FlagsRegister(
                zero = false,
                subtract = true,
                halfCarry = true,
                carry = true,
            ).toUByte(),
        )
    }
}