package com.vicgarci.kgbem.cpu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubtractTest {

    @Test
    fun subtract_8bit_noBorrow() {
        val result = sub(0xF1.toUByte(), 0x01.toUByte())

        assertEquals(0xF0.toUShort(), result.value)
        assertFalse(result.halfBorrow)
        assertFalse(result.borrow)
    }

    @Test
    fun subtract_8bit_halfBorrow() {
        val result = sub(0x10.toUByte(), 0x01.toUByte())

        assertEquals(0x0F.toUShort(), result.value)
        assertTrue(result.halfBorrow)
        assertFalse(result.borrow)
    }

    @Test
    fun subtract_8bit_borrow() {
        val result = sub(0x01.toUByte(), 0xF1.toUByte())

        assertEquals(0x10.toUShort(), result.value)
        assertFalse(result.halfBorrow)
        assertTrue(result.borrow)
    }

    @Test
    fun subtract_8bit_halfBorrowAndBorrow() {
        val result = sub(0x00.toUByte(), 0xF1.toUByte())

        assertEquals(0xF.toUShort(), result.value)
        assertTrue(result.halfBorrow)
        assertTrue(result.borrow)
    }

    @Test
    fun subtract_8bit_noFlags() {
        val result = sub(0x50.toUByte(), 0x20.toUByte())

        assertEquals(0x30.toUShort(), result.value)
        assertFalse(result.halfBorrow)
        assertFalse(result.borrow)
    }

    @Test
    fun subtract_8bit_halfBorrowOnly() {
        val result = sub(0x08.toUByte(), 0x0F.toUByte())

        assertEquals(0xF9.toUShort(), result.value)
        assertTrue(result.halfBorrow)
        assertTrue(result.borrow)
    }

    @Test
    fun subtract_8bit_withCarry() {
        val result = sub(
            0x10.toUByte(),
            0x01.toUByte(),
            carry = 0x01.toUByte(),
        )

        assertEquals(0x0E.toUShort(), result.value)
        assertTrue(result.halfBorrow)
        assertFalse(result.borrow)
    }

    @Test
    fun subtract_16bit_noBorrow() {
        val result = sub(0xF001.toUShort(), 0x0001.toUShort())

        assertEquals(0xF000.toUShort(), result.value)
        assertFalse(result.halfBorrow)
        assertFalse(result.borrow)
    }

    @Test
    fun subtract_16bit_halfBorrow() {
        val result = sub(0x1000.toUShort(), 0x0001.toUShort())

        assertEquals(0x0FFF.toUShort(), result.value)
        assertTrue(result.halfBorrow)
        assertFalse(result.borrow)
    }

    @Test
    fun subtract_16bit_borrow() {
        val result = sub(0x0001.toUShort(), 0xF001.toUShort())

        assertEquals(0x1000.toUShort(), result.value)
        assertFalse(result.halfBorrow)
        assertTrue(result.borrow)
    }

    @Test
    fun subtract_16bit_halfBorrowAndBorrow() {
        val result = sub(0x0000.toUShort(), 0xF001.toUShort())

        assertEquals(0x0FFF.toUShort(), result.value)
        assertTrue(result.halfBorrow)
        assertTrue(result.borrow)
    }

    @Test
    fun subtract_16bit_noFlags() {
        val result = sub(0x5000.toUShort(), 0x2000.toUShort())

        assertEquals(0x3000.toUShort(), result.value)
        assertFalse(result.halfBorrow)
        assertFalse(result.borrow)
    }

    @Test
    fun subtract_16bit_halfBorrowOnly() {
        val result = sub(0x0800.toUShort(), 0x0FFF.toUShort())

        assertEquals(0xF801.toUShort(), result.value)
        assertTrue(result.halfBorrow)
        assertTrue(result.borrow)
    }

    @Test
    fun subtract_16bit_withCarry() {
        val result = sub(
            0x1000.toUShort(),
            0x0001.toUShort(),
            carry = 0x0001.toUShort(),
        )

        assertEquals(0x0FFE.toUShort(), result.value)
        assertTrue(result.halfBorrow)
        assertFalse(result.borrow)
    }
}