package com.vicgarci.kgbem.platform

import com.vicgarci.kgbem.cartridge.CartridgeLoadResult
import com.vicgarci.kgbem.cartridge.CartridgeLoader
import com.vicgarci.kgbem.emulator.EmulatorLoop
import com.vicgarci.kgbem.joypad.InputSource
import com.vicgarci.kgbem.joypad.JoypadState
import com.vicgarci.kgbem.ppu.FrameSink
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertTrue

class LoopDriverTest {

    private val noOpInputSource = object : InputSource {
        override val state = MutableStateFlow(JoypadState())
    }

    private val nintendoLogo = byteArrayOf(
        0xCE.toByte(), 0xED.toByte(), 0x66, 0x66, 0xCC.toByte(), 0x0D, 0x00, 0x0B,
        0x03, 0x73, 0x00, 0x83.toByte(), 0x00, 0x0C, 0x00, 0x0D,
        0x00, 0x08, 0x11, 0x1F, 0x88.toByte(), 0x89.toByte(), 0x00, 0x0E,
        0xDC.toByte(), 0xCC.toByte(), 0x6E, 0xE6.toByte(), 0xDD.toByte(), 0xDD.toByte(), 0xD9.toByte(), 0x99.toByte(),
        0xBB.toByte(), 0xBB.toByte(), 0x67, 0x63, 0x6E, 0x0E, 0xEC.toByte(), 0xCC.toByte(),
        0xDD.toByte(), 0xDC.toByte(), 0x99.toByte(), 0x9F.toByte(), 0xBB.toByte(), 0xB9.toByte(), 0x33, 0x3E,
    )

    /** Creates a minimal valid ROM-only cartridge with JR -2 at 0x0100 (infinite NOP-safe loop). */
    private fun createMinimalRom(): ByteArray {
        val rom = ByteArray(0x8000)
        nintendoLogo.copyInto(rom, destinationOffset = 0x0104)
        // Place "JR -2" (0x18 0xFE) at 0x0100 so the CPU loops in place
        // instead of running into unimplemented opcodes in the logo area.
        rom[0x0100] = 0x18.toByte() // JR
        rom[0x0101] = 0xFE.toByte() // offset -2 (back to 0x0100)
        // Type 0x00 = ROM only
        var checksum = 0
        for (i in 0x0134..0x014C) checksum = checksum - rom[i] - 1
        rom[0x014D] = (checksum and 0xFF).toByte()
        return rom
    }

    private fun createLoop(frameCounter: AtomicInteger): EmulatorLoop {
        val romBytes = createMinimalRom()
        val result = CartridgeLoader.load(romBytes) as CartridgeLoadResult.Success
        val frameSink = object : FrameSink {
            override fun onFrame(pixels: IntArray) {
                frameCounter.incrementAndGet()
            }
        }
        return EmulatorLoop(
            cartridge = result.cartridge,
            frameSink = frameSink,
            inputSource = noOpInputSource,
        )
    }

    @Test
    fun `start runs frames`() {
        val frameCount = AtomicInteger(0)
        val loop = createLoop(frameCount)

        // Verify that runFrame works at all before testing the driver.
        loop.runFrame()
        assertTrue(
            frameCount.get() == 1,
            "Direct runFrame call should produce 1 frame, got ${frameCount.get()}",
        )

        // Now test the driver.
        frameCount.set(0)
        val driver = LoopDriver(loop)
        driver.start()
        Thread.sleep(2000)
        driver.stop()

        assertTrue(
            frameCount.get() >= 1,
            "Expected at least 1 frame after start, got ${frameCount.get()}",
        )
    }

    @Test
    fun `stop cancels the loop`() {
        val frameCount = AtomicInteger(0)
        val loop = createLoop(frameCount)
        val driver = LoopDriver(loop)

        driver.start()
        Thread.sleep(2000)
        driver.stop()

        val countAtStop = frameCount.get()
        assertTrue(countAtStop >= 1, "Expected at least 1 frame before stop, got $countAtStop")

        Thread.sleep(500)

        // Allow one in-flight frame that may complete after cancellation is signalled.
        assertTrue(
            frameCount.get() <= countAtStop + 1,
            "Expected no new frames after stop, but count went from $countAtStop to ${frameCount.get()}",
        )
    }
}
