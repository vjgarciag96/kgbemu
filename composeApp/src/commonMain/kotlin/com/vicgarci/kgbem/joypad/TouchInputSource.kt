package com.vicgarci.kgbem.joypad

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * [InputSource] backed by touch events from the UI overlay.
 *
 * Call [onButtonDown] / [onButtonUp] from the touch-overlay composable
 * to update the joypad state that the emulator reads each frame.
 */
@Inject
class TouchInputSource : InputSource {

    private val _state = MutableStateFlow(JoypadState())
    override val state: StateFlow<JoypadState> = _state

    fun onButtonDown(button: GameBoyButton) {
        _state.update { it.withButton(button, pressed = true) }
    }

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
