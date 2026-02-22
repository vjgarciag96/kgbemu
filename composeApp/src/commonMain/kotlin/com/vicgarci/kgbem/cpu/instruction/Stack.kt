package com.vicgarci.kgbem.cpu.instruction

import com.vicgarci.kgbem.cpu.Instruction
import com.vicgarci.kgbem.cpu.Register16

internal fun CPUInstructionScope.executeStack(instruction: Instruction.StackInstruction) {
    when (instruction) {
        is Instruction.Pop -> pop(instruction.target)
        is Instruction.Push -> push(instruction.target)
    }
}

private fun CPUInstructionScope.pop(target: Register16) {
    val stackPointer = readRegister16(Register16.SP)
    val leastSignificantByte = readMemory(stackPointer)
    val mostSignificantByte = readMemory((stackPointer + 1u).toUShort())
    writeRegister16(Register16.SP, (stackPointer + 2u).toUShort())

    val value = ((mostSignificantByte.toInt() shl 8) or leastSignificantByte.toInt()).toUShort()
    writeRegister16(target, value)
}

private fun CPUInstructionScope.push(target: Register16) {
    val value = readRegister16(target)
    val mostSignificantByte = ((value.toInt() ushr 8) and 0x00FF).toUByte()
    val leastSignificantByte = (value and 0x00FFu).toUByte()

    val stackPointer = readRegister16(Register16.SP)
    val firstWriteAddress = (stackPointer.toInt() - 1).toUShort()
    writeMemory(firstWriteAddress, mostSignificantByte)

    val secondWriteAddress = (firstWriteAddress.toInt() - 1).toUShort()
    writeMemory(secondWriteAddress, leastSignificantByte)
    writeRegister16(Register16.SP, secondWriteAddress)
}
