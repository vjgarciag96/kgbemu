package com.vicgarci.kgbem.emulator

import com.vicgarci.kgbem.joypad.InputSource
import com.vicgarci.kgbem.joypad.JoypadState
import com.vicgarci.kgbem.platform.SaveStorage
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EmulatorControllerMenuActionsTest {

    private lateinit var tempDir: File
    private lateinit var originalBaseDir: String

    private val noOpInputSource = object : InputSource {
        override val state = MutableStateFlow(JoypadState())
    }

    // -------------------------------------------------------------------------
    // ROM builder helpers (same as EmulatorControllerSaveTest)
    // -------------------------------------------------------------------------

    private val nintendoLogo = byteArrayOf(
        0xCE.toByte(), 0xED.toByte(), 0x66, 0x66, 0xCC.toByte(), 0x0D, 0x00, 0x0B,
        0x03, 0x73, 0x00, 0x83.toByte(), 0x00, 0x0C, 0x00, 0x0D,
        0x00, 0x08, 0x11, 0x1F, 0x88.toByte(), 0x89.toByte(), 0x00, 0x0E,
        0xDC.toByte(), 0xCC.toByte(), 0x6E, 0xE6.toByte(), 0xDD.toByte(), 0xDD.toByte(), 0xD9.toByte(), 0x99.toByte(),
        0xBB.toByte(), 0xBB.toByte(), 0x67, 0x63, 0x6E, 0x0E, 0xEC.toByte(), 0xCC.toByte(),
        0xDD.toByte(), 0xDC.toByte(), 0x99.toByte(), 0x9F.toByte(), 0xBB.toByte(), 0xB9.toByte(), 0x33, 0x3E
    )

    private fun createNoBatteryRom(): ByteArray {
        val rom = ByteArray(0x8000)
        nintendoLogo.copyInto(rom, destinationOffset = 0x0104)
        var checksum = 0
        for (i in 0x0134..0x014C) checksum = checksum - rom[i] - 1
        rom[0x014D] = (checksum and 0xFF).toByte()
        return rom
    }

    @BeforeTest
    fun setUp() {
        originalBaseDir = SaveStorage.baseDir
        tempDir = Files.createTempDirectory("kgbemu-menu-test").toFile()
        SaveStorage.baseDir = tempDir.absolutePath
    }

    @AfterTest
    fun tearDown() {
        SaveStorage.baseDir = originalBaseDir
        tempDir.deleteRecursively()
    }

    // -------------------------------------------------------------------------
    // reset() tests
    // -------------------------------------------------------------------------

    @Test
    fun `reset re-creates loop and state is Running`() {
        val controller = EmulatorController(noOpInputSource)
        controller.loadRom(createNoBatteryRom())
        assertEquals(EmulatorState.Running, controller.emulatorState.value)

        val firstLoop = controller.loop

        controller.reset()

        assertEquals(EmulatorState.Running, controller.emulatorState.value)
        assertNotNull(controller.loop, "Loop should exist after reset")
        assertTrue(controller.loop !== firstLoop, "Reset should create a new loop instance")
    }

    @Test
    fun `reset is no-op when no ROM was loaded`() {
        val controller = EmulatorController(noOpInputSource)
        assertEquals(EmulatorState.Idle, controller.emulatorState.value)

        controller.reset()

        assertEquals(EmulatorState.Idle, controller.emulatorState.value)
        assertNull(controller.loop, "Loop should remain null when no ROM was loaded")
    }

    // -------------------------------------------------------------------------
    // unload() tests
    // -------------------------------------------------------------------------

    @Test
    fun `unload sets state to Idle and clears frame state`() {
        val controller = EmulatorController(noOpInputSource)
        controller.loadRom(createNoBatteryRom())
        assertEquals(EmulatorState.Running, controller.emulatorState.value)

        controller.unload()

        assertEquals(EmulatorState.Idle, controller.emulatorState.value)
        assertNull(controller.frameState.value, "Frame state should be null after unload")
        assertNull(controller.loop, "Loop should be null after unload")
    }

    @Test
    fun `unload is safe when no ROM is loaded`() {
        val controller = EmulatorController(noOpInputSource)
        controller.unload() // must not throw
        assertEquals(EmulatorState.Idle, controller.emulatorState.value)
    }
}
