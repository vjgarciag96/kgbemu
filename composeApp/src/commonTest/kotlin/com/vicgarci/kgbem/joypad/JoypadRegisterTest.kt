package com.vicgarci.kgbem.joypad

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JoypadRegisterTest {
    private val reg = JoypadRegister()

    // Select helpers using write().
    // In the reference implementation:
    //   bit5 = 0 means directions selected
    //   bit4 = 0 means buttons selected
    // After write(), selectBits = value and 0x30.
    //   Directions: write(0x10) -> selectBits=0x10 -> read base = 0xC0|0x10 = 0xD0
    //   Buttons:    write(0x20) -> selectBits=0x20 -> read base = 0xC0|0x20 = 0xE0
    //   Both:       write(0x00) -> selectBits=0x00 -> read base = 0xC0|0x00 = 0xC0
    //   None:       write(0x30) -> selectBits=0x30 -> read base = 0xC0|0x30 = 0xF0, nibble=0xF -> 0xFF
    private fun selectDirections() = reg.write(0x10)
    private fun selectButtons() = reg.write(0x20)
    private fun selectBoth() = reg.write(0x00)
    private fun selectNone() = reg.write(0x30)

    @Test
    fun read_noButtonsPressed_directionsSelected_returnsAllBitsHigh() {
        selectDirections()
        assertEquals(0xDF, reg.read())
    }

    @Test
    fun read_rightPressed_directionsSelected_bit0Low() {
        selectDirections()
        reg.update(JoypadState(right = true))
        assertEquals(0xDE, reg.read())
    }

    @Test
    fun read_leftPressed_directionsSelected_bit1Low() {
        selectDirections()
        reg.update(JoypadState(left = true))
        assertEquals(0xDD, reg.read())
    }

    @Test
    fun read_upPressed_directionsSelected_bit2Low() {
        selectDirections()
        reg.update(JoypadState(up = true))
        assertEquals(0xDB, reg.read())
    }

    @Test
    fun read_downPressed_directionsSelected_bit3Low() {
        selectDirections()
        reg.update(JoypadState(down = true))
        assertEquals(0xD7, reg.read())
    }

    @Test
    fun read_aPressed_buttonsSelected_bit0Low() {
        selectButtons()
        reg.update(JoypadState(a = true))
        assertEquals(0xEE, reg.read())
    }

    @Test
    fun read_bPressed_buttonsSelected_bit1Low() {
        selectButtons()
        reg.update(JoypadState(b = true))
        assertEquals(0xED, reg.read())
    }

    @Test
    fun read_selectPressed_buttonsSelected_bit2Low() {
        selectButtons()
        reg.update(JoypadState(select = true))
        assertEquals(0xEB, reg.read())
    }

    @Test
    fun read_startPressed_buttonsSelected_bit3Low() {
        selectButtons()
        reg.update(JoypadState(start = true))
        assertEquals(0xE7, reg.read())
    }

    @Test
    fun read_bothSelectBitsLow_returnsAndedNibble() {
        selectBoth()
        reg.update(JoypadState(right = true, a = true))
        val result = reg.read() and 0x0F
        assertEquals(0x0E, result)
    }

    @Test
    fun read_bothSelectBitsLow_onlyRightPressed_returnsAndedNibble() {
        selectBoth()
        reg.update(JoypadState(right = true))
        val result = reg.read() and 0x0F
        assertEquals(0x0E, result)
    }

    @Test
    fun read_bothSelectBitsLow_upperBitsCorrect() {
        selectBoth()
        reg.update(JoypadState(right = true))
        assertEquals(0xCE, reg.read())
    }

    @Test
    fun update_pressingButton_setsInterruptFlag() {
        reg.update(JoypadState(a = true))
        assertTrue(reg.consumeInterrupt())
    }

    @Test
    fun update_holdingButton_doesNotRepeatInterrupt() {
        reg.update(JoypadState(a = true))
        reg.consumeInterrupt()
        reg.update(JoypadState(a = true))
        assertFalse(reg.consumeInterrupt())
    }

    @Test
    fun update_releasingButton_doesNotSetInterrupt() {
        reg.update(JoypadState(a = true))
        reg.consumeInterrupt()
        reg.update(JoypadState(a = false))
        assertFalse(reg.consumeInterrupt())
    }

    @Test
    fun read_nothingSelected_returns0xFF() {
        selectNone()
        assertEquals(0xFF, reg.read())
    }
}
