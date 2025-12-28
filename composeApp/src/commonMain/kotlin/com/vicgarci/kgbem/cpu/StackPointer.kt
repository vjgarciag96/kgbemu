package com.vicgarci.kgbem.cpu

class StackPointer(
    private var value: UShort = SKIP_BOOT_ROM,
) {

    /**
     * Return the current stack pointer and increase it by one.
     */
    fun increment(): UShort {
        val currValue = value
        value = (value.toInt() + 1).toUShort()
        return currValue
    }

    private companion object {
        // Initial value of the stack pointer to skip the boot ROM
        private val SKIP_BOOT_ROM = 0xFFFE.toUShort()
    }
}