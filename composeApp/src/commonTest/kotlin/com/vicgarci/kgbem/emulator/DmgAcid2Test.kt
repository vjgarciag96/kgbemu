package com.vicgarci.kgbem.emulator

import com.vicgarci.kgbem.cartridge.CartridgeLoadResult
import com.vicgarci.kgbem.cartridge.CartridgeLoader
import com.vicgarci.kgbem.joypad.InputSource
import com.vicgarci.kgbem.joypad.JoypadState
import com.vicgarci.kgbem.ppu.RecordingFrameSink
import com.vicgarci.kgbem.test.loadTestResource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private class Acid2NoOpInputSource : InputSource {
    override val state: StateFlow<JoypadState> = MutableStateFlow(JoypadState())
}

class DmgAcid2Test {

    @Test
    fun dmgAcid2RunsCleanlyFor200Frames() {
        val bytes = loadTestResource("dmg-acid2.gb")
        val result = CartridgeLoader.load(bytes)
        assertIs<CartridgeLoadResult.Success>(result)

        val sink = RecordingFrameSink()
        val emulatorLoop = EmulatorLoop(result.cartridge, sink, Acid2NoOpInputSource())

        repeat(200) {
            emulatorLoop.runFrame()
        }

        assertEquals(200, sink.frames.size)
    }
}
