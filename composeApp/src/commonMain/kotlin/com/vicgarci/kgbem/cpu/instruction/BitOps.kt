package com.vicgarci.kgbem.cpu.instruction

import com.vicgarci.kgbem.cpu.FlagsRegister
import com.vicgarci.kgbem.cpu.FlagsRegister.Companion.toFlagsRegister
import com.vicgarci.kgbem.cpu.FlagsRegister.Companion.toUByte
import com.vicgarci.kgbem.cpu.Instruction
import com.vicgarci.kgbem.cpu.Operand8

internal fun CPUInstructionScope.executeBitOps(instruction: Instruction) {
    when (instruction) {
        Instruction.Rra -> rra()
        is Instruction.Rr -> rr(instruction.target)
        is Instruction.Rrc -> rrc(instruction.target)
        Instruction.Rla -> rla()
        is Instruction.Rl -> rl(instruction.target)
        is Instruction.Rlc -> rlc(instruction.target)
        Instruction.Rrca -> rrca()
        Instruction.Rlca -> rlca()
        is Instruction.Bit -> bit(instruction.index, instruction.target)
        is Instruction.Res -> res(instruction.index, instruction.target)
        is Instruction.Set -> set(instruction.index, instruction.target)
        is Instruction.Srl -> srl(instruction.target)
        is Instruction.Sra -> sra(instruction.target)
        is Instruction.Sla -> sla(instruction.target)
        is Instruction.Swap -> swap(instruction.target)
        else -> error("Unsupported bit operation instruction: $instruction")
    }
}

private fun CPUInstructionScope.rra() {
    val leastSignificantBit = readAccumulator() and 0b1.toUByte()
    val flags = readFlagsRegister().toFlagsRegister()
    val carryBit = if (flags.carry) 0b1 else 0b0
    val rotatedA = readAccumulator().toInt() ushr 1
    val rotatedCarry = carryBit shl 7
    writeAccumulator(((rotatedA or rotatedCarry) and 0xFF).toUByte())
    writeFlagsRegister(
        flags.copy(
            carry = leastSignificantBit == 0b1.toUByte(),
        ).toUByte()
    )
}

private fun CPUInstructionScope.rr(target: Operand8) {
    val targetValue = readOperand8(target)
    val leastSignificantBit = targetValue and 0b1.toUByte()
    val flags = readFlagsRegister().toFlagsRegister()
    val carryBit = if (flags.carry) 0b1 else 0b0
    val rotatedValue = targetValue.toInt() ushr 1
    val rotatedCarry = carryBit shl 7
    val result = ((rotatedValue or rotatedCarry) and 0xFF).toUByte()
    writeFlagsRegister(
        FlagsRegister(
            zero = result == 0b0.toUByte(),
            subtract = false,
            halfCarry = false,
            carry = leastSignificantBit == 0b1.toUByte(),
        ).toUByte()
    )
    writeOperand8(target, result)
}

private fun CPUInstructionScope.rrc(target: Operand8) {
    val targetValue = readOperand8(target)
    val leastSignificantBit = targetValue and 0b1.toUByte()
    val bitToWrapAround = leastSignificantBit.toInt() shl 7
    val rotatedValue = targetValue.toInt() ushr 1
    val result = ((rotatedValue or bitToWrapAround) and 0xFF).toUByte()
    writeFlagsRegister(
        FlagsRegister(
            zero = result == 0b0.toUByte(),
            subtract = false,
            halfCarry = false,
            carry = leastSignificantBit == 0b1.toUByte(),
        ).toUByte()
    )
    writeOperand8(target, result)
}

private fun CPUInstructionScope.rla() {
    val mostSignificantBit = (readAccumulator() and (0b1 shl 7).toUByte()).toInt() ushr 7
    val flags = readFlagsRegister().toFlagsRegister()
    val carryBit = if (flags.carry) 0b1 else 0b0
    val rotatedA = readAccumulator().toInt() shl 1
    writeAccumulator(((rotatedA or carryBit) and 0xFF).toUByte())
    writeFlagsRegister(
        flags.copy(
            carry = (mostSignificantBit and 0xFF).toUByte() == 0b1.toUByte(),
        ).toUByte()
    )
}

private fun CPUInstructionScope.rl(target: Operand8) {
    val targetValue = readOperand8(target)
    val mostSignificantBit = (targetValue and (0b1 shl 7).toUByte()).toInt() ushr 7
    val flags = readFlagsRegister().toFlagsRegister()
    val carryBit = if (flags.carry) 0b1 else 0b0
    val rotatedValue = targetValue.toInt() shl 1
    val result = ((rotatedValue or carryBit) and 0xFF).toUByte()
    writeFlagsRegister(
        FlagsRegister(
            zero = result == 0b0.toUByte(),
            subtract = false,
            halfCarry = false,
            carry = (mostSignificantBit and 0xFF).toUByte() == 0b1.toUByte(),
        ).toUByte()
    )
    writeOperand8(target, result)
}

