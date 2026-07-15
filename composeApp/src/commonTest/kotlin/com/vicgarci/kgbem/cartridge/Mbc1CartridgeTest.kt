package com.vicgarci.kgbem.cartridge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class Mbc1CartridgeTest {

    /**
     * Creates a valid MBC1 ROM with the given number of banks.
     * Each bank's first byte is set to a unique marker: bankIndex and 0xAA.
     */
    private fun createRom(
        romBanks: Int = 2,
        ramSizeByte: Int = 0x00,
        typeId: Int = Mbc1Cartridge.TYPE_MBC1
    ): ByteArray {
        val romSizeByte = when (romBanks) {
            2 -> 0x00
            4 -> 0x01
            8 -> 0x02
            16 -> 0x03
            32 -> 0x04
            64 -> 0x05
            128 -> 0x06
            else -> 0x00
        }
        val size = romBanks * 0x4000
        val rom = ByteArray(size)

        // Mark each bank's first byte with a unique value
        for (bank in 0 until romBanks) {
            rom[bank * 0x4000] = (bank and 0xFF).toByte()
        }

        // Write Nintendo logo at 0x0104
        val logo = byteArrayOf(
            0xCE.toByte(), 0xED.toByte(), 0x66, 0x66, 0xCC.toByte(), 0x0D, 0x00, 0x0B,
            0x03, 0x73, 0x00, 0x83.toByte(), 0x00, 0x0C, 0x00, 0x0D,
            0x00, 0x08, 0x11, 0x1F, 0x88.toByte(), 0x89.toByte(), 0x00, 0x0E,
            0xDC.toByte(), 0xCC.toByte(), 0x6E, 0xE6.toByte(), 0xDD.toByte(), 0xDD.toByte(), 0xD9.toByte(), 0x99.toByte(),
            0xBB.toByte(), 0xBB.toByte(), 0x67, 0x63, 0x6E, 0x0E, 0xEC.toByte(), 0xCC.toByte(),
            0xDD.toByte(), 0xDC.toByte(), 0x99.toByte(), 0x9F.toByte(), 0xBB.toByte(), 0xB9.toByte(), 0x33, 0x3E
        )
        logo.copyInto(rom, destinationOffset = 0x0104)

        rom[0x0147] = typeId.toByte()
        rom[0x0148] = romSizeByte.toByte()
        rom[0x0149] = ramSizeByte.toByte()

        return rom
    }

    private fun createMbc1(
        romBanks: Int = 2,
        ramSizeByte: Int = 0x00,
        typeId: Int = Mbc1Cartridge.TYPE_MBC1
    ): Mbc1Cartridge {
        return Mbc1Cartridge(createRom(romBanks, ramSizeByte, typeId), typeId)
    }

    // --- Bank 0 always reads physical bank 0 ---

    @Test
    fun bank0AlwaysReadsPhysicalBank0() {
        val cart = createMbc1(romBanks = 4)
        // Bank 0's first byte should be 0 (the marker we set)
        assertEquals(0x00, cart.readRom(0x0000))
    }

    // --- Bank switching via write to 0x2000-0x3FFF ---

    @Test
    fun writeToRomBankSelectsBank1() {
        val cart = createMbc1(romBanks = 4)
        cart.writeRom(0x2000, 0x01)
        // Read from banked area should get bank 1's marker
        assertEquals(0x01, cart.readRom(0x4000))
    }

    @Test
    fun writeToRomBankSelectsBank2() {
        val cart = createMbc1(romBanks = 4)
        cart.writeRom(0x2000, 0x02)
        assertEquals(0x02, cart.readRom(0x4000))
    }

    @Test
    fun writeToRomBankSelectsBank3() {
        val cart = createMbc1(romBanks = 4)
        cart.writeRom(0x2000, 0x03)
        assertEquals(0x03, cart.readRom(0x4000))
    }

    // --- Bank 0x00 remapped to 0x01, 0x20->0x21, 0x40->0x41, 0x60->0x61 ---

    @Test
    fun bank0x00RemappedTo0x01() {
        val cart = createMbc1(romBanks = 4)
        cart.writeRom(0x2000, 0x00) // write 0 => should remap to 1
        assertEquals(0x01, cart.readRom(0x4000))
    }

    @Test
    fun bank0x20RemappedTo0x21() {
        // Need at least 64 banks for bank 0x21
        val cart = createMbc1(romBanks = 64)
        cart.writeRom(0x4000, 0x01) // upper 2 bits = 1 => bank high = 0x20
        cart.writeRom(0x2000, 0x00) // lower 5 bits = 0 => remapped to 1, so bank = 0x21
        assertEquals(0x21, cart.readRom(0x4000))
    }

    @Test
    fun bank0x40RemappedTo0x41() {
        val cart = createMbc1(romBanks = 128)
        cart.writeRom(0x4000, 0x02) // upper 2 bits = 2 => bank high = 0x40
        cart.writeRom(0x2000, 0x00) // lower 5 bits = 0 => remapped to 1, so bank = 0x41
        assertEquals(0x41, cart.readRom(0x4000))
    }

    @Test
    fun bank0x60RemappedTo0x61() {
        val cart = createMbc1(romBanks = 128)
        cart.writeRom(0x4000, 0x03) // upper 2 bits = 3 => bank high = 0x60
        cart.writeRom(0x2000, 0x00) // lower 5 bits = 0 => remapped to 1, so bank = 0x61
        assertEquals(0x61, cart.readRom(0x4000))
    }

    // --- Parametrised remap test ---

    @Test
    fun allBankRemapsCorrect() {
        val cart = createMbc1(romBanks = 128)
        val remaps = listOf(
            Pair(0x00, 0x01),
            Pair(0x20, 0x21),
            Pair(0x40, 0x41),
            Pair(0x60, 0x61)
        )
        for ((input, expected) in remaps) {
            val upperBits = (input shr 5) and 0x03
            val lowerBits = input and 0x1F
            cart.writeRom(0x4000, upperBits)
            cart.writeRom(0x2000, lowerBits)
            assertEquals(
                expected,
                cart.readRom(0x4000),
                "Bank ${"0x%02X".format(input)} should remap to ${"0x%02X".format(expected)}"
            )
        }
    }

    // --- Mode 0: upper 2 bits extend ROM bank ---

    @Test
    fun mode0UpperBitsExtendRomBank() {
        val cart = createMbc1(romBanks = 64)
        // Default is mode 0
        cart.writeRom(0x2000, 0x01) // lower 5 bits = 1
        cart.writeRom(0x4000, 0x01) // upper 2 bits = 1 => full bank = 0x21
        assertEquals(0x21, cart.readRom(0x4000))
    }

    @Test
    fun mode0RamFixedAtBank0() {
        val cart = createMbc1(romBanks = 4, ramSizeByte = 0x03, typeId = Mbc1Cartridge.TYPE_MBC1_RAM)
        // Enable RAM
        cart.writeRom(0x0000, 0x0A)
        // Write to RAM bank 0
        cart.writeRam(0xA000, 0x42)
        // Set upper bits to select RAM bank 1 (mode 0 should ignore this for RAM)
        cart.writeRom(0x4000, 0x01)
        // In mode 0, RAM is always bank 0
        assertEquals(0x42, cart.readRam(0xA000))
    }

    // --- Mode 1: upper 2 bits select RAM bank ---

    @Test
    fun mode1UpperBitsSelectRamBank() {
        val cart = createMbc1(romBanks = 4, ramSizeByte = 0x03, typeId = Mbc1Cartridge.TYPE_MBC1_RAM)
        // Enable RAM
        cart.writeRom(0x0000, 0x0A)
        // Write to RAM bank 0
        cart.writeRam(0xA000, 0x42)
        // Switch to mode 1
        cart.writeRom(0x6000, 0x01)
        // Select RAM bank 1
        cart.writeRom(0x4000, 0x01)
        // Write to RAM bank 1
        cart.writeRam(0xA000, 0x99.toByte().toInt() and 0xFF)
        // Read bank 1 => should get 0x99
        assertEquals(0x99, cart.readRam(0xA000))
        // Switch back to bank 0
        cart.writeRom(0x4000, 0x00)
        // Bank 0 should still have 0x42
        assertEquals(0x42, cart.readRam(0xA000))
    }

    // --- RAM disabled by default ---

    @Test
    fun ramDisabledByDefault() {
        val cart = createMbc1(romBanks = 2, ramSizeByte = 0x02, typeId = Mbc1Cartridge.TYPE_MBC1_RAM)
        assertEquals(0xFF, cart.readRam(0xA000))
    }

    @Test
    fun ramEnabledBy0x0AWriteTo0x0000() {
        val cart = createMbc1(romBanks = 2, ramSizeByte = 0x02, typeId = Mbc1Cartridge.TYPE_MBC1_RAM)
        cart.writeRom(0x0000, 0x0A)
        cart.writeRam(0xA000, 0x42)
        assertEquals(0x42, cart.readRam(0xA000))
    }

    @Test
    fun ramDisabledByNon0x0AWrite() {
        val cart = createMbc1(romBanks = 2, ramSizeByte = 0x02, typeId = Mbc1Cartridge.TYPE_MBC1_RAM)
        cart.writeRom(0x0000, 0x0A) // enable
        cart.writeRam(0xA000, 0x42)
        cart.writeRom(0x0000, 0x00) // disable
        assertEquals(0xFF, cart.readRam(0xA000))
    }

    // --- RAM returns 0xFF when disabled ---

    @Test
    fun ramReturns0xFFWhenDisabled() {
        val cart = createMbc1(romBanks = 2, ramSizeByte = 0x02, typeId = Mbc1Cartridge.TYPE_MBC1_RAM)
        assertEquals(0xFF, cart.readRam(0xA000))
        assertEquals(0xFF, cart.readRam(0xBFFF))
    }

    // --- RAM returns 0xFF when no RAM present ---

    @Test
    fun ramReturns0xFFWhenNoRamPresent() {
        val cart = createMbc1(romBanks = 2, ramSizeByte = 0x00, typeId = Mbc1Cartridge.TYPE_MBC1)
        cart.writeRom(0x0000, 0x0A) // try to enable
        assertEquals(0xFF, cart.readRam(0xA000))
    }

    // --- hasBattery ---

    @Test
    fun hasBatteryTrueForType0x03() {
        val cart = createMbc1(typeId = Mbc1Cartridge.TYPE_MBC1_RAM_BATTERY)
        assertTrue(cart.hasBattery())
    }

    @Test
    fun hasBatteryFalseForType0x01() {
        val cart = createMbc1(typeId = Mbc1Cartridge.TYPE_MBC1)
        assertFalse(cart.hasBattery())
    }

    @Test
    fun hasBatteryFalseForType0x02() {
        val cart = createMbc1(typeId = Mbc1Cartridge.TYPE_MBC1_RAM)
        assertFalse(cart.hasBattery())
    }

    // --- savableState ---

    @Test
    fun savableStateReturnsNullForNoBattery() {
        val cart = createMbc1(ramSizeByte = 0x02, typeId = Mbc1Cartridge.TYPE_MBC1_RAM)
        assertNull(cart.savableState())
    }

    @Test
    fun savableStateReturnsCopyForBatteryWithRam() {
        val cart = createMbc1(ramSizeByte = 0x02, typeId = Mbc1Cartridge.TYPE_MBC1_RAM_BATTERY)
        cart.writeRom(0x0000, 0x0A)
        cart.writeRam(0xA000, 0x42)
        val state = cart.savableState()
        assertTrue(state != null)
        assertEquals(0x42, state[0].toInt() and 0xFF)
    }

    // --- loadState ---

    @Test
    fun loadStateRestoresRam() {
        val cart = createMbc1(ramSizeByte = 0x02, typeId = Mbc1Cartridge.TYPE_MBC1_RAM_BATTERY)
        val state = ByteArray(0x2000)
        state[0] = 0x42
        cart.loadState(state)
        cart.writeRom(0x0000, 0x0A) // enable RAM
        assertEquals(0x42, cart.readRam(0xA000))
    }

    // --- CartridgeLoader integration ---

    @Test
    fun cartridgeLoaderReturnsMbc1ForType0x01() {
        val rom = createRom(romBanks = 2, typeId = Mbc1Cartridge.TYPE_MBC1)
        // Need valid header checksum
        var checksum = 0
        for (i in 0x0134..0x014C) {
            checksum = checksum - rom[i] - 1
        }
        rom[0x014D] = (checksum and 0xFF).toByte()
        val result = CartridgeLoader.load(rom)
        assertTrue(result is CartridgeLoadResult.Success)
        assertTrue(result.cartridge is Mbc1Cartridge)
    }

    @Test
    fun cartridgeLoaderReturnsMbc1ForType0x02() {
        val rom = createRom(romBanks = 2, ramSizeByte = 0x02, typeId = Mbc1Cartridge.TYPE_MBC1_RAM)
        var checksum = 0
        for (i in 0x0134..0x014C) {
            checksum = checksum - rom[i] - 1
        }
        rom[0x014D] = (checksum and 0xFF).toByte()
        val result = CartridgeLoader.load(rom)
        assertTrue(result is CartridgeLoadResult.Success)
        assertTrue(result.cartridge is Mbc1Cartridge)
    }

    @Test
    fun cartridgeLoaderReturnsMbc1ForType0x03() {
        val rom = createRom(romBanks = 2, ramSizeByte = 0x02, typeId = Mbc1Cartridge.TYPE_MBC1_RAM_BATTERY)
        var checksum = 0
        for (i in 0x0134..0x014C) {
            checksum = checksum - rom[i] - 1
        }
        rom[0x014D] = (checksum and 0xFF).toByte()
        val result = CartridgeLoader.load(rom)
        assertTrue(result is CartridgeLoadResult.Success)
        assertTrue(result.cartridge is Mbc1Cartridge)
    }

    // --- Mode 1: ROM bank 0 region uses upper bits ---

    @Test
    fun mode1Bank0RegionUsesUpperBits() {
        val cart = createMbc1(romBanks = 64)
        cart.writeRom(0x6000, 0x01) // switch to mode 1
        cart.writeRom(0x4000, 0x01) // upper 2 bits = 1 => bank 0 region reads from bank 0x20
        assertEquals(0x20, cart.readRom(0x0000)) // bank 0x20's marker
    }
}
