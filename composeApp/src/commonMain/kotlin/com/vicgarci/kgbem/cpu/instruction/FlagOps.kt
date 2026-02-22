package com.vicgarci.kgbem.cpu.instruction

import com.vicgarci.kgbem.cpu.FlagsRegister.Companion.toFlagsRegister
import com.vicgarci.kgbem.cpu.FlagsRegister.Companion.toUByte
import com.vicgarci.kgbem.cpu.Instruction

internal fun CPUInstructionScope.executeFlagOps(instruction: Instruction.FlagInstruction) {
    when (instruction) {
        Instruction.Ccf -> ccf()
        Instruction.Scf -> scf()
        Instruction.Cpl -> cpl()
        Instruction.Daa -> daa()
    }
}

private fun CPUInstructionScope.ccf() {
    val flags = readFlagsRegister().toFlagsRegister()
    writeFlagsRegister(
        flags.copy(
            subtract = false,
            halfCarry = false,
            carry = !flags.carry,
        ).toUByte()
    )
}

private fun CPUInstructionScope.scf() {
    val flags = readFlagsRegister().toFlagsRegister()
    writeFlagsRegister(
        flags.copy(
            subtract = false,
            halfCarry = false,
            carry = true,
        ).toUByte()
    )
}

private fun CPUInstructionScope.cpl() {
    writeAccumulator(readAccumulator().inv())
}

private fun CPUInstructionScope.daa() {
    var correction = 0
    val flags = readFlagsRegister().toFlagsRegister()
    var accumulator = readAccumulator()

    if (flags.subtract) {
        if (flags.halfCarry) {
            correction = correction or 0x06
        }
        if (flags.carry) {
            correction = correction or 0x60
        }
        accumulator = (accumulator.toInt() - correction).toUByte()
    } else {
        if (flags.carry || accumulator > 0x99.toUByte()) {
            correction = correction or 0x60
            writeFlagsRegister(readFlagsRegister().toFlagsRegister().copy(carry = true).toUByte())
        }
        if (flags.halfCarry || (accumulator and 0x0F.toUByte()) > 0x09.toUByte()) {
            correction = correction or 0x06
        }
        accumulator = (accumulator.toInt() + correction).toUByte()
    }

    writeAccumulator(accumulator)
    writeFlagsRegister(
        readFlagsRegister().toFlagsRegister().copy(
            zero = accumulator == 0.toUByte(),
            halfCarry = false,
        ).toUByte()
    )
}
