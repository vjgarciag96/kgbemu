package com.vicgarci.kgbem.cpu

sealed interface Instruction {

    val target: ArithmeticTarget

    data class Add(
        override val target: ArithmeticTarget,
    ) : Instruction

    data class AddHl(
        override val target: ArithmeticTarget,
    ) : Instruction

    /**
     * Add with carry
     */
    data class AddC(
        override val target: ArithmeticTarget,
    ) : Instruction

    data class Sub(
        override val target: ArithmeticTarget,
    ) : Instruction

    /**
     * Subtract with carry
     */
    data class Sbc(
        override val target: ArithmeticTarget,
    ) : Instruction

    data class And(
        override val target: ArithmeticTarget,
    ) : Instruction

    data class Or(
        override val target: ArithmeticTarget,
    ) : Instruction

    data class Xor(
        override val target: ArithmeticTarget,
    ) : Instruction

    /**
     * Compare (like subtract, but discarding the result and only setting the flags)
     */
    data class Cp(
        override val target: ArithmeticTarget,
    ) : Instruction
}

enum class ArithmeticTarget {
    A, B, C, D, E, H, L,
}