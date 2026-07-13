package com.vicgarci.kgbem.joypad

class JoypadRegister {
    @Volatile
    var state: JoypadState = JoypadState()
        private set

    private var selectBits: Int = 0x30 // bits 5-4; 0x30 = both deselected initially

    var interruptRequested: Boolean = false
        private set

    fun consumeInterrupt(): Boolean {
        val r = interruptRequested
        interruptRequested = false
        return r
    }

    fun read(): Int {
        val dirNibble = directionNibble()
        val btnNibble = buttonNibble()
        val bit5 = (selectBits ushr 5) and 1 // 0 = directions selected
        val bit4 = (selectBits ushr 4) and 1 // 0 = buttons selected

        val resultNibble = when {
            bit5 == 0 && bit4 == 0 -> dirNibble and btnNibble
            bit5 == 0 -> dirNibble
            bit4 == 0 -> btnNibble
            else -> 0x0F // nothing selected, all bits high
        }

        return 0xC0 or (selectBits and 0x30) or resultNibble
    }

    fun write(value: Int) {
        selectBits = value and 0x30 // only bits 5-4 matter
    }

    fun update(newState: JoypadState) {
        val prev = state
        state = newState
        // Interrupt on any false -> true transition
        if (
            (!prev.right && newState.right) ||
            (!prev.left && newState.left) ||
            (!prev.up && newState.up) ||
            (!prev.down && newState.down) ||
            (!prev.a && newState.a) ||
            (!prev.b && newState.b) ||
            (!prev.select && newState.select) ||
            (!prev.start && newState.start)
        ) {
            interruptRequested = true
        }
    }

    private fun directionNibble(): Int {
        var n = 0x0F
        if (state.right) n = n and 0x0E
        if (state.left) n = n and 0x0D
        if (state.up) n = n and 0x0B
        if (state.down) n = n and 0x07
        return n
    }

    private fun buttonNibble(): Int {
        var n = 0x0F
        if (state.a) n = n and 0x0E
        if (state.b) n = n and 0x0D
        if (state.select) n = n and 0x0B
        if (state.start) n = n and 0x07
        return n
    }
}
