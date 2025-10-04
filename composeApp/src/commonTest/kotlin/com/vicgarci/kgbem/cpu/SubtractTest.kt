package com.vicgarci.kgbem.cpu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubtractTest {

    @Test
    fun subtract_noBorrow() {
        val result = subtract(0xF1.toUByte(), 0x01.toUByte())

        assertEquals(0xF0.toUShort(), result.value)
        assertFalse(result.halfBorrow)
        assertFalse(result.borrow)
    }

    @Test
    fun subtract_halfBorrow() {
        val result = subtract(0x10.toUByte(), 0x01.toUByte())

        assertEquals(0x0F.toUShort(), result.value)
        assertTrue(result.halfBorrow)
        assertFalse(result.borrow)
    }

    @Test
    fun subtract_borrow() {
        val result = subtract(0x01.toUByte(), 0xF1.toUByte())

        assertEquals(0x0.toUShort(), result.value)
        assertFalse(result.halfBorrow)
        assertTrue(result.borrow)
    }

    @Test
    fun subtract_halfBorrow_borrow() {
        val result = subtract(0x00.toUByte(), 0xF1.toUByte())

        assertEquals(0x0.toUShort(), result.value)
        assertTrue(result.halfBorrow)
        assertTrue(result.borrow)
    }
}