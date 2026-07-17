package com.vicgarci.kgbem.joypad

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * [InputSource] driven by touch events.
 *
 * The overlay composable (Task 9.1.2) calls [onButtonDown] / [onButtonUp]
 * when the user touches or releases a virtual button. This class is agnostic
 * about *where* the touch zones are — it only translates button events into
 * [JoypadState] updates.
 */
class TouchInputSource : InputSource {

    private val _state = MutableStateFlow(JoypadState())
    override val state: StateFlow<JoypadState> = _state.asStateFlow()

    /** Mark [button] as pressed. */
    fun onButtonDown(button: GameBoyButton) {
        _state.update { it.withButton(button, pressed = true) }
    }

    /** Mark [button] as released. */
    fun onButtonUp(button: GameBoyButton) {
        _state.update { it.withButton(button, pressed = false) }
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
