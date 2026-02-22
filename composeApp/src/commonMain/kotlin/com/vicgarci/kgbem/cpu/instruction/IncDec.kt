package com.vicgarci.kgbem.cpu.instruction

import com.vicgarci.kgbem.cpu.FlagsRegister.Companion.toFlagsRegister
import com.vicgarci.kgbem.cpu.FlagsRegister.Companion.toUByte
import com.vicgarci.kgbem.cpu.Instruction
import com.vicgarci.kgbem.cpu.Register
import com.vicgarci.kgbem.cpu.Register16
import com.vicgarci.kgbem.cpu.Register8
import com.vicgarci.kgbem.cpu.overflowAdd
import com.vicgarci.kgbem.cpu.sub

internal fun CPUInstructionScope.executeIncDec(instruction: Instruction) {
    when (instruction) {
        is Instruction.Inc -> inc(instruction.target)
        is Instruction.Dec -> dec(instruction.target)
        else -> error("Unsupported INC/DEC instruction: $instruction")
    }
}

private fun CPUInstructionScope.inc(target: Register) {
    when (target) {
        is Register16 -> inc16(target)
        is Register8 -> inc8(target)
    }
}

private fun CPUInstructionScope.dec(target: Register) {
    when (target) {
        is Register16 -> dec16(target)
        is Register8 -> dec8(target)
    }
}

private fun CPUInstructionScope.inc8(target: Register8) {
    val value = readRegister8(target)
    val (sum, _, halfCarry) = overflowAdd(value, 0x1.toUByte())
    val updated = sum.toUByte()
    writeRegister8(target, updated)

    val flags = readFlagsRegister().toFlagsRegister().copy(
        zero = sum == 0.toUShort(),
        subtract = false,
        halfCarry = halfCarry,
    )
    writeFlagsRegister(flags.toUByte())
}

private fun CPUInstructionScope.dec8(target: Register8) {
    val value = readRegister8(target)
    val (result, halfBorrow, _) = sub(value, 0x1.toUByte())
    val updated = result.toUByte()
    writeRegister8(target, updated)

    val flags = readFlagsRegister().toFlagsRegister().copy(
        zero = result == 0x0.toUShort(),
        subtract = true,
        halfCarry = halfBorrow,
    )
    writeFlagsRegister(flags.toUByte())
}

private fun CPUInstructionScope.inc16(target: Register16) {
    val value = readRegister16(target)
    writeRegister16(target, (value.toInt() + 1).toUShort())
}

private fun CPUInstructionScope.dec16(target: Register16) {
    val value = readRegister16(target)
    writeRegister16(target, (value.toInt() - 1).toUShort())
}
