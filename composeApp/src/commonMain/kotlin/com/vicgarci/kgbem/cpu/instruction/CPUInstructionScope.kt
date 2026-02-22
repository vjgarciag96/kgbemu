package com.vicgarci.kgbem.cpu.instruction

import com.vicgarci.kgbem.cpu.Data8
import com.vicgarci.kgbem.cpu.MemoryAtData16
import com.vicgarci.kgbem.cpu.MemoryAtHighC
import com.vicgarci.kgbem.cpu.MemoryAtHighData8
import com.vicgarci.kgbem.cpu.MemoryAtHl
import com.vicgarci.kgbem.cpu.MemoryAtRegister16
import com.vicgarci.kgbem.cpu.Operand8
import com.vicgarci.kgbem.cpu.JumpCondition
import com.vicgarci.kgbem.cpu.Register16
import com.vicgarci.kgbem.cpu.Register8

internal interface CPUInstructionScope {
    fun writeRegister8(register: Register8, value: UByte)
    fun writeRegister16(register: Register16, value: UShort)

    fun readRegister8(register: Register8): UByte
    fun readRegister16(register: Register16): UShort

    fun readMemory(address: UShort): UByte
    fun writeMemory(address: UShort, value: UByte)

    fun readAccumulator(): UByte
    fun writeAccumulator(value: UByte)
    fun readFlagsRegister(): UByte
    fun writeFlagsRegister(value: UByte)

    fun readImmediate8(): UByte
    fun readImmediate16(): UShort

    fun addSignedToSp(): UShort

    fun pushProgramCounterToStack()
    fun returnFromSubroutine()
    fun writeProgramCounter(address: UShort)
    fun increaseProgramCounter(stepSize: Int)
    fun shouldJump(condition: JumpCondition): Boolean
    fun scheduleEnableGlobalInterrupt()
    fun disableGlobalInterrupts()
    fun haltCpu()
}

internal fun CPUInstructionScope.readOperand8(operand: Operand8): UByte {
    return when (operand) {
        Data8 -> readImmediate8()
        MemoryAtData16 -> {
            val address = readImmediate16()
            readMemory(address)
        }
        MemoryAtHighC -> {
            val address = (0xFF00 + readRegister8(Register8.C).toInt()).toUShort()
            readMemory(address)
        }
        MemoryAtHighData8 -> {
            val address = (0xFF00 + readImmediate8().toInt()).toUShort()
            readMemory(address)
        }
        MemoryAtHl -> {
            val hl = readRegister16(Register16.HL)
            readMemory(hl)
        }
        is MemoryAtRegister16 -> {
            val address = readRegister16(operand.register)
            readMemory(address)
        }
        is Register8 -> readRegister8(operand)
    }
}

internal fun CPUInstructionScope.writeOperand8(operand: Operand8, value: UByte) {
    when (operand) {
        Data8 -> error("Cannot write to Data8 operand")
        MemoryAtData16 -> {
            val address = readImmediate16()
            writeMemory(address, value)
        }
        MemoryAtHighC -> {
            val address = (0xFF00 + readRegister8(Register8.C).toInt()).toUShort()
            writeMemory(address, value)
        }
        MemoryAtHighData8 -> {
            val address = (0xFF00 + readImmediate8().toInt()).toUShort()
            writeMemory(address, value)
        }
        MemoryAtHl -> {
            val hl = readRegister16(Register16.HL)
            writeMemory(hl, value)
        }
        is MemoryAtRegister16 -> {
            val address = readRegister16(operand.register)
            writeMemory(address, value)
        }
        is Register8 -> writeRegister8(operand, value)
    }
}
