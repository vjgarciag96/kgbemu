package com.vicgarci.kgbem.platform

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * Shared state bridging the Composable world (which owns the ActivityResultLauncher)
 * and the suspend [pickRomFile] function (which coroutine callers await).
 *
 * [RegisterFilePicker] must be called unconditionally at composition root so that
 * the launcher is always registered before any click handler invokes [pickRomFile].
 */
internal object AndroidFilePicker {
    /** Application context for ContentResolver and filesDir access. */
    var context: Context? = null

    /** The deferred that [pickRomFile] is currently awaiting; null when idle. */
    val pendingResult = AtomicReference<CompletableDeferred<Uri?>?>()

    /** Launches the system file picker. Set by [RegisterFilePicker]. */
    var launch: (() -> Unit)? = null

    /** Called by the ActivityResultLauncher callback to deliver the result. */
    fun onResult(uri: Uri?) {
        pendingResult.getAndSet(null)?.complete(uri)
    }
}

/**
 * Must be called **unconditionally** at the composition root (e.g. inside
 * `setContent {}` in MainActivity) — never inside a click handler or lambda.
 *
 * Registers an [ActivityResultContracts.GetContent] launcher and wires it
 * into [AndroidFilePicker] so that [pickRomFile] can trigger and await it.
 */
@Composable
fun RegisterFilePicker() {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        AndroidFilePicker.onResult(uri)
    }
    AndroidFilePicker.launch = { launcher.launch("*/*") }
}

/**
 * Opens the system file picker, reads the selected ROM into memory via
 * [android.content.ContentResolver], and caches the bytes to
 * `filesDir/roms/<sanitised-name>.gb` so the emulator can reload without
 * re-opening the picker (the content:// URI is one-time and may be revoked).
 *
 * Returns `null` when the user cancels the picker.
 */
actual suspend fun pickRomFile(): ByteArray? {
    val launchPicker = AndroidFilePicker.launch ?: return null
    val deferred = CompletableDeferred<Uri?>()
    // Cancel any prior pending call that was never resolved (defensive against concurrent calls).
    AndroidFilePicker.pendingResult.getAndSet(deferred)?.cancel()

    // The launcher must be invoked on the main thread.
    withContext(Dispatchers.Main) { launchPicker() }

    val uri = deferred.await() ?: return null

    val ctx = AndroidFilePicker.context ?: return null
    return withContext(Dispatchers.IO) {
        readAndCacheRom(ctx, uri)
    }
}

/**
 * Reads the full byte content from [uri] via ContentResolver, then writes a
 * cached copy to `filesDir/roms/` under a filesystem-safe name.
 */
private fun readAndCacheRom(context: Context, uri: Uri): ByteArray? {
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: return null

    val romsDir = File(context.filesDir, "roms").also { it.mkdirs() }
    val name = sanitiseRomName(uri.lastPathSegment ?: "rom")
    File(romsDir, "$name.gb").writeBytes(bytes)

    return bytes
}

/**
 * Strips characters that are unsafe for filenames and caps the length at 50.
 */
private fun sanitiseRomName(raw: String): String =
    raw.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        .take(50)
        .ifEmpty { "rom" }
