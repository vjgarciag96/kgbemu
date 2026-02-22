package com.vicgarci.kgbem.cpu

import com.vicgarci.kgbem.cpu.FlagsRegister.Companion.toFlagsRegister
import com.vicgarci.kgbem.cpu.FlagsRegister.Companion.toUByte
import com.vicgarci.kgbem.cpu.instruction.CPUInstructionScope
import com.vicgarci.kgbem.cpu.instruction.execute as executeInstruction
import kotlin.toUByte

class CPU(
    private val registers: Registers,
    private val programCounter: ProgramCounter,
    private val memoryBus: MemoryBus,
    private val stackPointer: StackPointer = StackPointer(),
) : CPUInstructionScope {

    private var globalInterruptEnabled = true
    private var enableGlobalInterruptPending = false
    private var halted = false

    fun step() {
        val pending = anyInterruptPending()
        if (globalInterruptEnabled && pending) {
            halted = false
            serviceInterrupt()
            return
        }

        if (halted) {
            if (pending) {
                halted = false
            }
            return
        }

        var instructionByte = memoryBus.readByte(programCounter.getAndIncrement())
        val prefixed = instructionByte == 0xCB.toUByte()
        if (prefixed) {
            instructionByte = memoryBus.readByte((programCounter.getAndIncrement()))
        }

        val instruction = InstructionDecoder.decode(instructionByte, prefixed)
            ?: error("Invalid instruction $instructionByte")
        execute(instruction)

        if (enableGlobalInterruptPending) {
            globalInterruptEnabled = true
            enableGlobalInterruptPending = false
        }
    }

    fun execute(instruction: Instruction) {
        executeInstruction(instruction)
    }

    private fun updateOperand(
        target: Operand8,
        update: (UByte) -> UByte,
    ) {
        when (target) {
            Register8.A -> registers.a = update(registers.a)
            Register8.B -> registers.b = update(registers.b)
            Register8.C -> registers.c = update(registers.c)
            Register8.D -> registers.d = update(registers.d)
            Register8.E -> registers.e = update(registers.e)
            Register8.H -> registers.h = update(registers.h)
            Register8.L -> registers.l = update(registers.l)
            MemoryAtHl -> memoryBus.writeByte(
                registers.hl,
                update(memoryBus.readByte(registers.hl))
            )

            is MemoryAtRegister16 -> {
                val address = getRegister16Value(target.register)
                memoryBus.writeByte(address, update(memoryBus.readByte(address)))
            }

            MemoryAtData16 -> {
                val address = readImmediate16()
                memoryBus.writeByte(address, update(memoryBus.readByte(address)))
            }

            MemoryAtHighData8 -> {
                val address = highAddress(readImmediate8())
                memoryBus.writeByte(address, update(memoryBus.readByte(address)))
            }

            MemoryAtHighC -> {
                val address = highAddress(registers.c)
                memoryBus.writeByte(address, update(memoryBus.readByte(address)))
            }

            Data8 -> error("Cannot update value of Data8 operand")
        }
    }

    private fun updateOperand(
        target: Register16,
        update: (UShort) -> UShort,
    ) {
        when (target) {
            Register16.BC -> registers.bc = update(registers.bc)
            Register16.DE -> registers.de = update(registers.de)
            Register16.HL -> registers.hl = update(registers.hl)
            Register16.AF -> registers.af = update(registers.af)
            Register16.SP -> stackPointer.setTo(update(stackPointer.get()))
        }
    }

    private fun getRegisterValue(
        target: Register8,
    ): UByte {
        return when (target) {
            Register8.A -> registers.a
            Register8.B -> registers.b
            Register8.C -> registers.c
            Register8.D -> registers.d
            Register8.E -> registers.e
            Register8.H -> registers.h
            Register8.L -> registers.l
        }
    }

    private fun getOperandValue(
        target: Operand8,
    ): UByte {
        return when (target) {
            is Register8 -> getRegisterValue(target)
            MemoryAtHl -> memoryBus.readByte(registers.hl)
            is MemoryAtRegister16 -> readMemoryAtRegister16(target.register)
            MemoryAtData16 -> readMemoryAtImmediate16()
            MemoryAtHighData8 -> readMemoryAtHighData8()
            MemoryAtHighC -> readMemoryAtHighC()
            Data8 -> readImmediate8()
        }
    }

    private fun getRegister16Value(
        target: Register16,
    ): UShort {
        return when (target) {
            Register16.AF -> registers.af
            Register16.BC -> registers.bc
            Register16.DE -> registers.de
            Register16.HL -> registers.hl
            Register16.SP -> stackPointer.get()
        }
    }

    override fun readImmediate8(): UByte {
        return memoryBus.readByte(programCounter.getAndIncrement())
    }

    override fun readImmediate16(): UShort {
        val leastSignificantByte = readImmediate8()
        val mostSignificantByte = readImmediate8()
        return ((mostSignificantByte.toInt() shl 8) or (leastSignificantByte.toInt())).toUShort()
    }

    private fun readMemoryAtRegister16(register: Register16): UByte {
        val address = getRegister16Value(register)
        return memoryBus.readByte(address)
    }

    private fun readMemoryAtImmediate16(): UByte {
        val address = readImmediate16()
        return memoryBus.readByte(address)
    }

    override fun addSignedToSp(): UShort {
        val offsetByte = readImmediate8()
        val offsetSigned = offsetByte.toByte().toInt()
        val spValue = stackPointer.get().toInt()
        val unsignedOffset = offsetByte.toInt()
        val halfCarry = ((spValue and 0xF) + (unsignedOffset and 0xF)) > 0xF
        val carry = ((spValue and 0xFF) + (unsignedOffset and 0xFF)) > 0xFF
        val result = (spValue + offsetSigned) and 0xFFFF

        registers.f = FlagsRegister(
            zero = false,
            subtract = false,
            halfCarry = halfCarry,
            carry = carry,
        ).toUByte()

        return result.toUShort()
    }

    private fun readMemoryAtHighData8(): UByte {
        val address = highAddress(readImmediate8())
        return memoryBus.readByte(address)
    }

    private fun readMemoryAtHighC(): UByte {
        val address = highAddress(registers.c)
        return memoryBus.readByte(address)
    }

    private fun highAddress(offset: UByte): UShort {
        return (0xFF00 + offset.toInt()).toUShort()
    }

    override fun shouldJump(condition: JumpCondition): Boolean {
        val flags = registers.f.toFlagsRegister()
        return when (condition) {
            JumpCondition.NOT_ZERO -> !flags.zero
            JumpCondition.ZERO -> flags.zero
            JumpCondition.CARRY -> flags.carry
            JumpCondition.NOT_CARRY -> !flags.carry
            JumpCondition.ALWAYS -> true
        }
    }

    private fun anyInterruptPending(): Boolean {
        return memoryBus.anyInterruptPending()
    }

    private fun serviceInterrupt() {
        val pending = memoryBus.interruptPendingMask
        val highestPriorityInterrupt = pending.countTrailingZeroBits()
        globalInterruptEnabled = false
        memoryBus.setInterruptFlagBit(highestPriorityInterrupt, false)
        pushProgramCounterToStack()
        programCounter.setTo(
            when (highestPriorityInterrupt) {
                0 -> 0x40.toUShort() // V-Blank
                1 -> 0x48.toUShort() // LCD STAT
                2 -> 0x50.toUShort() // Timer
                3 -> 0x58.toUShort() // Serial
                4 -> 0x60.toUShort() // Joypad
                else -> error("Invalid interrupt bit: $highestPriorityInterrupt")
            }
        )
    }

    override fun pushProgramCounterToStack() {
        val pc = programCounter.get()
        memoryBus.writeByte(
            stackPointer.decrementAndGet(),
            ((pc.toInt() and 0xFF00) ushr 8).toUByte()
        )
        memoryBus.writeByte(stackPointer.decrementAndGet(), (pc.toInt() and 0x00FF).toUByte())
    }

    override fun returnFromSubroutine() {
        val leastSignificantByte = memoryBus.readByte(stackPointer.getAndIncrement())
        val mostSignificantByte = memoryBus.readByte(stackPointer.getAndIncrement())
        val addressHigh = mostSignificantByte.toInt() shl 8
        val addressLow = leastSignificantByte.toInt()
        val returnAddress = (addressHigh or addressLow).toUShort()
        programCounter.setTo(returnAddress)
    }

    override fun writeRegister8(register: Register8, value: UByte) {
        updateOperand(register) { value }
    }

    override fun writeRegister16(
        register: Register16,
        value: UShort
    ) {
        updateOperand(register) { value }
    }

    override fun readRegister8(register: Register8): UByte {
        return getRegisterValue(register)
    }

    override fun readRegister16(register: Register16): UShort {
        return getRegister16Value(register)
    }

    override fun readMemory(address: UShort): UByte {
        return memoryBus.readByte(address)
    }

    override fun writeMemory(address: UShort, value: UByte) {
        memoryBus.writeByte(address, value)
    }

    override fun readAccumulator(): UByte {
        return registers.a
    }

    override fun writeAccumulator(value: UByte) {
        registers.a = value
    }

    override fun readFlagsRegister(): UByte {
        return registers.f
    }

    override fun writeFlagsRegister(value: UByte) {
        registers.f = value
    }

    override fun writeProgramCounter(address: UShort) {
        programCounter.setTo(address)
    }

    override fun increaseProgramCounter(stepSize: Int) {
        programCounter.increaseBy(stepSize)
    }

    override fun scheduleEnableGlobalInterrupt() {
        enableGlobalInterruptPending = true
    }

    override fun disableGlobalInterrupts() {
        globalInterruptEnabled = false
        enableGlobalInterruptPending = false
    }

    override fun haltCpu() {
        halted = true
    }
}

interface ImmutableCPU {
    fun step()
}
