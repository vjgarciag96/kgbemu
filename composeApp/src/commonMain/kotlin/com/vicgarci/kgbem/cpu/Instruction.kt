package com.vicgarci.kgbem.cpu

sealed interface Instruction {

    val target: ArithmeticTarget

    data class Add(
        override val target: ArithmeticTarget,
    ) : Instruction

    data class AddHl(
        override val target: ArithmeticTarget,
    ) : Instruction
}

enum class ArithmeticTarget {
    A, B, C, D, E, H, L,
}