package com.vicgarci.kgbem.platform

import androidx.compose.ui.graphics.ImageBitmap

actual class PixelBuffer actual constructor(width: Int, height: Int) {
    actual fun updatePixels(pixels: IntArray): Unit = TODO("not yet implemented")
    actual fun toImageBitmap(): ImageBitmap = TODO("not yet implemented")
}
