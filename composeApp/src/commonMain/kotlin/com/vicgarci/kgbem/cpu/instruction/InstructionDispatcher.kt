package com.vicgarci.kgbem.cpu.instruction

import com.vicgarci.kgbem.cpu.Instruction

internal fun CPUInstructionScope.execute(instruction: Instruction) {
    when (instruction) {
        is Instruction.AddHl,
        Instruction.AddSp -> executeArithmetic16(instruction)
        is Instruction.Add,
        is Instruction.AddC,
        is Instruction.Sub,
        is Instruction.Sbc,
        is Instruction.And,
        is Instruction.Or,
        is Instruction.Xor,
        is Instruction.Cp -> executeArithmeticLogic8(instruction)
        is Instruction.Inc,
        is Instruction.Dec -> executeIncDec(instruction)
        Instruction.Ccf,
        Instruction.Scf,
        Instruction.Cpl,
        Instruction.Daa -> executeFlagOps(instruction)
        Instruction.Rra,
        is Instruction.Rr,
        is Instruction.Rrc,
        Instruction.Rla,
        is Instruction.Rl,
        is Instruction.Rlc,
        Instruction.Rrca,
        Instruction.Rlca,
        is Instruction.Bit,
        is Instruction.Res,
        is Instruction.Set,
        is Instruction.Srl,
        is Instruction.Sra,
        is Instruction.Sla,
        is Instruction.Swap -> executeBitOps(instruction)
        is Instruction.LoadInstruction -> executeLoad(instruction)
        is Instruction.Jp,
        Instruction.JpHl,
        is Instruction.Jr,
        is Instruction.Call,
        is Instruction.Ret,
        Instruction.RetI,
        is Instruction.Rst -> executeControlFlow(instruction)
        is Instruction.Pop,
        is Instruction.Push -> executeStack(instruction)
        Instruction.Halt,
        Instruction.Stop,
        Instruction.DisableInterrupts,
        Instruction.EnableInterrupts,
        Instruction.Nop -> executeCpuControl(instruction)
    }
}
