package com.vicgarci.kgbem

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.vicgarci.kgbem.di.AppGraph
import dev.zacsweers.metro.createGraph

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val appGraph = createGraph<AppGraph>()
    ComposeViewport {
        App(appGraph.emulatorController)
    }
}