package com.vicgarci.kgbem

import androidx.compose.runtime.Composable

@Composable
expect fun RomLoader(onRomLoaded: (UByteArray) -> Unit)
