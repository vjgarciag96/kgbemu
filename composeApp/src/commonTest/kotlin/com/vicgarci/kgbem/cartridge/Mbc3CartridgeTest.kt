package com.vicgarci.kgbem.cartridge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Mbc3CartridgeTest {

    private val logo = byteArrayOf(
        0xCE.toByte(), 0xED.toByte(), 0x66, 0x66, 0xCC.toByte(), 0x0D, 0x00, 0x0B,
        0x03, 0x73, 0x00, 0x83.toByte(), 0x00, 0x0C, 0x00, 0x0D,
        0x00, 0x08, 0x11, 0x1F, 0x88.toByte(), 0x89.toByte(), 0x00, 0x0E,
        0xDC.toByte(), 0xCC.toByte(), 0x6E, 0xE6.toByte(), 0xDD.toByte(), 0xDD.toByte(), 0xD9.toByte(), 0x99.toByte(),
        0xBB.toByte(), 0xBB.toByte(), 0x67, 0x63, 0x6E, 0x0E, 0xEC.toByte(), 0xCC.toByte(),
        0xDD.toByte(), 0xDC.toByte(), 0x99.toByte(), 0x9F.toByte(), 0xBB.toByte(), 0xB9.toByte(), 0x33, 0x3E
    )

    private fun createRom(
        romBanks: Int = 4,
        ramSizeByte: Int = 0x03,
        typeId: Int = Mbc3Cartridge.TYPE_MBC3_RAM_BATTERY
    ): ByteArray {
        val romSizeByte = when (romBanks) {
            2 -> 0x00; 4 -> 0x01; 8 -> 0x02; 16 -> 0x03; 32 -> 0x04
            64 -> 0x05; 128 -> 0x06; else -> 0x00
        }
        val size = romBanks * 0x4000
        val rom = ByteArray(size)

        for (bank in 0 until romBanks) {
            rom[bank * 0x4000] = (bank and 0xFF).toByte()
        }

        logo.copyInto(rom, destinationOffset = 0x0104)
        rom[0x0147] = typeId.toByte()
        rom[0x0148] = romSizeByte.toByte()
        rom[0x0149] = ramSizeByte.toByte()

        // Fix header checksum
        var checksum = 0
        for (i in 0x0134..0x014C) {
            checksum = checksum - rom[i] - 1
        }
        rom[0x014D] = (checksum and 0xFF).toByte()

        return rom
    }

    private fun createCart(
        romBanks: Int = 4,
        ramSizeByte: Int = 0x03,
        typeId: Int = Mbc3Cartridge.TYPE_MBC3_RAM_BATTERY
    ): Mbc3Cartridge {
        return Mbc3Cartridge(createRom(romBanks, ramSizeByte, typeId), typeId)
    }

    // --- ROM bank select ---

    @Test
    fun romBankSelectBank1() {
        val cart = createCart(romBanks = 4)
        cart.writeRom(0x2000, 0x01)
        assertEquals(0x01, cart.readRom(0x4000))
    }

    @Test
    fun romBankSelectBank2() {
        val cart = createCart(romBanks = 4)
        cart.writeRom(0x2000, 0x02)
        assertEquals(0x02, cart.readRom(0x4000))
    }

    @Test
    fun romBankSelectBank3() {
        val cart = createCart(romBanks = 4)
        cart.writeRom(0x2000, 0x03)
        assertEquals(0x03, cart.readRom(0x4000))
    }

    // --- ROM bank 0x00 clamped to 0x01 ---

    @Test
    fun romBank0x00ClampedTo0x01() {
        val cart = createCart(romBanks = 4)
        cart.writeRom(0x2000, 0x00)
        assertEquals(0x01, cart.readRom(0x4000))
    }

    // --- Bank 0 region always reads physical bank 0 ---

    @Test
    fun bank0AlwaysReadsPhysicalBank0() {
        val cart = createCart(romBanks = 4)
        cart.writeRom(0x2000, 0x03)
        assertEquals(0x00, cart.readRom(0x0000))
    }

    // --- RAM bank select ---

    @Test
    fun ramBankSelect() {
        val cart = createCart()
        // Enable RAM
        cart.writeRom(0x0000, 0x0A)
        // Write to RAM bank 0
        cart.writeRom(0x4000, 0x00)
        cart.writeRam(0xA000, 0x42)
        // Select bank 2
        cart.writeRom(0x4000, 0x02)
        cart.writeRam(0xA000, 0x99)
        // Read bank 2
        assertEquals(0x99, cart.readRam(0xA000))
        // Switch back to bank 0
        cart.writeRom(0x4000, 0x00)
        assertEquals(0x42, cart.readRam(0xA000))
    }

    // --- RAM disabled by default ---

    @Test
    fun ramDisabledByDefault() {
        val cart = createCart()
        assertEquals(0xFF, cart.readRam(0xA000))
    }

    @Test
    fun ramEnabledBy0x0A() {
        val cart = createCart()
        cart.writeRom(0x0000, 0x0A)
        cart.writeRam(0xA000, 0x42)
        assertEquals(0x42, cart.readRam(0xA000))
    }

    @Test
    fun ramDisabledByNon0x0A() {
        val cart = createCart()
        cart.writeRom(0x0000, 0x0A)
        cart.writeRam(0xA000, 0x42)
        cart.writeRom(0x0000, 0x00)
        assertEquals(0xFF, cart.readRam(0xA000))
    }

    // --- RTC register select and read/write ---

    @Test
    fun rtcRegisterSelectAndReadWrite() {
        val cart = createCart(typeId = Mbc3Cartridge.TYPE_MBC3_TIMER_RAM_BATTERY)
        cart.writeRom(0x0000, 0x0A) // enable

        // Select RTC_S (0x08)
        cart.writeRom(0x4000, 0x08)

        // First halt to stop the clock so we get deterministic reads
        cart.writeRom(0x4000, 0x0C)
        cart.writeRam(0xA000, 0x40) // set halt bit

        // Write seconds
        cart.writeRom(0x4000, 0x08)
        cart.writeRam(0xA000, 30)

        // Read back
        assertEquals(30, cart.readRam(0xA000))
    }

    // --- Latch ---

    @Test
    fun latchFreezeRtcValues() {
        val cart = createCart(typeId = Mbc3Cartridge.TYPE_MBC3_TIMER_RAM_BATTERY)
        cart.writeRom(0x0000, 0x0A)

        // Halt the clock
        cart.writeRom(0x4000, 0x0C)
        cart.writeRam(0xA000, 0x40)

        // Set seconds to 25
        cart.writeRom(0x4000, 0x08)
        cart.writeRam(0xA000, 25)

        // Latch: write 0x00 then 0x01 to 0x6000
        cart.writeRom(0x6000, 0x00)
        cart.writeRom(0x6000, 0x01)

        // Now write a different seconds value
        cart.writeRom(0x4000, 0x08)
        cart.writeRam(0xA000, 50)

        // Read should still return latched value of 25
        assertEquals(25, cart.readRam(0xA000))
    }

    // --- RTC halt ---

    @Test
    fun rtcHaltStopsAdvancement() {
        val cart = createCart(typeId = Mbc3Cartridge.TYPE_MBC3_TIMER_RAM_BATTERY)
        cart.writeRom(0x0000, 0x0A)

        // Halt the clock
        cart.writeRom(0x4000, 0x0C)
        cart.writeRam(0xA000, 0x40) // set halt bit (bit 6)

        // Set seconds to 10
        cart.writeRom(0x4000, 0x08)
        cart.writeRam(0xA000, 10)

        // Read seconds -- should remain 10 since halted
        val s1 = cart.readRam(0xA000)
        assertEquals(10, s1)

        // Read again, still 10
        val s2 = cart.readRam(0xA000)
        assertEquals(10, s2)
    }

    // --- hasBattery ---

    @Test
    fun hasBatteryTrueFor0x0F() {
        val cart = createCart(typeId = Mbc3Cartridge.TYPE_MBC3_TIMER_BATTERY, ramSizeByte = 0x00)
        assertTrue(cart.hasBattery())
    }

    @Test
    fun hasBatteryTrueFor0x10() {
        val cart = createCart(typeId = Mbc3Cartridge.TYPE_MBC3_TIMER_RAM_BATTERY)
        assertTrue(cart.hasBattery())
    }

    @Test
    fun hasBatteryTrueFor0x13() {
        val cart = createCart(typeId = Mbc3Cartridge.TYPE_MBC3_RAM_BATTERY)
        assertTrue(cart.hasBattery())
    }

    @Test
    fun hasBatteryFalseFor0x11() {
        val cart = createCart(typeId = Mbc3Cartridge.TYPE_MBC3_ONLY, ramSizeByte = 0x00)
        assertFalse(cart.hasBattery())
    }

    @Test
    fun hasBatteryFalseFor0x12() {
        val cart = createCart(typeId = Mbc3Cartridge.TYPE_MBC3_RAM)
        assertFalse(cart.hasBattery())
    }

    // --- CartridgeLoader integration ---

    @Test
    fun cartridgeLoaderReturnsMbc3ForType0x0F() {
        val rom = createRom(typeId = Mbc3Cartridge.TYPE_MBC3_TIMER_BATTERY, ramSizeByte = 0x00)
        val result = CartridgeLoader.load(rom)
        assertTrue(result is CartridgeLoadResult.Success)
        assertTrue(result.cartridge is Mbc3Cartridge)
    }

    @Test
    fun cartridgeLoaderReturnsMbc3ForType0x10() {
        val rom = createRom(typeId = Mbc3Cartridge.TYPE_MBC3_TIMER_RAM_BATTERY)
        val result = CartridgeLoader.load(rom)
        assertTrue(result is CartridgeLoadResult.Success)
        assertTrue(result.cartridge is Mbc3Cartridge)
    }

    @Test
    fun cartridgeLoaderReturnsMbc3ForType0x11() {
        val rom = createRom(typeId = Mbc3Cartridge.TYPE_MBC3_ONLY, ramSizeByte = 0x00)
        val result = CartridgeLoader.load(rom)
        assertTrue(result is CartridgeLoadResult.Success)
        assertTrue(result.cartridge is Mbc3Cartridge)
    }

    @Test
    fun cartridgeLoaderReturnsMbc3ForType0x12() {
        val rom = createRom(typeId = Mbc3Cartridge.TYPE_MBC3_RAM)
        val result = CartridgeLoader.load(rom)
        assertTrue(result is CartridgeLoadResult.Success)
        assertTrue(result.cartridge is Mbc3Cartridge)
    }

    @Test
    fun cartridgeLoaderReturnsMbc3ForType0x13() {
        val rom = createRom(typeId = Mbc3Cartridge.TYPE_MBC3_RAM_BATTERY)
        val result = CartridgeLoader.load(rom)
        assertTrue(result is CartridgeLoadResult.Success)
        assertTrue(result.cartridge is Mbc3Cartridge)
    }
}
