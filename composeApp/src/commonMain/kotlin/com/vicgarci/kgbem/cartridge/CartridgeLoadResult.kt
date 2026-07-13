package com.vicgarci.kgbem.cartridge

sealed class CartridgeLoadResult {
    data class Success(val cartridge: Cartridge) : CartridgeLoadResult()
    data class Failure(val error: RomError) : CartridgeLoadResult()
}
