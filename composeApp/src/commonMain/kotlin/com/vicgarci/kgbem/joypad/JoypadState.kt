package com.vicgarci.kgbem.joypad

data class JoypadState(
    val right: Boolean = false,
    val left: Boolean = false,
    val up: Boolean = false,
    val down: Boolean = false,
    val a: Boolean = false,
    val b: Boolean = false,
    val select: Boolean = false,
    val start: Boolean = false,
)
