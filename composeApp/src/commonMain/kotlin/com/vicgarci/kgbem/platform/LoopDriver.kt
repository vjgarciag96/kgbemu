package com.vicgarci.kgbem.platform

// LoopDriver is implemented per-platform to drive the emulator loop
// at the correct cadence (Choreographer, CADisplayLink, withFrameNanos, etc.)
// The common interface: start/stop the loop.
expect class LoopDriver {
    fun start()
    fun stop()
}
