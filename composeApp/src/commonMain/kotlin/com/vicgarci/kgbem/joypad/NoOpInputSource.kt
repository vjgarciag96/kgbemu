package com.vicgarci.kgbem.joypad

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * No-op [InputSource] used until the real touch overlay is wired (Task 9.1.1).
 */
@Inject
class NoOpInputSource : InputSource {
    override val state: StateFlow<JoypadState> = MutableStateFlow(JoypadState())
}
