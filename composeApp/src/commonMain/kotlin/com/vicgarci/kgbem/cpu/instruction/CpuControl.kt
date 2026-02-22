package com.vicgarci.kgbem.cpu.instruction

import com.vicgarci.kgbem.cpu.Instruction

internal fun CPUInstructionScope.executeCpuControl(instruction: Instruction.CpuControlInstruction) {
    when (instruction) {
        Instruction.Halt -> haltCpu()
        Instruction.Stop -> haltCpu()
        Instruction.DisableInterrupts -> disableGlobalInterrupts()
        Instruction.EnableInterrupts -> scheduleEnableGlobalInterrupt()
        Instruction.Nop -> {}
    }
}
