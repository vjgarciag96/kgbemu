package com.vicgarci.kgbem.cpu.instruction

import com.vicgarci.kgbem.cpu.FlagsRegister.Companion.toFlagsRegister
import com.vicgarci.kgbem.cpu.FlagsRegister.Companion.toUByte
import com.vicgarci.kgbem.cpu.Instruction
import com.vicgarci.kgbem.cpu.Register16
import com.vicgarci.kgbem.cpu.overflowAdd

internal fun CPUInstructionScope.executeArithmetic16(instruction: Instruction) {
    when (instruction) {
        is Instruction.AddHl -> addHl(instruction.target)
        Instruction.AddSp -> addSp()
        else -> error("Unsupported 16-bit arithmetic instruction: $instruction")
    }
}

private fun CPUInstructionScope.addHl(target: Register16) {
    val hl = readRegister16(Register16.HL)
    val targetValue = readRegister16(target)
    val (sum, carry, halfCarry) = overflowAdd(hl, targetValue)

    writeRegister16(Register16.HL, sum)
    val flags = readFlagsRegister().toFlagsRegister().copy(
        subtract = false,
        halfCarry = halfCarry,
        carry = carry,
    )
    writeFlagsRegister(flags.toUByte())
}

private fun CPUInstructionScope.addSp() {
    writeRegister16(Register16.SP, addSignedToSp())
}
