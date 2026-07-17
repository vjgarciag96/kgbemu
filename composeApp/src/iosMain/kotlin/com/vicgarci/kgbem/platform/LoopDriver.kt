@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.vicgarci.kgbem.platform

import com.vicgarci.kgbem.emulator.EmulatorLoop
import platform.Foundation.NSDefaultRunLoopMode
import platform.Foundation.NSRunLoop
import platform.Foundation.NSSelectorFromString
import platform.QuartzCore.CADisplayLink
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.NSObject

actual class LoopDriver actual constructor(private val loop: EmulatorLoop) {

    private var displayLink: CADisplayLink? = null
    private var target: DisplayLinkTarget? = null

    actual fun start() {
        stop()
        val linkTarget = DisplayLinkTarget(loop)
        val link = CADisplayLink.displayLinkWithTarget(
            target = linkTarget,
            selector = NSSelectorFromString("step")
        )
        link.addToRunLoop(NSRunLoop.mainRunLoop, forMode = NSDefaultRunLoopMode)
        target = linkTarget
        displayLink = link
    }

    actual fun stop() {
        displayLink?.invalidate()
        displayLink = null
        target = null
    }
}

private class DisplayLinkTarget(private val loop: EmulatorLoop) : NSObject() {

    @kotlinx.cinterop.ObjCAction
    fun step() {
        dispatch_async(
            dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)
        ) {
            loop.runFrame()
        }
    }
}
