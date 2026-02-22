package com.vicgarci.kgbem.cpu.instruction

import com.vicgarci.kgbem.cpu.Instruction

internal fun CPUInstructionScope.executeCpuControl(instruction: Instruction) {
    when (instruction) {
        Instruction.Halt -> haltCpu()
        Instruction.Stop -> haltCpu()
        Instruction.DisableInterrupts -> disableGlobalInterrupts()
        Instruction.EnableInterrupts -> scheduleEnableGlobalInterrupt()
        Instruction.Nop -> {}
        else -> error("Unsupported CPU control instruction: $instruction")
    }
}
