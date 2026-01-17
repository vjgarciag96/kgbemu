package com.vicgarci.kgbem.cpu

sealed interface Instruction {

    data class Add(
        val target: Register8,
    ) : Instruction

    data class AddHl(
        val target: Register8,
    ) : Instruction

    /**
     * Add with carry
     */
    data class AddC(
        val target: Register8,
    ) : Instruction

    data class Sub(
        val target: Register8,
    ) : Instruction

    /**
     * Subtract with carry
     */
    data class Sbc(
        val target: Register8,
    ) : Instruction

    data class And(
        val target: Register8,
    ) : Instruction

    data class Or(
        val target: Register8,
    ) : Instruction

    data class Xor(
        val target: Register8,
    ) : Instruction

    /**
     * Compare (like subtract, but discarding the result and only setting the flags)
     */
    data class Cp(
        val target: Register8,
    ) : Instruction

    /**
     * Does not perform a full arithmetic operation with carry propagation (i.e., carry flag
     * mustn't change).
     */
    data class Inc(
        val target: Register,
    ) : Instruction

    /**
     * Does not perform a full arithmetic operation with carry propagation (i.e., carry flag
     * mustn't change).
     */
    data class Dec(
        val target: Register,
    ) : Instruction

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
        val target: Operand8,
    ) : Instruction

    /**
     * Rotate register [target] right circularly (i.e., least significant bit wraps around to most
     * significant bit, and becomes the new carry).
     *
     * Zero and carry flags are set according to result. Subtract and half carry are set to false.
     */
    data class Rrc(
        val target: Operand8,
    ) : Instruction

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
        val target: Operand8,
    ) : Instruction

    /**
     * Rotate register [target] left circularly (i.e., most significant bit wraps around to least
     * significant bit, and becomes the new carry).
     *
     * Zero and carry flags are set according to result. Subtract and half carry are set to false.
     */
    data class Rlc(
        val target: Operand8,
    ) : Instruction

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
        val target: Operand8,
    ) : Instruction {

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
        val target: Operand8,
    ) : Instruction {

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
        val target: Operand8,
    ) : Instruction {

        init {
            require(index in 0..7)
        }
    }

    /**
     * Shift right logically register [target].
     */
    data class Srl(
        val target: Operand8,
    ) : Instruction

    /**
     * Shift right arithmetically register [target].
     *
     * Most significant bit (sign bit) is preserved, rest is shifted.
     *
     * Zero and carry flags are set according to result. Subtract and half carry are set to false.
     */
    data class Sra(
        val target: Operand8,
    ) : Instruction

    /**
     * Shift left arithmetically register [target].
     *
     * Most significant bit moves into carry flag, least significant bit becomes zero.
     *
     * Zero and carry flags are set according to result. Subtract and half carry are set to false.
     */
    data class Sla(
        val target: Operand8,
    ) : Instruction

    /**
     * Switch upper and lower nibble (most and least significant 4 bits) of a specific register.
     */
    data class Swap(
        val target: Operand8,
    ) : Instruction

    data object Nop : Instruction

    /**
     * Jump to a particular address dependent on one [condition]. The address to jump to is
     * located in the 2 bytes following the instruction identifier.
     */
    data class Jp(
        val condition: JumpCondition,
    ) : Instruction

    /**
     * Load a byte [source] into a register [target].
     */
    data class Ld(
        val source: Operand,
        val target: Operand,
    ) : Instruction

    /**
     * Load the value of register A into the memory address pointed by HL, then increment HL.
     */
    data object LdIncHLA: Instruction

    /**
     * Load the value of the memory address pointed by HL into register A, then increment HL.
     */
    data object LdIncAHL: Instruction
    /**
     * Load the value of register A into the memory address pointed by HL, then decrement HL.
     */
    data object LdDecHLA: Instruction
    /**
     * Load the value of the memory address pointed by HL into register A, then decrement HL.
     */
    data object LdDecAHL: Instruction

    /**
     * Pop a value from the stack into a 16-bit register [target].
     */
    data class Pop(
        val target: Register16,
    ) : Instruction

    /**
     * Push a value from a 16-bit register [target] onto the stack.
     */
    data class Push(
        val target: Register16,
    ) : Instruction

    data class Call(
        val condition: JumpCondition,
    ) : Instruction

    data class Ret(
        val condition: JumpCondition,
    ) : Instruction

    data class Jr(
        val condition: JumpCondition,
    ) : Instruction

    data class Rst(
        val address: UByte,
    ) : Instruction

    /**
     * Decimal adjust register A for BCD addition/subtraction. BCD = Binary-Coded Decimal, a form
     * of number representation where each nibble (4 bits) represents a decimal digit (0-9).
     *
     * For more information, see: https://en.wikipedia.org/wiki/Binary-coded_decimal
     */
    data object Daa : Instruction

    /**
     * Halt the CPU until an interrupt occurs.
     */
    data object Halt : Instruction
    data object DisableInterrupts : Instruction
    data object EnableInterrupts : Instruction

    /**
     * Return from a subroutine and enable interrupts.
     */
    data object RetI : Instruction
}

sealed interface Operand

/**
 * An operand that is 8 bits wide.
 */
sealed interface Operand8 : Operand

/**
 * An operand that is 16 bits wide.
 */
sealed interface Operand16 : Operand

sealed interface Register : Operand

enum class Register8 : Register, Operand8 {
    A, B, C, D, E, H, L
}

enum class Register16 : Register, Operand16 {
    AF, BC, DE, HL,
    SP,
}

/**
 * Represents an 8-bit constant located in the byte following the instruction.
 */
data object Data8 : Operand8

/**
 * Represents a 16-bit constant located in the two bytes following the instruction.
 */
data object Data16 : Operand16

/**
 * Represents the memory address pointed by the HL register pair.
 */
data object MemoryAtHl : Operand8
