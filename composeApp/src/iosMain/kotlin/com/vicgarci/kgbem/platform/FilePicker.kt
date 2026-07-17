package com.vicgarci.kgbem.platform

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypeData
import platform.darwin.NSObject
import platform.posix.memcpy
import kotlin.coroutines.resume

/**
 * Presents a system document picker and returns the selected file as a [ByteArray],
 * or null if the user cancels.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual suspend fun pickRomFile(): ByteArray? = suspendCancellableCoroutine { cont ->
    val picker = UIDocumentPickerViewController(
        forOpeningContentTypes = listOf(UTTypeData),
        asCopy = true,
    )

    val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
        override fun documentPicker(
            controller: UIDocumentPickerViewController,
            didPickDocumentsAtURLs: List<*>,
        ) {
            val url = didPickDocumentsAtURLs.firstOrNull() as? platform.Foundation.NSURL
            if (url == null) {
                cont.resume(null)
                return
            }
            val nsData = NSData.dataWithContentsOfURL(url)
            if (nsData == null || nsData.length.toInt() == 0) {
                cont.resume(null)
                return
            }
            cont.resume(nsData.toByteArray())
        }

        override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
            cont.resume(null)
        }
    }

    picker.delegate = delegate

    val rootVC = UIApplication.sharedApplication
        .keyWindow
        ?.rootViewController
    rootVC?.presentViewController(picker, animated = true, completion = null)
}

/**
 * Converts [NSData] to a Kotlin [ByteArray].
 */
@OptIn(ExperimentalForeignApi::class)
internal fun NSData.toByteArray(): ByteArray {
    val len = length.toInt()
    if (len == 0) return ByteArray(0)
    val bytes = ByteArray(len)
    bytes.usePinned { pinned ->
        memcpy(pinned.addressOf(0), this.bytes, this.length)
    }
    return bytes
}
