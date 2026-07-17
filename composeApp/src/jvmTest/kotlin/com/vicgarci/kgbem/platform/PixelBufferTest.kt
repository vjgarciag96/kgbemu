package com.vicgarci.kgbem.platform

import kotlin.test.Test
import kotlin.test.assertEquals

class PixelBufferTest {

    @Test
    fun `toImageBitmap returns correct dimensions`() {
        val buffer = PixelBuffer(160, 144)
        val pixels = IntArray(160 * 144) { 0xFF00FF00.toInt() }
        buffer.updatePixels(pixels)

        val bitmap = buffer.toImageBitmap()

        assertEquals(160, bitmap.width)
        assertEquals(144, bitmap.height)
    }

    @Test
    fun `updatePixels can be called repeatedly without error`() {
        val buffer = PixelBuffer(160, 144)
        val red = IntArray(160 * 144) { 0xFFFF0000.toInt() }
        val blue = IntArray(160 * 144) { 0xFF0000FF.toInt() }

        buffer.updatePixels(red)
        val first = buffer.toImageBitmap()

        buffer.updatePixels(blue)
        val second = buffer.toImageBitmap()

        assertEquals(160, first.width)
        assertEquals(160, second.width)
    }

    @Test
    fun `works with small dimensions`() {
        val buffer = PixelBuffer(1, 1)
        buffer.updatePixels(intArrayOf(0xFFFFFFFF.toInt()))

        val bitmap = buffer.toImageBitmap()

        assertEquals(1, bitmap.width)
        assertEquals(1, bitmap.height)
    }

    @Test
    fun `pixel data is written correctly`() {
        val buffer = PixelBuffer(2, 2)
        val pixels = intArrayOf(
            0xFFFF0000.toInt(), 0xFF00FF00.toInt(),
            0xFF0000FF.toInt(), 0xFFFFFFFF.toInt(),
        )
        buffer.updatePixels(pixels)

        val bitmap = buffer.toImageBitmap()
        val readBack = IntArray(4)
        bitmap.readPixels(readBack, 0, 0, 2, 2)

        // Compose ImageBitmap readPixels returns ARGB data
        assertEquals(0xFFFF0000.toInt(), readBack[0])
        assertEquals(0xFF00FF00.toInt(), readBack[1])
        assertEquals(0xFF0000FF.toInt(), readBack[2])
        assertEquals(0xFFFFFFFF.toInt(), readBack[3])
    }
}
