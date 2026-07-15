package com.vicgarci.kgbem.cartridge

object CartridgeLoader {

    private const val MIN_HEADER_SIZE = 0x150

    private val NINTENDO_LOGO = byteArrayOf(
        0xCE.toByte(), 0xED.toByte(), 0x66, 0x66, 0xCC.toByte(), 0x0D, 0x00, 0x0B,
        0x03, 0x73, 0x00, 0x83.toByte(), 0x00, 0x0C, 0x00, 0x0D,
        0x00, 0x08, 0x11, 0x1F, 0x88.toByte(), 0x89.toByte(), 0x00, 0x0E,
        0xDC.toByte(), 0xCC.toByte(), 0x6E, 0xE6.toByte(), 0xDD.toByte(), 0xDD.toByte(), 0xD9.toByte(), 0x99.toByte(),
        0xBB.toByte(), 0xBB.toByte(), 0x67, 0x63, 0x6E, 0x0E, 0xEC.toByte(), 0xCC.toByte(),
        0xDD.toByte(), 0xDC.toByte(), 0x99.toByte(), 0x9F.toByte(), 0xBB.toByte(), 0xB9.toByte(), 0x33, 0x3E
    )

    private const val LOGO_START = 0x0104
    private const val LOGO_END = 0x0133
    private const val CHECKSUM_START = 0x0134
    private const val CHECKSUM_END = 0x014C
    private const val CHECKSUM_ADDR = 0x014D
    private const val CARTRIDGE_TYPE_ADDR = 0x0147

    private const val TYPE_ROM_ONLY = 0x00

    fun load(bytes: ByteArray): CartridgeLoadResult {
        if (bytes.size < MIN_HEADER_SIZE) {
            return CartridgeLoadResult.Failure(RomError.Truncated)
        }

        if (!validateNintendoLogo(bytes)) {
            return CartridgeLoadResult.Failure(RomError.InvalidHeader)
        }

        if (!validateHeaderChecksum(bytes)) {
            return CartridgeLoadResult.Failure(RomError.InvalidHeader)
        }

        val typeId = bytes[CARTRIDGE_TYPE_ADDR].toInt() and 0xFF
        return when (typeId) {
            TYPE_ROM_ONLY -> CartridgeLoadResult.Success(RomOnlyCartridge(bytes))
            Mbc1Cartridge.TYPE_MBC1,
            Mbc1Cartridge.TYPE_MBC1_RAM,
            Mbc1Cartridge.TYPE_MBC1_RAM_BATTERY ->
                CartridgeLoadResult.Success(Mbc1Cartridge(bytes, typeId))
            else -> CartridgeLoadResult.Failure(RomError.UnsupportedMapper(typeId))
        }
    }

    private fun validateNintendoLogo(bytes: ByteArray): Boolean {
        for (i in NINTENDO_LOGO.indices) {
            if (bytes[LOGO_START + i] != NINTENDO_LOGO[i]) return false
        }
        return true
    }

    private fun validateHeaderChecksum(bytes: ByteArray): Boolean {
        var sum = 0
        for (i in CHECKSUM_START..CHECKSUM_END) {
            sum = sum - bytes[i] - 1
        }
        return (sum and 0xFF) == (bytes[CHECKSUM_ADDR].toInt() and 0xFF)
    }
}
