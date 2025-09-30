package com.vicgarci.kgbem.cpu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AddTest {

    @Test
    fun add_noCarry() {
        val result = overflowAdd(
            0x00.toUByte(),
            0x01.toUByte(),
        )

        assertEquals(0x01.toUByte(), result.sum)
        assertFalse(result.carry)
        assertFalse(result.halfCarry)
    }

    @Test
    fun add_halfCarry() {
        val result = overflowAdd(
            0x0F.toUByte().also { println(it) },
            0b1.toUByte().also { println(it) },
        )

        assertEquals(0x10.toUByte(), result.sum)
        assertFalse(result.carry)
        assertTrue(result.halfCarry)
    }

    @Test
    fun add_carry() {
        val result = overflowAdd(
            0xFF.toUByte().also { println(it) },
            0b1.toUByte().also { println(it) },
        )

        assertEquals(0x00.toUByte(), result.sum)
        assertTrue(result.carry)
        assertTrue(result.halfCarry)
    }
}