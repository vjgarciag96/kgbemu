package com.vicgarci.kgbem.test

actual fun loadTestResource(path: String): ByteArray {
    return Thread.currentThread().contextClassLoader
        ?.getResourceAsStream(path)
        ?.readBytes()
        ?: error("Test resource not found: $path")
}
