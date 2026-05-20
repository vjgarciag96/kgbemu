package com.vicgarci.kgbem

class Cartridge(val rom: UByteArray) {

    val title: String = buildString {
        for (i in 0x134..0x143) {
            val b = rom[i].toInt()
            if (b == 0) break
            append(b.toChar())
        }
    }.trim()

    val type: UByte get() = rom[0x147]

    fun read(address: UShort): UByte {
        val addr = address.toInt() and 0xFFFF
        return if (addr < rom.size) rom[addr] else 0xFFu
    }

    companion object {
        fun fromBytes(bytes: ByteArray): Cartridge =
            Cartridge(UByteArray(bytes.size) { bytes[it].toUByte() })
    }
}
