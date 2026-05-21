package com.vicgarci.kgbem.cpu

import com.vicgarci.kgbem.cpu.Instruction.*

internal fun Instruction.cycleCost(branchTaken: Boolean = false): Int = when (this) {
    Nop, Halt, Stop, DisableInterrupts, EnableInterrupts -> 4
    Ccf, Scf, Cpl, Daa -> 4
    Rla, Rra, Rlca, Rrca -> 4
    RetI -> 16
    JpHl -> 4
    AddSp -> 16
    LdSpHl -> 8
    LdHlSpOffset -> 12
    LdMemoryAtData16Sp -> 20
    LdIncAHL, LdDecAHL, LdIncHLA, LdDecHLA -> 8
    is Add -> if (target.isMemoryOrImmediate()) 8 else 4
    is AddC -> if (target.isMemoryOrImmediate()) 8 else 4
    is Sub -> if (target.isMemoryOrImmediate()) 8 else 4
    is Sbc -> if (target.isMemoryOrImmediate()) 8 else 4
    is And -> if (target.isMemoryOrImmediate()) 8 else 4
    is Or -> if (target.isMemoryOrImmediate()) 8 else 4
    is Xor -> if (target.isMemoryOrImmediate()) 8 else 4
    is Cp -> if (target.isMemoryOrImmediate()) 8 else 4
    is Inc -> when (target) {
        is Register8 -> 4
        is Register16 -> 8
        else -> 4
    }
    is Dec -> when (target) {
        is Register8 -> 4
        is Register16 -> 8
        else -> 4
    }
    is AddHl -> 8
    is Ld8 -> when {
        source == MemoryAtData16 || target == MemoryAtData16 -> 16
        source == MemoryAtHighData8 || target == MemoryAtHighData8 -> 12
        source == MemoryAtHighC || target == MemoryAtHighC -> 8
        source == MemoryAtHl || target == MemoryAtHl -> 8
        source is MemoryAtRegister16 || target is MemoryAtRegister16 -> 8
        source == Data8 -> if (target == MemoryAtHl) 12 else 8
        else -> 4
    }
    is Ld16 -> 12
    is Push -> 16
    is Pop -> 12
    is Jp -> if (condition == JumpCondition.ALWAYS) 16 else if (branchTaken) 16 else 12
    is Jr -> if (branchTaken) 12 else 8
    is Call -> if (branchTaken) 24 else 12
    is Ret -> if (condition == JumpCondition.ALWAYS) 16 else if (branchTaken) 20 else 8
    is Rst -> 16
    is Bit -> if (target == MemoryAtHl) 12 else 8
    is Res -> if (target == MemoryAtHl) 16 else 8
    is Set -> if (target == MemoryAtHl) 16 else 8
    is Rlc -> if (target == MemoryAtHl) 16 else 8
    is Rrc -> if (target == MemoryAtHl) 16 else 8
    is Rl -> if (target == MemoryAtHl) 16 else 8
    is Rr -> if (target == MemoryAtHl) 16 else 8
    is Sla -> if (target == MemoryAtHl) 16 else 8
    is Sra -> if (target == MemoryAtHl) 16 else 8
    is Srl -> if (target == MemoryAtHl) 16 else 8
    is Swap -> if (target == MemoryAtHl) 16 else 8
}

private fun Operand8.isMemoryOrImmediate() = this is MemoryAtHl || this == Data8
