package com.vicgarci.kgbem.ppu

interface FrameSink {
    /** Called on the emulation thread once per frame. pixels is 160x144 ARGB, row-major. */
    fun onFrame(pixels: IntArray)
}
