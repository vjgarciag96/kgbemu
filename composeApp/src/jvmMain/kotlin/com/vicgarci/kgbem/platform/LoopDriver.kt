package com.vicgarci.kgbem.platform

import com.vicgarci.kgbem.emulator.EmulatorLoop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

actual class LoopDriver actual constructor(private val loop: EmulatorLoop) {

    private var job: Job? = null

    actual fun start() {
        job?.cancel()
        job = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                val frameStart = System.nanoTime()
                loop.runFrame()
                val elapsed = System.nanoTime() - frameStart
                val remaining = FRAME_NANOS - elapsed
                if (remaining > 0) {
                    delay(remaining / 1_000_000)
                }
            }
        }
    }

    actual fun stop() {
        job?.cancel()
        job = null
    }

    private companion object {
        const val FRAME_NANOS = 1_000_000_000L / 59
    }
}
