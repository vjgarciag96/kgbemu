package com.vicgarci.kgbem.cpu

import com.vicgarci.kgbem.cpu.FlagsRegister.Companion.toFlagsRegister
import com.vicgarci.kgbem.cpu.FlagsRegister.Companion.toUByte
import com.vicgarci.kgbem.cpu.instruction.CPUInstructionScope
import com.vicgarci.kgbem.cpu.instruction.execute as executeInstruction
import kotlin.toUByte

class CPU(
    private val registers: Registers,
    private val programCounter: ProgramCounter,
    private val bus: Bus,
    private val stackPointer: StackPointer = StackPointer(),
) : CPUInstructionScope {

    private var globalInterruptEnabled = true
    private var enableGlobalInterruptPending = false
    private var halted = false
    private var lastBranchTaken = false

    fun step(): Int {
        val pending = anyInterruptPending()
        if (globalInterruptEnabled && pending) {
            halted = false
            serviceInterrupt()
            return 20
        }

        if (halted) {
            if (pending) halted = false
            return 4
        }

        var instructionByte = bus.readByte(programCounter.getAndIncrement())
        val prefixed = instructionByte == 0xCB.toUByte()
        if (prefixed) {
            instructionByte = bus.readByte(programCounter.getAndIncrement())
        }

        val instruction = InstructionDecoder.decode(instructionByte, prefixed)
            ?: error("Invalid instruction 0x${instructionByte.toString(16).padStart(2, '0')}")

        lastBranchTaken = false
        execute(instruction)

        if (enableGlobalInterruptPending) {
            globalInterruptEnabled = true
            enableGlobalInterruptPending = false
        }

        return instruction.cycleCost(lastBranchTaken)
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
            MemoryAtHl -> bus.writeByte(
                registers.hl,
                update(bus.readByte(registers.hl))
            )

            is MemoryAtRegister16 -> {
                val address = getRegister16Value(target.register)
                bus.writeByte(address, update(bus.readByte(address)))
            }

            MemoryAtData16 -> {
                val address = readImmediate16()
                bus.writeByte(address, update(bus.readByte(address)))
            }

            MemoryAtHighData8 -> {
                val address = highAddress(readImmediate8())
                bus.writeByte(address, update(bus.readByte(address)))
            }

            MemoryAtHighC -> {
                val address = highAddress(registers.c)
                bus.writeByte(address, update(bus.readByte(address)))
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
            MemoryAtHl -> bus.readByte(registers.hl)
            is MemoryAtRegister16 -> bus.readByte(getRegister16Value(target.register))
            MemoryAtData16 -> bus.readByte(readImmediate16())
            MemoryAtHighData8 -> bus.readByte(highAddress(readImmediate8()))
            MemoryAtHighC -> bus.readByte(highAddress(registers.c))
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
        return bus.readByte(programCounter.getAndIncrement())
    }

    override fun readImmediate16(): UShort {
        val leastSignificantByte = readImmediate8()
        val mostSignificantByte = readImmediate8()
        return ((mostSignificantByte.toInt() shl 8) or (leastSignificantByte.toInt())).toUShort()
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

    private fun highAddress(offset: UByte): UShort = (0xFF00 + offset.toInt()).toUShort()

    override fun shouldJump(condition: JumpCondition): Boolean {
        val flags = registers.f.toFlagsRegister()
        val result = when (condition) {
            JumpCondition.NOT_ZERO -> !flags.zero
            JumpCondition.ZERO -> flags.zero
            JumpCondition.CARRY -> flags.carry
            JumpCondition.NOT_CARRY -> !flags.carry
            JumpCondition.ALWAYS -> true
        }
        lastBranchTaken = result
        return result
    }

    private fun anyInterruptPending(): Boolean = bus.anyInterruptPending()

    private fun serviceInterrupt() {
        val pending = bus.interruptPendingMask
        val highestPriorityInterrupt = pending.countTrailingZeroBits()
        globalInterruptEnabled = false
        bus.setInterruptFlagBit(highestPriorityInterrupt, false)
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
        bus.writeByte(stackPointer.decrementAndGet(), ((pc.toInt() and 0xFF00) ushr 8).toUByte())
        bus.writeByte(stackPointer.decrementAndGet(), (pc.toInt() and 0x00FF).toUByte())
    }

    override fun returnFromSubroutine() {
        val leastSignificantByte = bus.readByte(stackPointer.getAndIncrement())
        val mostSignificantByte = bus.readByte(stackPointer.getAndIncrement())
        val returnAddress = ((mostSignificantByte.toInt() shl 8) or leastSignificantByte.toInt()).toUShort()
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

    override fun readMemory(address: UShort): UByte = bus.readByte(address)

    override fun writeMemory(address: UShort, value: UByte) = bus.writeByte(address, value)

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
    fun step(): Int
}
