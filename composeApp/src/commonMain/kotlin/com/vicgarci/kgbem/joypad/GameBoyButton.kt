package com.vicgarci.kgbem.joypad

/**
 * The eight physical buttons on a Game Boy.
 *
 * Used by [TouchInputSource] to receive button-down / button-up events
 * from the touch-overlay composable (Task 9.1.2).
 */
enum class GameBoyButton {
    RIGHT,
    LEFT,
    UP,
    DOWN,
    A,
    B,
    SELECT,
    START,
}
