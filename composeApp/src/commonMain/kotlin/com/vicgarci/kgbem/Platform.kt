package com.vicgarci.kgbem

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

/** Returns raw ROM bytes selected by the user, or null if cancelled / unsupported. */
expect fun loadRomBytes(): ByteArray?