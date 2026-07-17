package com.vicgarci.kgbem.platform

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Platform-specific pixel buffer that holds a reusable backing bitmap.
 *
 * Created once per composable lifetime via `remember` so that no bitmap
 * allocation occurs on every frame. The PPU's ARGB [IntArray] is written
 * into the backing bitmap via [updatePixels], then [toImageBitmap] returns
 * a Compose-ready [ImageBitmap] without creating a new bitmap object.
 *
 * @param width  frame width in pixels (Game Boy = 160).
 * @param height frame height in pixels (Game Boy = 144).
 */
expect class PixelBuffer(width: Int, height: Int) {
    fun updatePixels(pixels: IntArray)
    fun toImageBitmap(): ImageBitmap
}
