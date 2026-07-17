package com.vicgarci.kgbem.joypad

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TouchInputSourceTest {

    private val source = TouchInputSource()

    // --- AC-1: onButtonDown sets the corresponding flag to true ---

    @Test
    fun `onButtonDown A sets a to true`() {
        source.onButtonDown(GameBoyButton.A)
        assertTrue(source.state.value.a)
    }

    @Test
    fun `onButtonDown sets each button independently`() {
        for (button in GameBoyButton.entries) {
            val src = TouchInputSource()
            src.onButtonDown(button)
            val state = src.state.value
            assertTrue(state.flagFor(button), "Expected $button to be true")
        }
    }

    // --- AC-2: onButtonUp after onButtonDown clears the flag ---

    @Test
    fun `onButtonUp A after onButtonDown A sets a to false`() {
        source.onButtonDown(GameBoyButton.A)
        source.onButtonUp(GameBoyButton.A)
        assertFalse(source.state.value.a)
    }

    @Test
    fun `onButtonUp clears each button independently`() {
        for (button in GameBoyButton.entries) {
            val src = TouchInputSource()
            src.onButtonDown(button)
            src.onButtonUp(button)
            val state = src.state.value
            assertFalse(state.flagFor(button), "Expected $button to be false after up")
        }
    }

    // --- AC-3: multiple simultaneous buttons ---

    @Test
    fun `LEFT and B can be held simultaneously`() {
        source.onButtonDown(GameBoyButton.LEFT)
        source.onButtonDown(GameBoyButton.B)

        val state = source.state.value
        assertTrue(state.left)
        assertTrue(state.b)
    }

    @Test
    fun `releasing one simultaneous button does not affect the other`() {
        source.onButtonDown(GameBoyButton.LEFT)
        source.onButtonDown(GameBoyButton.B)
        source.onButtonUp(GameBoyButton.LEFT)

        val state = source.state.value
        assertFalse(state.left)
        assertTrue(state.b)
    }

    // --- AC-4: no cross-contamination ---

    @Test
    fun `pressing A does not affect B`() {
        source.onButtonDown(GameBoyButton.A)

        val state = source.state.value
        assertTrue(state.a)
        assertFalse(state.b)
    }

    @Test
    fun `pressing a button leaves all other buttons unaffected`() {
        for (pressed in GameBoyButton.entries) {
            val src = TouchInputSource()
            src.onButtonDown(pressed)
            val state = src.state.value
            for (other in GameBoyButton.entries) {
                if (other != pressed) {
                    assertFalse(
                        state.flagFor(other),
                        "Pressing $pressed should not affect $other",
                    )
                }
            }
        }
    }

    // --- Initial state ---

    @Test
    fun `initial state has all buttons released`() {
        assertEquals(JoypadState(), source.state.value)
    }
}

/** Helper to read the flag corresponding to a [GameBoyButton]. */
private fun JoypadState.flagFor(button: GameBoyButton): Boolean =
    when (button) {
        GameBoyButton.RIGHT -> right
        GameBoyButton.LEFT -> left
        GameBoyButton.UP -> up
        GameBoyButton.DOWN -> down
        GameBoyButton.A -> a
        GameBoyButton.B -> b
        GameBoyButton.SELECT -> select
        GameBoyButton.START -> start
    }
