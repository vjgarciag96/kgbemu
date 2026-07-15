package com.vicgarci.kgbem.emulator

import com.vicgarci.kgbem.cartridge.CartridgeLoadResult
import com.vicgarci.kgbem.cartridge.CartridgeLoader
import com.vicgarci.kgbem.di.AppScope
import com.vicgarci.kgbem.joypad.InputSource
import com.vicgarci.kgbem.ppu.FrameSink
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages emulator lifecycle and state.
 *
 * Owns the [EmulatorLoop] instance and acts as the [FrameSink] so that each
 * rendered frame is published to [frameState]. The actual loop timing is
 * driven externally by LoopDriver (Task 4.1.4).
 */
@Inject
@SingleIn(AppScope::class)
class EmulatorController(
    private val inputSource: InputSource,
) : FrameSink {

    private val _emulatorState = MutableStateFlow<EmulatorState>(EmulatorState.Idle)
    val emulatorState: StateFlow<EmulatorState> = _emulatorState.asStateFlow()

    private val _frameState = MutableStateFlow<IntArray?>(null)
    val frameState: StateFlow<IntArray?> = _frameState.asStateFlow()

    /** The current loop instance, created on successful ROM load. */
    var loop: EmulatorLoop? = null
        private set

    override fun onFrame(pixels: IntArray) {
        _frameState.value = pixels
    }

    /**
     * Loads a ROM from raw bytes. On success, creates an [EmulatorLoop] and
     * transitions to [EmulatorState.Running]. On failure, transitions to
     * [EmulatorState.Error].
     */
    fun loadRom(bytes: ByteArray) {
        _emulatorState.value = EmulatorState.Loading
        when (val result = CartridgeLoader.load(bytes)) {
            is CartridgeLoadResult.Success -> {
                loop = EmulatorLoop(
                    cartridge = result.cartridge,
                    frameSink = this,
                    inputSource = inputSource,
                )
                _emulatorState.value = EmulatorState.Running
            }
            is CartridgeLoadResult.Failure -> {
                _emulatorState.value = EmulatorState.Error(result.error)
            }
        }
    }

    /**
     * Pauses emulation. Only transitions if currently [EmulatorState.Running].
     */
    fun pause() {
        if (_emulatorState.value == EmulatorState.Running) {
            _emulatorState.value = EmulatorState.Paused
        }
    }

    /**
     * Resumes emulation. Only transitions if currently [EmulatorState.Paused].
     */
    fun resume() {
        if (_emulatorState.value == EmulatorState.Paused) {
            _emulatorState.value = EmulatorState.Running
        }
    }
}
