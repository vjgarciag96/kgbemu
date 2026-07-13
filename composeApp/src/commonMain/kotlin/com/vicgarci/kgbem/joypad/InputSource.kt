package com.vicgarci.kgbem.joypad

import kotlinx.coroutines.flow.StateFlow

interface InputSource {
    val state: StateFlow<JoypadState>
}
