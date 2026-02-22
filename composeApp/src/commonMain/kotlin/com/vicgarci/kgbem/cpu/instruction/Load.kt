package com.vicgarci.kgbem.cpu.instruction

import com.vicgarci.kgbem.cpu.Instruction
import com.vicgarci.kgbem.cpu.Instruction.LoadInstruction
import com.vicgarci.kgbem.cpu.Operand8
import com.vicgarci.kgbem.cpu.Register16
import com.vicgarci.kgbem.cpu.Register8

internal fun CPUInstructionScope.executeLoad(instruction: LoadInstruction) {
    return when (instruction) {
        Instruction.LdDecAHL -> loadDecAhl()
        Instruction.LdIncAHL -> loadIncAhl()
        Instruction.LdDecHLA -> loadDecHla()
        Instruction.LdIncHLA -> loadIncHla()
        Instruction.LdHlSpOffset -> loadHlSpOffset()
        Instruction.LdSpHl -> loadSpHl()
        Instruction.LdMemoryAtData16Sp -> loadMemoryAtData16Sp()
        is Instruction.Ld8 -> load(instruction.source, instruction.target)
        is Instruction.Ld16 -> load(instruction.target)
    }
}

private fun CPUInstructionScope.loadDecAhl() {
    val hl = readRegister16(Register16.HL)
    val memoryAtHl = readMemory(hl)
    writeRegister8(Register8.A, memoryAtHl)
    writeRegister16(Register16.HL, (hl.toInt() - 1).toUShort())
}

private fun CPUInstructionScope.loadIncAhl() {
    val hl = readRegister16(Register16.HL)
    val memoryAtHl = readMemory(hl)
    writeRegister8(Register8.A, memoryAtHl)
    writeRegister16(Register16.HL, (hl.toInt() + 1).toUShort())
}

private fun CPUInstructionScope.loadDecHla() {
    val hl = readRegister16(Register16.HL)
    val a = readRegister8(Register8.A)
    writeMemory(hl, a)
    writeRegister16(Register16.HL, (hl.toInt() - 1).toUShort())
}

private fun CPUInstructionScope.loadIncHla() {
    val hl = readRegister16(Register16.HL)
    val a = readRegister8(Register8.A)
    writeMemory(hl, a)
    writeRegister16(Register16.HL, (hl.toInt() + 1).toUShort())
}

private fun CPUInstructionScope.loadHlSpOffset() {
    writeRegister16(Register16.HL, addSignedToSp())
}

private fun CPUInstructionScope.loadSpHl() {
    val hl = readRegister16(Register16.HL)
    writeRegister16(Register16.SP, hl)
}

private fun CPUInstructionScope.loadMemoryAtData16Sp() {
    val address = readImmediate16()
    val sp = readRegister16(Register16.SP)
    writeMemory(address, (sp and 0x00FFu).toUByte())
    writeMemory((address + 1u).toUShort(), ((sp.toInt() shr 8) and 0x00FF).toUByte())
}

private fun CPUInstructionScope.load(source: Operand8, target: Operand8) {
    val value = readOperand8(source)
    writeOperand8(target, value)
}

private fun CPUInstructionScope.load(target: Register16) {
    val immediateValue = readImmediate16()
    writeRegister16(target, immediateValue)
}
