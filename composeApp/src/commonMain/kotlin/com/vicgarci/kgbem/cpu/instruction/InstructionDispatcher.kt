package com.vicgarci.kgbem.cpu.instruction

import com.vicgarci.kgbem.cpu.Instruction

internal fun CPUInstructionScope.execute(instruction: Instruction) {
    when (instruction) {
        is Instruction.Arithmetic16Instruction -> executeArithmetic16(instruction)
        is Instruction.ArithmeticLogic8Instruction -> executeArithmeticLogic8(instruction)
        is Instruction.IncDecInstruction -> executeIncDec(instruction)
        is Instruction.FlagInstruction -> executeFlagOps(instruction)
        is Instruction.BitInstruction -> executeBitOps(instruction)
        is Instruction.LoadInstruction -> executeLoad(instruction)
        is Instruction.ControlFlowInstruction -> executeControlFlow(instruction)
        is Instruction.StackInstruction -> executeStack(instruction)
        is Instruction.CpuControlInstruction -> executeCpuControl(instruction)
    }
}
