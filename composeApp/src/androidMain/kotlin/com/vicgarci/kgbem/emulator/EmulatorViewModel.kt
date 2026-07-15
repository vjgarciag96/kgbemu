package com.vicgarci.kgbem.emulator

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Android ViewModel that wraps [EmulatorController] and responds to
 * lifecycle events to pause/resume emulation automatically.
 */
class EmulatorViewModel(
    val emulatorController: EmulatorController,
) : ViewModel(), DefaultLifecycleObserver {

    /** Track whether we were running before onStop so we can auto-resume. */
    private var wasRunning = false

    override fun onStop(owner: LifecycleOwner) {
        wasRunning = emulatorController.emulatorState.value == EmulatorState.Running
        emulatorController.pause()
    }

    override fun onStart(owner: LifecycleOwner) {
        if (wasRunning) {
            emulatorController.resume()
        }
    }

    /**
     * Factory that creates [EmulatorViewModel] from an [EmulatorController].
     */
    class Factory(
        private val emulatorController: EmulatorController,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EmulatorViewModel(emulatorController) as T
        }
    }
}
