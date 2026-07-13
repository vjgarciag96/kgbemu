package com.vicgarci.kgbem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.vicgarci.kgbem.di.AppGraph
import com.vicgarci.kgbem.platform.AndroidFilePicker
import com.vicgarci.kgbem.platform.RegisterFilePicker
import dev.zacsweers.metro.createGraph

class MainActivity : ComponentActivity() {
    private val appGraph by lazy { createGraph<AppGraph>() }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        AndroidFilePicker.context = applicationContext

        setContent {
            RegisterFilePicker()
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}