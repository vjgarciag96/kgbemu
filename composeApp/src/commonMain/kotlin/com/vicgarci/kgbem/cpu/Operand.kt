package com.vicgarci.kgbem.cpu

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

data class MemoryAtRegister16(
    val register: Register16,
) : Operand8

/**
 * Represents the memory address in the 16-bit immediate operand (nn).
 */
data object MemoryAtData16 : Operand8

/**
 * Represents the high memory address 0xFF00 + immediate 8-bit offset (n).
 */
data object MemoryAtHighData8 : Operand8

/**
 * Represents the high memory address 0xFF00 + register C.
 */
data object MemoryAtHighC : Operand8
