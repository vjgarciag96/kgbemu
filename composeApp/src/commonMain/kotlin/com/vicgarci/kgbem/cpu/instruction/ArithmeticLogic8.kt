package com.vicgarci.kgbem.cpu.instruction

import com.vicgarci.kgbem.cpu.FlagsRegister
import com.vicgarci.kgbem.cpu.FlagsRegister.Companion.toFlagsRegister
import com.vicgarci.kgbem.cpu.FlagsRegister.Companion.toUByte
import com.vicgarci.kgbem.cpu.Instruction
import com.vicgarci.kgbem.cpu.overflowAdd
import com.vicgarci.kgbem.cpu.sub

internal fun CPUInstructionScope.executeArithmeticLogic8(instruction: Instruction) {
    when (instruction) {
        is Instruction.Add -> add(instruction.target)
        is Instruction.AddC -> addC(instruction.target)
        is Instruction.Sub -> sub(instruction.target)
        is Instruction.Sbc -> sbc(instruction.target)
        is Instruction.And -> and(instruction.target)
        is Instruction.Or -> or(instruction.target)
        is Instruction.Xor -> xor(instruction.target)
        is Instruction.Cp -> cp(instruction.target)
        else -> error("Unsupported ALU 8-bit instruction: $instruction")
    }
}

private fun CPUInstructionScope.add(target: com.vicgarci.kgbem.cpu.Operand8) {
    val targetValue = readOperand8(target)
    val (sum, carry, halfCarry) = overflowAdd(readAccumulator(), targetValue)
    writeAccumulator(sum.toUByte())
    writeFlagsRegister(
        FlagsRegister(
            zero = sum == 0.toUShort(),
            subtract = false,
            halfCarry = halfCarry,
            carry = carry,
        ).toUByte()
    )
}

private fun CPUInstructionScope.addC(target: com.vicgarci.kgbem.cpu.Operand8) {
    val targetValue = readOperand8(target)
    val carryIn = if (readFlagsRegister().toFlagsRegister().carry) 0x01.toUByte() else 0x00.toUByte()
    val (sum, carryOut, halfCarry) = overflowAdd(readAccumulator(), targetValue, carryIn)
    writeAccumulator(sum.toUByte())
    writeFlagsRegister(
        FlagsRegister(
            zero = sum == 0.toUShort(),
            subtract = false,
            halfCarry = halfCarry,
            carry = carryOut,
        ).toUByte()
    )
}

private fun CPUInstructionScope.sub(target: com.vicgarci.kgbem.cpu.Operand8) {
    val targetValue = readOperand8(target)
    val (result, halfBorrow, borrow) = sub(readAccumulator(), targetValue)
    writeAccumulator(result.toUByte())
    writeFlagsRegister(
        FlagsRegister(
            zero = result == 0.toUShort(),
            subtract = true,
            halfCarry = halfBorrow,
            carry = borrow,
        ).toUByte()
    )
}

private fun CPUInstructionScope.sbc(target: com.vicgarci.kgbem.cpu.Operand8) {
    val targetValue = readOperand8(target)
    val carryIn = if (readFlagsRegister().toFlagsRegister().carry) 0x01.toUByte() else 0x00.toUByte()
    val (result, halfBorrow, borrow) = sub(readAccumulator(), targetValue, carryIn)
    writeAccumulator(result.toUByte())
    writeFlagsRegister(
        FlagsRegister(
            zero = result == 0.toUShort(),
            subtract = true,
            halfCarry = halfBorrow,
            carry = borrow,
        ).toUByte()
    )
}

private fun CPUInstructionScope.and(target: com.vicgarci.kgbem.cpu.Operand8) {
    writeAccumulator(readAccumulator() and readOperand8(target))
}

private fun CPUInstructionScope.or(target: com.vicgarci.kgbem.cpu.Operand8) {
    writeAccumulator(readAccumulator() or readOperand8(target))
}

private fun CPUInstructionScope.xor(target: com.vicgarci.kgbem.cpu.Operand8) {
    writeAccumulator(readAccumulator() xor readOperand8(target))
}

private fun CPUInstructionScope.cp(target: com.vicgarci.kgbem.cpu.Operand8) {
    val targetValue = readOperand8(target)
    val (result, halfBorrow, borrow) = sub(readAccumulator(), targetValue)
    writeFlagsRegister(
        FlagsRegister(
            zero = result == 0.toUShort(),
            subtract = true,
            halfCarry = halfBorrow,
            carry = borrow,
        ).toUByte()
    )
}
