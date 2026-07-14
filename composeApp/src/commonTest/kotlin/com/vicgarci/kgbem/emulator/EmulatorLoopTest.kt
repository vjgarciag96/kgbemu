package com.vicgarci.kgbem.emulator

import com.vicgarci.kgbem.cartridge.RomOnlyCartridge
import com.vicgarci.kgbem.ppu.RecordingFrameSink
import kotlin.test.Test
import kotlin.test.assertEquals

class EmulatorLoopTest {

    /**
     * Build a NOP-filled 32 KB ROM. Opcode 0x00 is NOP, so an all-zero
     * ByteArray is a valid stream of NOP instructions.
     */
    private fun nopRom(): RomOnlyCartridge {
        return RomOnlyCartridge(ByteArray(32 * 1024))
    }

    @Test
    fun runFrame_three_times_delivers_three_frames_to_sink() {
        val sink = RecordingFrameSink()
        val loop = EmulatorLoop(nopRom(), sink)

        loop.runFrame()
        loop.runFrame()
        loop.runFrame()

        assertEquals(3, sink.frames.size)
    }

    @Test
    fun runFrame_delivers_correctly_sized_pixel_buffer() {
        val sink = RecordingFrameSink()
        val loop = EmulatorLoop(nopRom(), sink)

        loop.runFrame()

        val frame = sink.lastFrame
        assertEquals(160 * 144, frame?.size)
    }

    @Test
    fun runFrame_does_not_throw() {
        val sink = RecordingFrameSink()
        val loop = EmulatorLoop(nopRom(), sink)

        // Should complete without exception
        loop.runFrame()
        loop.runFrame()
        loop.runFrame()
    }
}
