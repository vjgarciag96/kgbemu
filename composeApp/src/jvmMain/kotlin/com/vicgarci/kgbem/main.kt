package com.vicgarci.kgbem

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.vicgarci.kgbem.di.AppGraph
import dev.zacsweers.metro.createGraph

fun main() {
    val appGraph = createGraph<AppGraph>()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Kgbem",
        ) {
            App(appGraph.emulatorController)
        }
    }
}