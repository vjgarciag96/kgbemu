package com.vicgarci.kgbem.cpu

class StackPointer(
    private var value: UShort = SKIP_BOOT_ROM,
) {

    /**
     * Return the current program counter.
     */
    fun get(): UShort {
        return value
    }

    /**
     * Return the current stack pointer and increase it by one.
     */
    fun getAndIncrement(): UShort {
        val currValue = value
        value = (value.toInt() + 1).toUShort()
        return currValue
    }

    /**
     * Decrease the stack pointer by one and return its value.
     */
    fun decrementAndGet(): UShort {
        value = (value.toInt() - 1).toUShort()
        return value
    }

    /**
     * Set the stack pointer to the given [address].
     */
    fun setTo(address: UShort) {
        value = address
    }

    private companion object {
        // Initial value of the stack pointer to skip the boot ROM
        private val SKIP_BOOT_ROM = 0xFFFE.toUShort()
    }
}