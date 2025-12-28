package com.vicgarci.kgbem.cpu

class ProgramCounter(
    private var value: UShort,
) {

    /**
     * Return the current program counter and increase it by one.
     */
    fun getAndIncrement(): UShort {
        val currValue = value
        increaseBy(stepSize = 1)
        return currValue
    }

    /**
     * Set the program counter to [address].
     */
    fun setTo(address: UShort) {
        value = address
    }

    /**
     * Increase the program counter by [stepSize].
     */
    fun increaseBy(stepSize: Int) {
        value = (value.toInt() + stepSize).toUShort()
    }
}