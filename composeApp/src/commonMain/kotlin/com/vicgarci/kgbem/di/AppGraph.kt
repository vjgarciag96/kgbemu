package com.vicgarci.kgbem.di

import com.vicgarci.kgbem.emulator.EmulatorController
import com.vicgarci.kgbem.joypad.InputSource
import com.vicgarci.kgbem.joypad.TouchInputSource
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.SingleIn

@DependencyGraph
@SingleIn(AppScope::class)
interface AppGraph {
    val emulatorController: EmulatorController

    @Binds
    fun TouchInputSource.bindInputSource(): InputSource
}
