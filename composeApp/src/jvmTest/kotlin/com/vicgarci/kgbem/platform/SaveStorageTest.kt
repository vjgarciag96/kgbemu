package com.vicgarci.kgbem.platform

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SaveStorageTest {

    private lateinit var tempDir: File
    private lateinit var originalBaseDir: String

    @BeforeTest
    fun setUp() {
        originalBaseDir = SaveStorage.baseDir
        tempDir = Files.createTempDirectory("kgbemu-test-saves").toFile()
        SaveStorage.baseDir = tempDir.absolutePath
    }

    @AfterTest
    fun tearDown() {
        SaveStorage.baseDir = originalBaseDir
        tempDir.deleteRecursively()
    }

    @Test
    fun `save and load round-trip returns identical data`() {
        val data = byteArrayOf(0x00, 0x11, 0x22, 0x33, 0xFF.toByte())
        SaveStorage.save("TestRom", data)

        val loaded = SaveStorage.load("TestRom")
        assertContentEquals(data, loaded)
    }

    @Test
    fun `save creates sav file and no tmp file remains`() {
        val data = byteArrayOf(0x42)
        SaveStorage.save("CleanUp", data)

        val savFile = File(tempDir, "CleanUp.sav")
        val tmpFile = File(tempDir, "CleanUp.sav.tmp")

        assertTrue(savFile.exists(), "Expected .sav file to exist")
        assertTrue(!tmpFile.exists(), "Expected .sav.tmp file to not exist after save")
    }

    @Test
    fun `load returns null when file does not exist`() {
        val result = SaveStorage.load("NonExistent")
        assertNull(result)
    }

    @Test
    fun `romTitle slashes are stripped`() {
        val data = byteArrayOf(0x01, 0x02)
        SaveStorage.save("some/evil/title", data)

        val savFile = File(tempDir, "someeviltitle.sav")
        assertTrue(savFile.exists(), "Expected sanitised filename without slashes")

        val loaded = SaveStorage.load("some/evil/title")
        assertContentEquals(data, loaded)
    }

    @Test
    fun `save overwrites previous save atomically`() {
        val first = byteArrayOf(0x0A)
        val second = byteArrayOf(0x0B, 0x0C)
        SaveStorage.save("Overwrite", first)
        SaveStorage.save("Overwrite", second)

        val loaded = SaveStorage.load("Overwrite")
        assertContentEquals(second, loaded)
    }
}
