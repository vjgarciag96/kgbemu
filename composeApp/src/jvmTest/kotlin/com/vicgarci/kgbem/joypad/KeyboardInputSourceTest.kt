package com.vicgarci.kgbem.joypad

import androidx.compose.ui.input.key.Key
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KeyboardInputSourceTest {

    private val source = KeyboardInputSource()

    // -------------------------------------------------------------------------
    // Key mapping
    // -------------------------------------------------------------------------

    @Test
    fun `arrow left maps to LEFT`() {
        assertEquals(GameBoyButton.LEFT, source.mapKey(Key.DirectionLeft))
    }

    @Test
    fun `arrow right maps to RIGHT`() {
        assertEquals(GameBoyButton.RIGHT, source.mapKey(Key.DirectionRight))
    }

    @Test
    fun `arrow up maps to UP`() {
        assertEquals(GameBoyButton.UP, source.mapKey(Key.DirectionUp))
    }

    @Test
    fun `arrow down maps to DOWN`() {
        assertEquals(GameBoyButton.DOWN, source.mapKey(Key.DirectionDown))
    }

    @Test
    fun `Z maps to A`() {
        assertEquals(GameBoyButton.A, source.mapKey(Key.Z))
    }

    @Test
    fun `X maps to B`() {
        assertEquals(GameBoyButton.B, source.mapKey(Key.X))
    }

    @Test
    fun `Enter maps to START`() {
        assertEquals(GameBoyButton.START, source.mapKey(Key.Enter))
    }

    @Test
    fun `ShiftRight maps to SELECT`() {
        assertEquals(GameBoyButton.SELECT, source.mapKey(Key.ShiftRight))
    }

    @Test
    fun `unmapped key returns null`() {
        assertNull(source.mapKey(Key.A))
    }

    // -------------------------------------------------------------------------
    // Key down sets button true
    // -------------------------------------------------------------------------

    @Test
    fun `onKeyDown sets corresponding button to true`() {
        source.onKeyDown(Key.DirectionUp)
        assertTrue(source.state.value.up, "UP should be true after key down")
    }

    @Test
    fun `onKeyDown returns true for mapped key`() {
        assertTrue(source.onKeyDown(Key.Z))
    }

    @Test
    fun `onKeyDown returns false for unmapped key`() {
        assertFalse(source.onKeyDown(Key.A))
    }

    @Test
    fun `onKeyDown does not affect other buttons`() {
        source.onKeyDown(Key.Z)
        val state = source.state.value
        assertTrue(state.a, "A should be true")
        assertFalse(state.b, "B should still be false")
        assertFalse(state.up, "UP should still be false")
        assertFalse(state.start, "START should still be false")
    }

    // -------------------------------------------------------------------------
    // Key up sets button false
    // -------------------------------------------------------------------------

    @Test
    fun `onKeyUp clears corresponding button`() {
        source.onKeyDown(Key.DirectionLeft)
        assertTrue(source.state.value.left)

        source.onKeyUp(Key.DirectionLeft)
        assertFalse(source.state.value.left, "LEFT should be false after key up")
    }

    @Test
    fun `onKeyUp returns true for mapped key`() {
        assertTrue(source.onKeyUp(Key.X))
    }

    @Test
    fun `onKeyUp returns false for unmapped key`() {
        assertFalse(source.onKeyUp(Key.Spacebar))
    }

    // -------------------------------------------------------------------------
    // Multiple simultaneous keys
    // -------------------------------------------------------------------------

    @Test
    fun `multiple keys can be held simultaneously`() {
        source.onKeyDown(Key.DirectionRight)
        source.onKeyDown(Key.Z)
        source.onKeyDown(Key.Enter)

        val state = source.state.value
        assertTrue(state.right, "RIGHT should be true")
        assertTrue(state.a, "A should be true")
        assertTrue(state.start, "START should be true")
        assertFalse(state.left, "LEFT should still be false")
    }

    @Test
    fun `releasing one key does not affect others`() {
        source.onKeyDown(Key.DirectionRight)
        source.onKeyDown(Key.Z)

        source.onKeyUp(Key.DirectionRight)

        val state = source.state.value
        assertFalse(state.right, "RIGHT should be false after release")
        assertTrue(state.a, "A should still be true")
    }

    // -------------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------------

    @Test
    fun `initial state has all buttons released`() {
        val state = source.state.value
        assertFalse(state.right)
        assertFalse(state.left)
        assertFalse(state.up)
        assertFalse(state.down)
        assertFalse(state.a)
        assertFalse(state.b)
        assertFalse(state.select)
        assertFalse(state.start)
    }
}
