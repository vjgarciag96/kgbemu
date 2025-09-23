package com.vicgarci.kgbem

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform