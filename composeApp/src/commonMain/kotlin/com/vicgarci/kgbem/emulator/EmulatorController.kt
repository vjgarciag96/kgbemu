package com.vicgarci.kgbem.emulator

import com.vicgarci.kgbem.cartridge.Cartridge
import com.vicgarci.kgbem.cartridge.CartridgeLoadResult
import com.vicgarci.kgbem.cartridge.CartridgeLoader
import com.vicgarci.kgbem.di.AppScope
import com.vicgarci.kgbem.joypad.InputSource
import com.vicgarci.kgbem.platform.SaveStorage
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

    private var cartridge: Cartridge? = null
    private var romTitle: String = ""

    override fun onFrame(pixels: IntArray) {
        _frameState.value = pixels
    }

    /**
     * Loads a ROM from raw bytes. On success, restores any saved battery RAM,
     * creates an [EmulatorLoop], and transitions to [EmulatorState.Running].
     * On failure, transitions to [EmulatorState.Error].
     */
    fun loadRom(bytes: ByteArray) {
        _emulatorState.value = EmulatorState.Loading
        romTitle = extractRomTitle(bytes)
        when (val result = CartridgeLoader.load(bytes)) {
            is CartridgeLoadResult.Success -> {
                val cart = result.cartridge
                cartridge = cart
                if (cart.hasBattery()) {
                    try {
                        SaveStorage.load(romTitle)?.let { cart.loadState(it) }
                    } catch (_: Exception) {
                        // Corrupt save data — start with clean RAM state.
                    }
                }
                loop = EmulatorLoop(
                    cartridge = cart,
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
     * Persists battery RAM to [SaveStorage] if the loaded cartridge supports it.
     * Safe to call when no ROM is loaded (no-op).
     */
    fun saveGame() {
        val cart = cartridge ?: return
        if (!cart.hasBattery()) return
        val data = cart.savableState() ?: return
        SaveStorage.save(romTitle, data)
    }

    private fun extractRomTitle(bytes: ByteArray): String {
        if (bytes.size < 0x0144) return "rom"
        return bytes.copyOfRange(0x0134, 0x0144)
            .takeWhile { it != 0.toByte() }
            .map { (it.toInt() and 0xFF).toChar() }
            .filter { it.isLetterOrDigit() || it == ' ' || it == '_' || it == '-' }
            .joinToString("")
            .trim()
            .take(16)
            .ifEmpty { "rom" }
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
