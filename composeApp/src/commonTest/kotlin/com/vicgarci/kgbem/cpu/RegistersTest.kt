package com.vicgarci.kgbem.cpu

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
}