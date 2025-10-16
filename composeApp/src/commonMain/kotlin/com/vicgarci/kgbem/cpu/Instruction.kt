package com.vicgarci.kgbem.cpu

sealed interface Instruction {

    sealed interface ArithmeticTargetInstruction : Instruction {
        val target: ArithmeticTarget
    }

    data class Add(
        override val target: ArithmeticTarget,
    ) : ArithmeticTargetInstruction

    data class AddHl(
        override val target: ArithmeticTarget,
    ) : ArithmeticTargetInstruction

    /**
     * Add with carry
     */
    data class AddC(
        override val target: ArithmeticTarget,
    ) : ArithmeticTargetInstruction

    data class Sub(
        override val target: ArithmeticTarget,
    ) : ArithmeticTargetInstruction

    /**
     * Subtract with carry
     */
    data class Sbc(
        override val target: ArithmeticTarget,
    ) : ArithmeticTargetInstruction

    data class And(
        override val target: ArithmeticTarget,
    ) : ArithmeticTargetInstruction

    data class Or(
        override val target: ArithmeticTarget,
    ) : ArithmeticTargetInstruction

    data class Xor(
        override val target: ArithmeticTarget,
    ) : ArithmeticTargetInstruction

    /**
     * Compare (like subtract, but discarding the result and only setting the flags)
     */
    data class Cp(
        override val target: ArithmeticTarget,
    ) : ArithmeticTargetInstruction

    /**
     * Does not perform a full arithmetic operation with carry propagation (i.e., carry flag
     * mustn't change).
     */
    data class Inc(
        override val target: ArithmeticTarget,
    ) : ArithmeticTargetInstruction

    /**
     * Does not perform a full arithmetic operation with carry propagation (i.e., carry flag
     * mustn't change).
     */
    data class Dec(
        override val target: ArithmeticTarget,
    ) : ArithmeticTargetInstruction

    /**
     * Complement carry flag.
     *
     * Changes subtract and half carry flags to false, and inverts the carry flag.
     */
    data object Ccf : Instruction

    /**
     * Set carry flag.
     *
     * Changes subtract and half carry flags to false, and sets the carry flag to true.
     */
    data object Scf : Instruction

    /**
     * Right-rotate the A register through carry (i.e., carry becomes most significant bit of A,
     * least significant bit of A becomes carry).
     *
     * Zero flag, subtract, and half carry are set to false. Carry is set according to result.
     */
    data object Rra : Instruction

    /**
     * Rotate register [target] right, through carry (i.e., carry becomes most significant bit of
     * register, least significant bit of register becomes carry)
     *
     * Zero and carry flags are set according to result. Subtract and half carry are set to false.
     */
    data class Rr(
        override val target: ArithmeticTarget,
    ) : ArithmeticTargetInstruction

    /**
     * Left-rotate the A register through carry (i.e., carry becomes least significant bit of A,
     * most significant bit of A becomes carry).
     *
     * Zero flag, subtract, and half carry are set to false. Carry is set according to result.
     */
    data object Rla : Instruction

    /**
     * Rotate register [target] left, through carry (i.e., carry becomes least significant bit of
     * register, most significant bit of register becomes carry)
     *
     * Zero and carry flags are set according to result. Subtract and half carry are set to false.
     */
    data class Rl(
        override val target: ArithmeticTarget,
    ) : ArithmeticTargetInstruction

    /**
     * Right-rotate the A register (i.e., least significant bit becomes most significant and is
     * also copied into the carry flag).
     */
    data object Rrca : Instruction

    /**
     * Left-rotate the A register (i.e., most significant bit becomes least significant and is
     * also copied into the carry flag).
     */
    data object Rlca : Instruction

    /**
     * Toggle every bit of the register A.
     */
    data object Cpl : Instruction

    /**
     * Test bit [index] in register [target], set the zero flag if bit not set.
     *
     * @param index position of the bit to test in [target]. From 0 to 7.
     * 0 is the least significant bit, 7 the most.
     */
    data class Bit(
        val index: Int,
        override val target: ArithmeticTarget,
    ) : ArithmeticTargetInstruction {

        init {
            require(index in 0..7)
        }
    }

    /**
     * Set bit [index] in register [target] to 0.
     *
     * @param index position of the bit to test in [target]. From 0 to 7.
     * 0 is the least significant bit, 7 the most.
     */
    data class Res(
        val index: Int,
        override val target: ArithmeticTarget,
    ) : ArithmeticTargetInstruction {

        init {
            require(index in 0..7)
        }
    }

    /**
     * Set bit [index] in register [target] to 1.
     *
     * @param index position of the bit to test in [target]. From 0 to 7.
     * 0 is the least significant bit, 7 the most.
     */
    data class Set(
        val index: Int,
        override val target: ArithmeticTarget,
    ) : ArithmeticTargetInstruction {

        init {
            require(index in 0..7)
        }
    }

    /**
     * Shift right logically register [target].
     */
    data class Srl(
        override val target: ArithmeticTarget,
    ) : ArithmeticTargetInstruction
}

enum class ArithmeticTarget {
    A, B, C, D, E, H, L,
}