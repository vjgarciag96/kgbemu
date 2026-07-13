package com.vicgarci.kgbem.ppu

class RecordingFrameSink : FrameSink {
    private val _frames = mutableListOf<IntArray>()
    val frames: List<IntArray> get() = _frames
    val lastFrame: IntArray? get() = _frames.lastOrNull()

    override fun onFrame(pixels: IntArray) {
        _frames.add(pixels.copyOf())
    }
}
