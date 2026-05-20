package com.vicgarci.kgbem

import kotlin.test.Test
import kotlin.test.assertEquals

class CartridgeTest {

    private fun makeRom(size: Int = 0x8000, init: (UByteArray) -> Unit = {}): Cartridge {
        val rom = UByteArray(size)
        init(rom)
        return Cartridge(rom)
    }

    @Test
    fun read_returns_correct_byte_at_address() {
        val cartridge = makeRom { it[0x100] = 0xABu }
        assertEquals(0xABu, cartridge.read(0x100.toUShort()))
    }

    @Test
    fun read_out_of_bounds_returns_0xFF() {
        val cartridge = makeRom(size = 0x4000) // 16KB ROM
        assertEquals(0xFFu, cartridge.read(0x5000.toUShort()))
    }

    @Test
    fun title_parsed_from_header() {
        val cartridge = makeRom {
            val title = "TETRIS"
            title.forEachIndexed { i, c -> it[0x134 + i] = c.code.toUByte() }
        }
        assertEquals("TETRIS", cartridge.title)
    }

    @Test
    fun title_stops_at_null_terminator() {
        val cartridge = makeRom {
            it[0x134] = 'A'.code.toUByte()
            it[0x135] = 'B'.code.toUByte()
            it[0x136] = 0u
            it[0x137] = 'X'.code.toUByte() // should not appear in title
        }
        assertEquals("AB", cartridge.title)
    }

    @Test
    fun type_reads_cartridge_type_byte() {
        val cartridge = makeRom { it[0x147] = 0x01u } // MBC1
        assertEquals(0x01u, cartridge.type)
    }

    @Test
    fun fromBytes_converts_signed_bytes() {
        val bytes = byteArrayOf(0x00, 0xFF.toByte(), 0x80.toByte())
        val cartridge = Cartridge.fromBytes(bytes)
        assertEquals(0x00u, cartridge.read(0x0000.toUShort()))
        assertEquals(0xFFu, cartridge.read(0x0001.toUShort()))
        assertEquals(0x80u, cartridge.read(0x0002.toUShort()))
    }
}
