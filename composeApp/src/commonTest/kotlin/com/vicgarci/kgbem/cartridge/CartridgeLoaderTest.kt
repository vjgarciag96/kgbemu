package com.vicgarci.kgbem.cartridge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CartridgeLoaderTest {

    private val nintendoLogo = byteArrayOf(
        0xCE.toByte(), 0xED.toByte(), 0x66, 0x66, 0xCC.toByte(), 0x0D, 0x00, 0x0B,
        0x03, 0x73, 0x00, 0x83.toByte(), 0x00, 0x0C, 0x00, 0x0D,
        0x00, 0x08, 0x11, 0x1F, 0x88.toByte(), 0x89.toByte(), 0x00, 0x0E,
        0xDC.toByte(), 0xCC.toByte(), 0x6E, 0xE6.toByte(), 0xDD.toByte(), 0xDD.toByte(), 0xD9.toByte(), 0x99.toByte(),
        0xBB.toByte(), 0xBB.toByte(), 0x67, 0x63, 0x6E, 0x0E, 0xEC.toByte(), 0xCC.toByte(),
        0xDD.toByte(), 0xDC.toByte(), 0x99.toByte(), 0x9F.toByte(), 0xBB.toByte(), 0xB9.toByte(), 0x33, 0x3E
    )

    private fun validRomOnlyBytes(): ByteArray {
        val rom = ByteArray(0x8000)

        // Write Nintendo logo at 0x0104–0x0133
        nintendoLogo.copyInto(rom, destinationOffset = 0x0104)

        // Cartridge type = 0x00 (ROM only)
        rom[0x0147] = 0x00

        // Compute header checksum: sum = 0; for i in 0x0134..0x014C: sum = sum - bytes[i] - 1
        var checksum = 0
        for (i in 0x0134..0x014C) {
            checksum = checksum - rom[i] - 1
        }
        rom[0x014D] = (checksum and 0xFF).toByte()

        return rom
    }

    @Test
    fun loadValidRomOnlyFileReturnsSuccess() {
        val result = CartridgeLoader.load(validRomOnlyBytes())
        assertIs<CartridgeLoadResult.Success>(result)
        assertIs<RomOnlyCartridge>(result.cartridge)
    }

    @Test
    fun loadFileShorterThan0x150ReturnsTruncated() {
        val tooSmall = ByteArray(0x014F)
        val result = CartridgeLoader.load(tooSmall)
        assertIs<CartridgeLoadResult.Failure>(result)
        assertEquals(RomError.Truncated, result.error)
    }

    @Test
    fun loadBadNintendoLogoReturnsInvalidHeader() {
        val rom = validRomOnlyBytes()
        rom[0x0104] = 0x00 // corrupt first logo byte
        // Recompute checksum so only the logo check fails
        var checksum = 0
        for (i in 0x0134..0x014C) {
            checksum = checksum - rom[i] - 1
        }
        rom[0x014D] = (checksum and 0xFF).toByte()

        val result = CartridgeLoader.load(rom)
        assertIs<CartridgeLoadResult.Failure>(result)
        assertEquals(RomError.InvalidHeader, result.error)
    }

    @Test
    fun loadBadChecksumReturnsInvalidHeader() {
        val rom = validRomOnlyBytes()
        rom[0x014D] = (rom[0x014D] + 1).toByte() // corrupt checksum

        val result = CartridgeLoader.load(rom)
        assertIs<CartridgeLoadResult.Failure>(result)
        assertEquals(RomError.InvalidHeader, result.error)
    }

    @Test
    fun loadUnsupportedMapperType0x05ReturnsUnsupportedMapper() {
        val rom = validRomOnlyBytes()
        rom[0x0147] = 0x05 // MBC2, unsupported
        // Recompute checksum
        var checksum = 0
        for (i in 0x0134..0x014C) {
            checksum = checksum - rom[i] - 1
        }
        rom[0x014D] = (checksum and 0xFF).toByte()

        val result = CartridgeLoader.load(rom)
        assertIs<CartridgeLoadResult.Failure>(result)
        assertEquals(RomError.UnsupportedMapper(0x05), result.error)
    }

    @Test
    fun loadUnsupportedMapperType0x01ReturnsUnsupportedMapper() {
        val rom = validRomOnlyBytes()
        rom[0x0147] = 0x01 // MBC1, not yet implemented
        // Recompute checksum
        var checksum = 0
        for (i in 0x0134..0x014C) {
            checksum = checksum - rom[i] - 1
        }
        rom[0x014D] = (checksum and 0xFF).toByte()

        val result = CartridgeLoader.load(rom)
        assertIs<CartridgeLoadResult.Failure>(result)
        assertEquals(RomError.UnsupportedMapper(0x01), result.error)
    }

    @Test
    fun loadValidRomOnlyCartridgeReadsCorrectByte() {
        val rom = validRomOnlyBytes()
        rom[0x0000] = 0x42

        val result = CartridgeLoader.load(rom)
        assertIs<CartridgeLoadResult.Success>(result)
        assertEquals(0x42, result.cartridge.readRom(0x0000))
    }
}
