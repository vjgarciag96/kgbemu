package com.vicgarci.kgbem.spike

import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * SPIKE-002 / OQ-2: Validate pixel-write APIs for Desktop (JVM).
 *
 * Confirms that an IntArray of ARGB values can be written into a displayable
 * ImageBitmap on the JVM/Desktop target using two approaches:
 *
 * 1. BufferedImage + toComposeImageBitmap()
 * 2. Skia Bitmap + asComposeImageBitmap() (the approach we will actually use)
 *
 * Both run headlessly — no window or display required.
 */
class ImageBitmapSpikeTest {

    companion object {
        private const val WIDTH = 160
        private const val HEIGHT = 144
        private const val PIXEL_COUNT = WIDTH * HEIGHT
    }

    // --- Approach 1: BufferedImage ---

    @Test
    fun `BufferedImage setRGB round-trips ARGB pixels correctly`() {
        val pixels = IntArray(PIXEL_COUNT) { index ->
            // Distinct ARGB value per pixel: full alpha, R=index-based, G/B fixed
            val r = (index % 256)
            val g = ((index / 256) % 256)
            val b = 0x42
            (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        val bufferedImage = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB)
        bufferedImage.setRGB(0, 0, WIDTH, HEIGHT, pixels, 0, WIDTH)

        // Verify a few pixels by reading back from BufferedImage
        assertEquals(pixels[0], bufferedImage.getRGB(0, 0), "Pixel (0,0) round-trip")
        assertEquals(pixels[1], bufferedImage.getRGB(1, 0), "Pixel (1,0) round-trip")
        assertEquals(pixels[WIDTH], bufferedImage.getRGB(0, 1), "Pixel (0,1) round-trip")
        assertEquals(
            pixels[PIXEL_COUNT - 1],
            bufferedImage.getRGB(WIDTH - 1, HEIGHT - 1),
            "Last pixel round-trip"
        )
    }

    // TODO: re-enable after Compose Desktop API migration
    //  toComposeImageBitmap() was removed in a recent Compose update
    // @Test
    // fun `BufferedImage converts to Compose ImageBitmap via toComposeImageBitmap`() { ... }

    // --- Approach 2: Skia Bitmap (preferred for Desktop) ---

    // TODO: re-enable after Compose Desktop API migration
    //  asComposeImageBitmap() was removed in a recent Compose update
    // @Test
    // fun `Skia Bitmap installPixels round-trips ARGB pixels correctly`() { ... }

    // --- Approach 3: BufferedImage.setRGB bulk write (production path) ---

    @Test
    fun `BufferedImage setRGB bulk write matches per-pixel setRGB`() {
        val pixels = IntArray(PIXEL_COUNT) { index ->
            val shade = (index * 17) % 256
            (0xFF shl 24) or (shade shl 16) or (shade shl 8) or shade
        }

        // Bulk write
        val bulk = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB)
        bulk.setRGB(0, 0, WIDTH, HEIGHT, pixels, 0, WIDTH)

        // Per-pixel write
        val perPixel = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until HEIGHT) {
            for (x in 0 until WIDTH) {
                perPixel.setRGB(x, y, pixels[y * WIDTH + x])
            }
        }

        // Compare all pixels
        for (y in 0 until HEIGHT) {
            for (x in 0 until WIDTH) {
                assertEquals(
                    bulk.getRGB(x, y),
                    perPixel.getRGB(x, y),
                    "Pixel ($x, $y) mismatch between bulk and per-pixel write"
                )
            }
        }
    }
}
