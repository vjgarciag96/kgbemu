package com.vicgarci.kgbem

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.vicgarci.kgbem.di.AppGraph
import dev.zacsweers.metro.createGraph

fun MainViewController() = ComposeUIViewController {
    val appGraph = remember { createGraph<AppGraph>() }
    App(appGraph.emulatorController)
}