package com.vicgarci.kgbem.cpu.instruction

import com.vicgarci.kgbem.cpu.Instruction
import com.vicgarci.kgbem.cpu.JumpCondition
import com.vicgarci.kgbem.cpu.Register16

internal fun CPUInstructionScope.executeControlFlow(instruction: Instruction.ControlFlowInstruction) {
    when (instruction) {
        is Instruction.Jp -> jump(instruction.condition)
        Instruction.JpHl -> jumpHl()
        is Instruction.Jr -> jumpRelative(instruction.condition)
        is Instruction.Call -> call(instruction.condition)
        is Instruction.Ret -> ret(instruction.condition)
        Instruction.RetI -> returnAndEnableInterrupts()
        is Instruction.Rst -> rst(instruction.address)
    }
}

private fun CPUInstructionScope.jump(condition: JumpCondition) {
    val shouldJump = shouldJump(condition)
    if (shouldJump) {
        val address = readImmediate16()
        writeProgramCounter(address)
    } else {
        // Consume the 16-bit immediate jump address.
        increaseProgramCounter(stepSize = 2)
    }
}

private fun CPUInstructionScope.jumpRelative(condition: JumpCondition) {
    val shouldJump = shouldJump(condition)
    if (shouldJump) {
        val offset = readImmediate8()
        increaseProgramCounter(stepSize = offset.toByte().toInt())
    } else {
        // Consume the 8-bit relative offset.
        increaseProgramCounter(stepSize = 1)
    }
}

private fun CPUInstructionScope.jumpHl() {
    writeProgramCounter(readRegister16(Register16.HL))
}

private fun CPUInstructionScope.call(condition: JumpCondition) {
    if (shouldJump(condition)) {
        val address = readImmediate16()
        pushProgramCounterToStack()
        writeProgramCounter(address)
    } else {
        // Consume the 16-bit call target when branch is not taken.
        increaseProgramCounter(stepSize = 2)
    }
}

private fun CPUInstructionScope.ret(condition: JumpCondition) {
    if (shouldJump(condition)) {
        returnFromSubroutine()
    }
}

private fun CPUInstructionScope.returnAndEnableInterrupts() {
    returnFromSubroutine()
    scheduleEnableGlobalInterrupt()
}

private fun CPUInstructionScope.rst(address: UByte) {
    pushProgramCounterToStack()
    writeProgramCounter(address.toUShort())
}
