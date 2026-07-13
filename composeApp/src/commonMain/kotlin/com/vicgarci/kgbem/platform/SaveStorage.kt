package com.vicgarci.kgbem.platform

// Persists cartridge battery RAM (and MBC3 RTC state) to app-private storage.
// romTitle is sanitised (alphanumeric only, max 16 chars) before use as a filename.
expect object SaveStorage {
    fun load(romTitle: String): ByteArray?
    fun save(romTitle: String, data: ByteArray)
}
