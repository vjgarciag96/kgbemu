package com.vicgarci.kgbem.platform

import com.vicgarci.kgbem.emulator.EmulatorLoop

// LoopDriver is implemented per-platform to drive the emulator loop
// at the correct cadence (Choreographer, CADisplayLink, withFrameNanos, etc.)
// The common interface: start/stop the loop.
expect class LoopDriver(loop: EmulatorLoop) {
    fun start()
    fun stop()
}
