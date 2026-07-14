package com.vicgarci.kgbem.emulator

import com.vicgarci.kgbem.cartridge.RomOnlyCartridge
import com.vicgarci.kgbem.joypad.InputSource
import com.vicgarci.kgbem.joypad.JoypadState
import com.vicgarci.kgbem.ppu.RecordingFrameSink
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.test.Test
import kotlin.test.assertTrue

private class FakeInputSource : InputSource {
    val mutableState = MutableStateFlow(JoypadState())
    override val state: StateFlow<JoypadState> = mutableState
}

class EmulatorLoopJoypadTest {

    private fun nopRom(): RomOnlyCartridge {
        return RomOnlyCartridge(ByteArray(32 * 1024))
    }

    @Test
    fun runFrame_with_a_pressed_triggers_joypad_interrupt() {
        val input = FakeInputSource()
        val sink = RecordingFrameSink()
        val loop = EmulatorLoop(nopRom(), sink, input)

        input.mutableState.value = JoypadState(a = true)
        loop.runFrame()

        assertTrue(loop.joypadRegister.consumeInterrupt())
    }
}
