package com.vicgarci.kgbem.emulator

import com.vicgarci.kgbem.cartridge.RomError

sealed interface EmulatorState {
    data object Idle : EmulatorState
    data object Loading : EmulatorState
    data object Running : EmulatorState
    data object Paused : EmulatorState
    data class Error(val error: RomError) : EmulatorState
}
