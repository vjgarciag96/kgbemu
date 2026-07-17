package com.vicgarci.kgbem.joypad

import androidx.compose.ui.input.key.Key
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * [InputSource] backed by keyboard events on Desktop.
 *
 * Call [onKeyDown] / [onKeyUp] from the Compose [Window]'s `onKeyEvent`
 * callback to update the joypad state that the emulator reads each frame.
 *
 * Key mapping:
 * - Arrow keys -> D-pad (LEFT, RIGHT, UP, DOWN)
 * - Z -> A
 * - X -> B
 * - Enter -> Start
 * - Right Shift -> Select
 */
class KeyboardInputSource : InputSource {

    private val _state = MutableStateFlow(JoypadState())
    override val state: StateFlow<JoypadState> = _state

    /**
     * Maps a Compose [Key] to its [GameBoyButton], or `null` if unmapped.
     */
    fun mapKey(key: Key): GameBoyButton? = when (key) {
        Key.DirectionLeft -> GameBoyButton.LEFT
        Key.DirectionRight -> GameBoyButton.RIGHT
        Key.DirectionUp -> GameBoyButton.UP
        Key.DirectionDown -> GameBoyButton.DOWN
        Key.Z -> GameBoyButton.A
        Key.X -> GameBoyButton.B
        Key.Enter -> GameBoyButton.START
        Key.ShiftRight -> GameBoyButton.SELECT
        else -> null
    }

    /**
     * Marks the button mapped to [key] as pressed.
     * Returns `true` if the key was a mapped Game Boy button.
     */
    fun onKeyDown(key: Key): Boolean {
        val button = mapKey(key) ?: return false
        _state.update { it.withButton(button, pressed = true) }
        return true
    }

    /**
     * Marks the button mapped to [key] as released.
     * Returns `true` if the key was a mapped Game Boy button.
     */
    fun onKeyUp(key: Key): Boolean {
        val button = mapKey(key) ?: return false
        _state.update { it.withButton(button, pressed = false) }
        return true
    }
}

private fun JoypadState.withButton(button: GameBoyButton, pressed: Boolean): JoypadState =
    when (button) {
        GameBoyButton.RIGHT -> copy(right = pressed)
        GameBoyButton.LEFT -> copy(left = pressed)
        GameBoyButton.UP -> copy(up = pressed)
        GameBoyButton.DOWN -> copy(down = pressed)
        GameBoyButton.A -> copy(a = pressed)
        GameBoyButton.B -> copy(b = pressed)
        GameBoyButton.SELECT -> copy(select = pressed)
        GameBoyButton.START -> copy(start = pressed)
    }
