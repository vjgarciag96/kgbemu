package com.vicgarci.kgbem.platform

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual class PixelBuffer actual constructor(width: Int, height: Int) {

    private val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    private val imageBitmap = bitmap.asImageBitmap()
    private val stride = width

    actual fun updatePixels(pixels: IntArray) {
        bitmap.setPixels(pixels, 0, stride, 0, 0, bitmap.width, bitmap.height)
    }

    actual fun toImageBitmap(): ImageBitmap = imageBitmap
}
