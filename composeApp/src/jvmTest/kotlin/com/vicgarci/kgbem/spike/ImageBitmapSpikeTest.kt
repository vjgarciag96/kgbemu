package com.vicgarci.kgbem.spike

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ImageInfo
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * SPIKE-002 / OQ-2: Validate pixel-write APIs for Desktop (JVM).
 *
 * Confirms that an IntArray of ARGB values can be written into a displayable
 * ImageBitmap on the JVM/Desktop target using two approaches:
 *
 * 1. BufferedImage.setRGB() + toComposeImageBitmap()
 * 2. Skia Bitmap.installPixels() + asComposeImageBitmap()
 *
 * Both run headlessly with no window or display required.
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
            val r = (index % 256)
            val g = ((index / 256) % 256)
            val b = 0x42
            (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        val bufferedImage = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB)
        bufferedImage.setRGB(0, 0, WIDTH, HEIGHT, pixels, 0, WIDTH)

        assertEquals(pixels[0], bufferedImage.getRGB(0, 0), "Pixel (0,0) round-trip")
        assertEquals(pixels[1], bufferedImage.getRGB(1, 0), "Pixel (1,0) round-trip")
        assertEquals(pixels[WIDTH], bufferedImage.getRGB(0, 1), "Pixel (0,1) round-trip")
        assertEquals(
            pixels[PIXEL_COUNT - 1],
            bufferedImage.getRGB(WIDTH - 1, HEIGHT - 1),
            "Last pixel round-trip"
        )
    }

    @Test
    fun `BufferedImage converts to Compose ImageBitmap via toComposeImageBitmap`() {
        val argb = (0xFF shl 24) or (0xAA shl 16) or (0xBB shl 8) or 0xCC
        val pixels = IntArray(PIXEL_COUNT) { argb }

        val bufferedImage = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB)
        bufferedImage.setRGB(0, 0, WIDTH, HEIGHT, pixels, 0, WIDTH)

        val imageBitmap: ImageBitmap = bufferedImage.toComposeImageBitmap()

        assertEquals(WIDTH, imageBitmap.width)
        assertEquals(HEIGHT, imageBitmap.height)

        val pixelMap = imageBitmap.toPixelMap()
        val readBack = pixelMap[0, 0]
        assertEquals(0xAA / 255f, readBack.red, 0.01f, "Red channel")
        assertEquals(0xBB / 255f, readBack.green, 0.01f, "Green channel")
        assertEquals(0xCC / 255f, readBack.blue, 0.01f, "Blue channel")
        assertEquals(1.0f, readBack.alpha, 0.01f, "Alpha channel")
    }

    // --- Approach 2: Skia Bitmap ---

    @Test
    fun `Skia Bitmap installPixels writes data and converts to ImageBitmap`() {
        val argb = (0xFF shl 24) or (0x11 shl 16) or (0x22 shl 8) or 0x33
        val pixels = IntArray(PIXEL_COUNT) { argb }

        val skiaBitmap = Bitmap()
        skiaBitmap.allocPixels(
            ImageInfo.makeN32(WIDTH, HEIGHT, ColorAlphaType.UNPREMUL)
        )

        // Convert IntArray(ARGB) to ByteArray for installPixels.
        // N32 is platform-native order (BGRA on little-endian / macOS).
        val byteBuffer = ByteBuffer.allocate(PIXEL_COUNT * 4).order(ByteOrder.nativeOrder())
        for (pixel in pixels) {
            byteBuffer.putInt(pixel)
        }
        skiaBitmap.installPixels(byteBuffer.array())

        val imageBitmap: ImageBitmap = skiaBitmap.asComposeImageBitmap()

        assertEquals(WIDTH, imageBitmap.width)
        assertEquals(HEIGHT, imageBitmap.height)

        // Read back and check. N32 on macOS is BGRA, so writing ARGB ints
        // with native byte order may swap R and B channels.
        val pixelMap = imageBitmap.toPixelMap()
        val readBack = pixelMap[0, 0]
        val redMatch = kotlin.math.abs(readBack.red - 0x11 / 255f) < 0.02f
        val blueMatch = kotlin.math.abs(readBack.blue - 0x33 / 255f) < 0.02f

        if (!redMatch || !blueMatch) {
            // N32 is BGRA on this platform
            println("SPIKE FINDING: Skia N32 is BGRA on this platform.")
            println("  Written ARGB=0xFF112233, read back R=${"0x" + (readBack.red * 255).toInt().toString(16)}, " +
                "B=${"0x" + (readBack.blue * 255).toInt().toString(16)}")
            println("  -> BufferedImage.setRGB() + toComposeImageBitmap() handles ARGB natively.")
        }

        assertEquals(1.0f, readBack.alpha, 0.01f, "Alpha channel should be opaque")
    }

    // --- Approach 3: BufferedImage bulk vs per-pixel consistency ---

    @Test
    fun `BufferedImage setRGB bulk write matches per-pixel setRGB`() {
        val pixels = IntArray(PIXEL_COUNT) { index ->
            val shade = (index * 17) % 256
            (0xFF shl 24) or (shade shl 16) or (shade shl 8) or shade
        }

        val bulk = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB)
        bulk.setRGB(0, 0, WIDTH, HEIGHT, pixels, 0, WIDTH)

        val perPixel = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until HEIGHT) {
            for (x in 0 until WIDTH) {
                perPixel.setRGB(x, y, pixels[y * WIDTH + x])
            }
        }

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

    /**
     * Documents the Android API chain for reference (not runnable on JVM).
     *
     * Android equivalent:
     * ```
     * val bitmap = android.graphics.Bitmap.createBitmap(160, 144, Bitmap.Config.ARGB_8888)
     * bitmap.setPixels(pixels, 0, 160, 0, 0, 160, 144)
     * val pixel = bitmap.getPixel(0, 0)  // returns ARGB int
     * val imageBitmap: ImageBitmap = bitmap.asImageBitmap()  // from androidx.compose.ui.graphics
     * ```
     *
     * The Android Bitmap.setPixels() uses the same ARGB IntArray format as
     * BufferedImage.setRGB(), so the common PPU IntArray(160*144) buffer
     * works on both platforms without conversion.
     */
    @Test
    fun `document Android API chain`() {
        // This test exists only as documentation. The Android API names are:
        // - android.graphics.Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        // - bitmap.setPixels(pixels: IntArray, offset, stride, x, y, width, height)
        // - bitmap.getPixel(x, y): Int  (returns ARGB)
        // - bitmap.asImageBitmap(): ImageBitmap  (from androidx.compose.ui.graphics)
        //
        // These are confirmed by the Android SDK documentation and are not
        // testable in a JVM-only environment without Robolectric.
    }
}
