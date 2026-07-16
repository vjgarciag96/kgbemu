package com.vicgarci.kgbem.platform

import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import kotlin.test.assertContentEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class SaveStorageTest {

    @Before
    fun setUp() {
        SaveStorage.context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        // Clean up saves directory
        val ctx = SaveStorage.context ?: return
        File(ctx.filesDir, "saves").deleteRecursively()
        SaveStorage.context = null
    }

    @Test
    fun `save and load round-trips data`() {
        val data = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        SaveStorage.save("TESTROM", data)
        val loaded = SaveStorage.load("TESTROM")
        assertContentEquals(data, loaded)
    }

    @Test
    fun `load returns null for missing file`() {
        assertNull(SaveStorage.load("NONEXISTENT"))
    }

    @Test
    fun `save overwrites previous data atomically`() {
        val first = byteArrayOf(0xAA.toByte(), 0xBB.toByte())
        val second = byteArrayOf(0xCC.toByte(), 0xDD.toByte(), 0xEE.toByte())

        SaveStorage.save("OVERWRITE", first)
        SaveStorage.save("OVERWRITE", second)

        val loaded = SaveStorage.load("OVERWRITE")
        assertContentEquals(second, loaded)
    }

    @Test
    fun `romTitle with path separators is sanitised`() {
        val data = byteArrayOf(0x10, 0x20)
        SaveStorage.save("evil/path", data)
        val loaded = SaveStorage.load("evil/path")
        assertContentEquals(data, loaded)
    }

    @Test
    fun `load returns null when context is not set`() {
        SaveStorage.context = null
        assertNull(SaveStorage.load("TESTROM"))
    }
}
