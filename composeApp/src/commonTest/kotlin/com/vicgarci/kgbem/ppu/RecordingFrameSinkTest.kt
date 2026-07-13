package com.vicgarci.kgbem.ppu

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RecordingFrameSinkTest {

    @Test
    fun onFrame_storesFrameCopy() {
        val sink = RecordingFrameSink()
        val original = intArrayOf(1, 2, 3, 4, 5)
        sink.onFrame(original)

        original[0] = 99
        assertEquals(1, sink.frames[0][0], "Recorded frame should be a copy, unaffected by mutation of the original")
    }

    @Test
    fun lastFrame_isNullWhenEmpty() {
        val sink = RecordingFrameSink()
        assertNull(sink.lastFrame, "lastFrame should be null before any onFrame calls")
    }

    @Test
    fun lastFrame_returnsLastRecordedFrame() {
        val sink = RecordingFrameSink()
        sink.onFrame(intArrayOf(1))
        sink.onFrame(intArrayOf(2))
        sink.onFrame(intArrayOf(3))

        assertContentEquals(intArrayOf(3), sink.lastFrame, "lastFrame should return the most recently recorded frame")
    }

    @Test
    fun frames_tracksAllCalls() {
        val sink = RecordingFrameSink()
        repeat(5) { i ->
            sink.onFrame(intArrayOf(i))
        }
        assertEquals(5, sink.frames.size, "frames.size should equal the number of onFrame calls")
    }
}
