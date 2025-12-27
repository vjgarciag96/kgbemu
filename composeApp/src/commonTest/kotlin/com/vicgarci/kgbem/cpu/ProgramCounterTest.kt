package com.vicgarci.kgbem.cpu

import kotlin.test.Test
import kotlin.test.assertEquals

class ProgramCounterTest {

    @Test
    fun next() {
        val pc = ProgramCounter(0x0000.toUShort())

        val current = pc.next()
        val next = pc.next()

        assertEquals(0x0000.toUShort(), current)
        assertEquals(0x0001.toUShort(), next)
    }

    @Test
    fun next_wrapAround() {
        val pc = ProgramCounter(0xFFFF.toUShort())

        pc.next()
        val next = pc.next()

        assertEquals(0x0000.toUShort(), next)
    }

    @Test
    fun setTo() {
        val pc = ProgramCounter(0xBBBB.toUShort())

        val address = 0xABCD.toUShort()
        pc.setTo(address)
        val current = pc.next()

        assertEquals(address, current)
    }

    @Test
    fun increaseBy() {
        val pc = ProgramCounter(0xBBBB.toUShort())

        pc.increaseBy(stepSize = 3)
        val current = pc.next()

        assertEquals(0xBBBE.toUShort(), current)
    }

    @Test
    fun increaseBy_wrapAround() {
        val pc = ProgramCounter(0xFFFE.toUShort())

        pc.increaseBy(stepSize = 5)
        val current = pc.next()

        assertEquals(0x0003.toUShort(), current)
    }
}