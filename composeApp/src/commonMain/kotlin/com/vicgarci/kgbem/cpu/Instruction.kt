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
     * Left-rotate the A register through carry (i.e., carry becomes least significant bit of A,
     * most significant bit of A becomes carry).
     *
     * Zero flag, subtract, and half carry are set to false. Carry is set according to result.
     */
    data object Rla : Instruction
}

enum class ArithmeticTarget {
    A, B, C, D, E, H, L,
}