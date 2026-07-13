package com.vicgarci.kgbem.di

import com.vicgarci.kgbem.emulator.EmulatorController
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.SingleIn

@DependencyGraph
@SingleIn(AppScope::class)
interface AppGraph {
    val emulatorController: EmulatorController
}
