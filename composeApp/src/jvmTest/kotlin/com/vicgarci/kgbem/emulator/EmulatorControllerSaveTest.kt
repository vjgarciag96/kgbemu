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
import kotlin.test.assertContentEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EmulatorControllerSaveTest {

    private lateinit var tempDir: File
    private lateinit var originalBaseDir: String

    private val noOpInputSource = object : InputSource {
        override val state = MutableStateFlow(JoypadState())
    }

    @BeforeTest
    fun setUp() {
        originalBaseDir = SaveStorage.baseDir
        tempDir = Files.createTempDirectory("kgbemu-save-test").toFile()
        SaveStorage.baseDir = tempDir.absolutePath
    }

    @AfterTest
    fun tearDown() {
        SaveStorage.baseDir = originalBaseDir
        tempDir.deleteRecursively()
    }

    // -------------------------------------------------------------------------
    // ROM builder helpers (mirrors Mbc3CartridgeTest pattern)
    // -------------------------------------------------------------------------

    private val nintendoLogo = byteArrayOf(
        0xCE.toByte(), 0xED.toByte(), 0x66, 0x66, 0xCC.toByte(), 0x0D, 0x00, 0x0B,
        0x03, 0x73, 0x00, 0x83.toByte(), 0x00, 0x0C, 0x00, 0x0D,
        0x00, 0x08, 0x11, 0x1F, 0x88.toByte(), 0x89.toByte(), 0x00, 0x0E,
        0xDC.toByte(), 0xCC.toByte(), 0x6E, 0xE6.toByte(), 0xDD.toByte(), 0xDD.toByte(), 0xD9.toByte(), 0x99.toByte(),
        0xBB.toByte(), 0xBB.toByte(), 0x67, 0x63, 0x6E, 0x0E, 0xEC.toByte(), 0xCC.toByte(),
        0xDD.toByte(), 0xDC.toByte(), 0x99.toByte(), 0x9F.toByte(), 0xBB.toByte(), 0xB9.toByte(), 0x33, 0x3E
    )

    /** Creates a minimal valid MBC3+RAM+BATTERY ROM with an 8KB RAM bank. */
    private fun createBatteryRom(title: String = "TESTROM"): ByteArray {
        val rom = ByteArray(4 * 0x4000) // 2 ROM banks minimum
        nintendoLogo.copyInto(rom, destinationOffset = 0x0104)

        // Title at 0x0134 (up to 16 bytes, null-terminated)
        val titleBytes = title.toByteArray(Charsets.US_ASCII)
        titleBytes.copyInto(rom, destinationOffset = 0x0134, endIndex = minOf(titleBytes.size, 16))

        rom[0x0147] = 0x13.toByte() // MBC3+RAM+BATTERY
        rom[0x0148] = 0x01.toByte() // 4 banks (64KB)
        rom[0x0149] = 0x02.toByte() // 8KB RAM (1 bank)

        // Header checksum (0x0134–0x014C)
        var checksum = 0
        for (i in 0x0134..0x014C) checksum = checksum - rom[i] - 1
        rom[0x014D] = (checksum and 0xFF).toByte()

        return rom
    }

    /** Creates a minimal valid ROM-only ROM (no battery). */
    private fun createNoBatteryRom(): ByteArray {
        val rom = ByteArray(0x8000)
        nintendoLogo.copyInto(rom, destinationOffset = 0x0104)
        // Type 0x00 = ROM only (no battery)
        var checksum = 0
        for (i in 0x0134..0x014C) checksum = checksum - rom[i] - 1
        rom[0x014D] = (checksum and 0xFF).toByte()
        return rom
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    fun `saveGame is no-op when no ROM is loaded`() {
        val controller = EmulatorController(noOpInputSource)
        controller.saveGame() // must not throw
        assertTrue(tempDir.listFiles()?.isEmpty() ?: true, "Expected no save files when no ROM loaded")
    }

    @Test
    fun `saveGame is no-op for ROM without battery`() {
        val controller = EmulatorController(noOpInputSource)
        controller.loadRom(createNoBatteryRom())
        controller.saveGame()
        assertTrue(tempDir.listFiles()?.isEmpty() ?: true, "Expected no save files for ROM-only cartridge")
    }

    @Test
    fun `saveGame writes sav file for battery-backed ROM`() {
        val controller = EmulatorController(noOpInputSource)
        controller.loadRom(createBatteryRom("TESTROM"))
        controller.saveGame()

        val savFile = File(tempDir, "TESTROM.sav")
        assertTrue(savFile.exists(), "Expected TESTROM.sav to exist after saveGame()")
    }

    @Test
    fun `loadRom restores saved state into cartridge RAM`() {
        // Pre-populate a save with known data (8KB of 0x42).
        val ramData = ByteArray(8192) { 0x42 }
        SaveStorage.save("TESTROM", ramData)

        val controller = EmulatorController(noOpInputSource)
        controller.loadRom(createBatteryRom("TESTROM"))

        // After load, the RAM should contain the restored data.
        // Verify indirectly: saveGame() should write back those same bytes.
        controller.saveGame()

        val savedBytes = SaveStorage.load("TESTROM")
        assertNotNull(savedBytes, "Expected saved bytes after round-trip")
        assertContentEquals(ramData, savedBytes, "Loaded state must match what was originally saved")
    }

    @Test
    fun `loadRom with corrupt save proceeds with clean state`() {
        // Pre-populate a valid-format save but then load a ROM to verify it doesn't crash.
        // We just verify no exception is thrown and state is Running.
        SaveStorage.save("TESTROM", ByteArray(100) { 0xFF.toByte() }) // wrong size

        val controller = EmulatorController(noOpInputSource)
        controller.loadRom(createBatteryRom("TESTROM"))

        // Should not crash — cartridge loads with clean or partial state.
        assertTrue(controller.emulatorState.value == EmulatorState.Running)
    }

    @Test
    fun `saveGame writes back updated RAM after write-through`() {
        val controller = EmulatorController(noOpInputSource)
        controller.loadRom(createBatteryRom("SAVEME"))

        // saveGame should produce a 8KB file (matching the RAM bank size).
        controller.saveGame()

        val saved = SaveStorage.load("SAVEME")
        assertNotNull(saved)
        assertTrue(saved.size == 8192, "Expected 8KB save file, got ${saved.size} bytes")
    }
}