private fun CPUInstructionScope.rlc(target: Operand8) {
    val targetValue = readOperand8(target)
    val mostSignificantBit = (targetValue and (0b1 shl 7).toUByte()).toInt() ushr 7
    val rotatedValue = targetValue.toInt() shl 1
    val result = ((rotatedValue or mostSignificantBit) and 0xFF).toUByte()
    writeFlagsRegister(
        FlagsRegister(
            zero = result == 0b0.toUByte(),
            subtract = false,
            halfCarry = false,
            carry = (mostSignificantBit and 0xFF).toUByte() == 0b1.toUByte(),
        ).toUByte()
    )
    writeOperand8(target, result)
}

private fun CPUInstructionScope.rrca() {
    val leastSignificantBit = readAccumulator() and 0b1.toUByte()
    val flags = readFlagsRegister().toFlagsRegister()
    val rotatedA = (readAccumulator().toInt() ushr 1) or (leastSignificantBit.toInt() shl 7)
    writeAccumulator((rotatedA and 0xFF).toUByte())
    writeFlagsRegister(
        flags.copy(
            carry = leastSignificantBit == 0b1.toUByte(),
        ).toUByte()
    )
}

private fun CPUInstructionScope.rlca() {
    val mostSignificantBit = (readAccumulator() and (0b1 shl 7).toUByte()).toInt() ushr 7
    val flags = readFlagsRegister().toFlagsRegister()
    val rotatedA = readAccumulator().toInt() shl 1
    writeAccumulator(((rotatedA or mostSignificantBit) and 0xFF).toUByte())
    writeFlagsRegister(
        flags.copy(
            carry = (mostSignificantBit and 0xFF).toUByte() == 0b1.toUByte(),
        ).toUByte()
    )
}

private fun CPUInstructionScope.bit(index: Int, target: Operand8) {
    val targetValue = readOperand8(target)
    val bitSet = ((targetValue.toInt() ushr index).toUByte() and 0b1.toUByte()) == 0b1.toUByte()
    val flags = readFlagsRegister().toFlagsRegister()
    writeFlagsRegister(
        flags.copy(
            zero = !bitSet,
            subtract = false,
            halfCarry = true,
        ).toUByte()
    )
}

private fun CPUInstructionScope.res(index: Int, target: Operand8) {
    val targetValue = readOperand8(target)
    val mask = (0b1 shl index).toUByte().inv()
    writeOperand8(target, targetValue and mask)
}

private fun CPUInstructionScope.set(index: Int, target: Operand8) {
    val targetValue = readOperand8(target)
    val mask = (0b1 shl index).toUByte()
    writeOperand8(target, targetValue or mask)
}

private fun CPUInstructionScope.srl(target: Operand8) {
    val targetValue = readOperand8(target)
    val leastSignificantBit = targetValue and 0b1.toUByte()
    val shiftedValue = ((targetValue.toInt() ushr 1) and 0xFF).toUByte()

    writeFlagsRegister(
        readFlagsRegister().toFlagsRegister().copy(
            zero = shiftedValue == 0b0.toUByte(),
            carry = leastSignificantBit == 0b1.toUByte(),
        ).toUByte()
    )
    writeOperand8(target, shiftedValue)
}

private fun CPUInstructionScope.sra(target: Operand8) {
    val targetValue = readOperand8(target)
    val leastSignificantBit = targetValue and 0b1.toUByte()
    val mostSignificantBit = targetValue and (0b1 shl 7).toUByte()
    val shiftedValue = ((targetValue.toInt() ushr 1) and 0xFF).toUByte() or mostSignificantBit

    writeFlagsRegister(
        FlagsRegister(
            zero = shiftedValue == 0b0.toUByte(),
            subtract = false,
            halfCarry = false,
            carry = leastSignificantBit == 0b1.toUByte(),
        ).toUByte()
    )
    writeOperand8(target, shiftedValue)
}

private fun CPUInstructionScope.sla(target: Operand8) {
    val targetValue = readOperand8(target)
    val mostSignificantBit = targetValue and (0b1 shl 7).toUByte()
    val shiftedValue = ((targetValue.toInt() shl 1) and 0xFF).toUByte()

    writeFlagsRegister(
        FlagsRegister(
            zero = shiftedValue == 0b0.toUByte(),
            subtract = false,
            halfCarry = false,
            carry = ((mostSignificantBit.toInt() ushr 7) and 0xFF).toUByte() == 0b1.toUByte(),
        ).toUByte()
    )
    writeOperand8(target, shiftedValue)
}

private fun CPUInstructionScope.swap(target: Operand8) {
    val targetValue = readOperand8(target)
    val upperNibble = targetValue and 0xF0.toUByte()
    val lowerNibble = targetValue and 0x0F.toUByte()

    val shiftedUpperNibble = ((upperNibble.toInt() ushr 4) and 0xFF).toUByte()
    val shiftedLowerNibble = ((lowerNibble.toInt() shl 4) and 0xFF).toUByte()
    val result = shiftedUpperNibble or shiftedLowerNibble

    writeFlagsRegister(
        FlagsRegister(
            zero = result == 0b0.toUByte(),
            subtract = false,
            halfCarry = false,
            carry = false,
        ).toUByte()
    )
    writeOperand8(target, result)
}
