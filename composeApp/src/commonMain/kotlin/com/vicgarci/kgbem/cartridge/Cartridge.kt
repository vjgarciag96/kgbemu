package com.vicgarci.kgbem.cartridge

class Cartridge(private val rom: UByteArray) {

    val title: String = buildString {
        for (i in 0x0134..0x0143) {
            val b = rom.getOrElse(i) { 0u }
            if (b == 0u.toUByte()) break
            append(b.toInt().toChar())
        }
    }

    val type: Int = rom.getOrElse(0x0147) { 0u }.toInt()

    fun readRomByte(address: Int): UByte = rom.getOrElse(address) { 0xFFu }
}
