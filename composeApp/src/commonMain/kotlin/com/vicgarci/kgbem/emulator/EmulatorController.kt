package com.vicgarci.kgbem.emulator

import com.vicgarci.kgbem.cartridge.Cartridge
import com.vicgarci.kgbem.cartridge.CartridgeLoadResult
import com.vicgarci.kgbem.cartridge.CartridgeLoader
import com.vicgarci.kgbem.di.AppScope
import com.vicgarci.kgbem.joypad.GameBoyButton
import com.vicgarci.kgbem.joypad.InputSource
import com.vicgarci.kgbem.joypad.TouchInputSource
import com.vicgarci.kgbem.platform.LoopDriver
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

    /**
     * Optional override for the injected [InputSource].
     * On Desktop, [main.kt] sets this to a [KeyboardInputSource] so that
     * keyboard events drive the joypad instead of (the DI-default) touch.
     */
    private var inputOverride: InputSource? = null

    /**
     * Replaces the DI-provided [InputSource] with [source].
     * Must be called before [loadRom] so the [EmulatorLoop] picks up
     * the correct source.
     */
    fun setInputOverride(source: InputSource) {
        inputOverride = source
    }

    /** The effective input source: override if set, otherwise DI-injected. */
    private val effectiveInput: InputSource
        get() = inputOverride ?: inputSource

    private val _emulatorState = MutableStateFlow<EmulatorState>(EmulatorState.Idle)
    val emulatorState: StateFlow<EmulatorState> = _emulatorState.asStateFlow()

    private val _frameState = MutableStateFlow<IntArray?>(null)
    val frameState: StateFlow<IntArray?> = _frameState.asStateFlow()

    /** The current loop instance, created on successful ROM load. */
    var loop: EmulatorLoop? = null
        private set

    private var loopDriver: LoopDriver? = null

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
                val emulatorLoop = EmulatorLoop(
                    cartridge = cart,
                    frameSink = this,
                    inputSource = effectiveInput,
                )
                loop = emulatorLoop
                loopDriver?.stop()
                val driver = LoopDriver(emulatorLoop)
                loopDriver = driver
                driver.start()
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
     * Forwards a button-press to the underlying [TouchInputSource].
     * No-op when the injected [InputSource] is not a [TouchInputSource]
     * (e.g. on Desktop where keyboard input is used instead).
     */
    fun buttonDown(button: GameBoyButton) {
        (inputSource as? TouchInputSource)?.onButtonDown(button)
    }

    /**
     * Forwards a button-release to the underlying [TouchInputSource].
     * No-op when the injected [InputSource] is not a [TouchInputSource].
     */
    fun buttonUp(button: GameBoyButton) {
        (inputSource as? TouchInputSource)?.onButtonUp(button)
    }

    /**
     * Pauses emulation. Only transitions if currently [EmulatorState.Running].
     */
    fun pause() {
        if (_emulatorState.value == EmulatorState.Running) {
            loopDriver?.stop()
            _emulatorState.value = EmulatorState.Paused
        }
    }

    /**
     * Resumes emulation. Only transitions if currently [EmulatorState.Paused].
     */
    fun resume() {
        if (_emulatorState.value == EmulatorState.Paused) {
            loopDriver?.start()
            _emulatorState.value = EmulatorState.Running
        }
    }
}
