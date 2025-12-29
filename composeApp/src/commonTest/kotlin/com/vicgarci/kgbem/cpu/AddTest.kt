package com.vicgarci.kgbem.cpu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AddTest {

    @Test
    fun add_8bit_noCarry() {
        val result = overflowAdd(
            0x00.toUByte(),
            0x01.toUByte(),
        )

        assertEquals(0x01.toUShort(), result.sum)
        assertFalse(result.carry)
        assertFalse(result.halfCarry)
    }

    @Test
    fun add_8bit_halfCarry() {
        val result = overflowAdd(
            0x0F.toUByte(),
            0x01.toUByte(),
        )

        assertEquals(0x10.toUShort(), result.sum)
        assertFalse(result.carry)
        assertTrue(result.halfCarry)
    }

    @Test
    fun add_8bit_carry() {
        val result = overflowAdd(
            0xFF.toUByte(),
            0x01.toUByte(),
        )

        assertEquals(0.toUShort(), result.sum)
        assertTrue(result.carry)
        assertTrue(result.halfCarry)
    }

    @Test
    fun add_8bit_carryAndHalfCarry() {
        val result = overflowAdd(
            0x80.toUByte(),
            0x80.toUByte(),
        )

        assertEquals(0.toUShort(), result.sum)
        assertTrue(result.carry)
        assertFalse(result.halfCarry)
    }

    @Test
    fun add_8bit_halfCarryOnly() {
        val result = overflowAdd(
            0x08.toUByte(),
            0x08.toUByte(),
        )

        assertEquals(0x10.toUShort(), result.sum)
        assertFalse(result.carry)
        assertTrue(result.halfCarry)
    }

    @Test
    fun add_8bit_noFlags() {
        val result = overflowAdd(
            0x10.toUByte(),
            0x20.toUByte(),
        )

        assertEquals(0x30.toUShort(), result.sum)
        assertFalse(result.carry)
        assertFalse(result.halfCarry)
    }

    @Test
    fun add_8bit_withCarry() {
        val result = overflowAdd(
            0x0F.toUByte(),
            0x01.toUByte(),
            carry = 0x01.toUByte(),
        )

        assertEquals(0x11.toUShort(), result.sum)
        assertFalse(result.carry)
        assertTrue(result.halfCarry)
    }

    @Test
    fun add_16bit_noCarry() {
        val result = overflowAdd(
            0x0000.toUShort(),
            0x0001.toUShort(),
        )

        assertEquals(0x0001.toUShort(), result.sum)
        assertFalse(result.carry)
        assertFalse(result.halfCarry)
    }

    @Test
    fun add_16bit_halfCarry() {
        val result = overflowAdd(
            0x0FFF.toUShort(),
            0x0001.toUShort(),
        )

        assertEquals(0x1000.toUShort(), result.sum)
        assertFalse(result.carry)
        assertTrue(result.halfCarry)
    }

    @Test
    fun add_16bit_carry() {
        val result = overflowAdd(
            0xFFFF.toUShort(),
            0x0001.toUShort(),
        )

        assertEquals(0x0000.toUShort(), result.sum)
        assertTrue(result.carry)
        assertTrue(result.halfCarry)
    }

    @Test
    fun add_16bit_carryAndHalfCarry() {
        val result = overflowAdd(
            0x8000.toUShort(),
            0x8000.toUShort(),
        )

        assertEquals(0x0000.toUShort(), result.sum)
        assertTrue(result.carry)
        assertFalse(result.halfCarry)
    }

    @Test
    fun add_16bit_halfCarryOnly() {
        val result = overflowAdd(
            0x0800.toUShort(),
            0x0800.toUShort(),
        )

        assertEquals(0x1000.toUShort(), result.sum)
        assertFalse(result.carry)
        assertTrue(result.halfCarry)
    }

    @Test
    fun add_16bit_noFlags() {
        val result = overflowAdd(
            0x1000.toUShort(),
            0x2000.toUShort(),
        )

        assertEquals(0x3000.toUShort(), result.sum)
        assertFalse(result.carry)
        assertFalse(result.halfCarry)
    }

    @Test
    fun add_16bit_withCarry() {
        val result = overflowAdd(
            0x0FFF.toUShort(),
            0x0001.toUShort(),
            carry = 0x0001.toUShort(),
        )

        assertEquals(0x1001.toUShort(), result.sum)
        assertFalse(result.carry)
        assertTrue(result.halfCarry)
    }
}