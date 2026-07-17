package com.vicgarci.kgbem.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.image.BufferedImage

actual class PixelBuffer actual constructor(private val width: Int, private val height: Int) {

    private val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

    actual fun updatePixels(pixels: IntArray) {
        bufferedImage.setRGB(0, 0, width, height, pixels, 0, width)
    }

    actual fun toImageBitmap(): ImageBitmap = bufferedImage.toComposeImageBitmap()
}
