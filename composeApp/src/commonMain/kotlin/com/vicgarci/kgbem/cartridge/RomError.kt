package com.vicgarci.kgbem.cartridge

sealed interface RomError {
    data object Truncated : RomError
    data object InvalidHeader : RomError
    data class UnsupportedMapper(val typeId: Int) : RomError
}
