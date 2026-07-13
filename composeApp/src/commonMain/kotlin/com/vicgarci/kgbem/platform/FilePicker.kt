package com.vicgarci.kgbem.platform

// Returns the ROM file bytes chosen by the user, or null if cancelled.
// The common module never sees a URI, File, or path — only ByteArray?.
// Must be called from a coroutine.
expect suspend fun pickRomFile(): ByteArray?
