package com.vicgarci.kgbem

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.vicgarci.kgbem.di.AppGraph
import dev.zacsweers.metro.createGraph

fun main() = application {
    val appGraph = createGraph<AppGraph>()
    Window(
        onCloseRequest = ::exitApplication,
        title = "Kgbem",
    ) {
        App(appGraph.emulatorController)
    }
}