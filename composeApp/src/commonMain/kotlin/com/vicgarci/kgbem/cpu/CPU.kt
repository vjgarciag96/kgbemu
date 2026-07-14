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
    private var lastBranchTaken = false

    fun step(): Int {
        val pending = anyInterruptPending()
        if (globalInterruptEnabled && pending) {
            halted = false
            serviceInterrupt()
            return INTERRUPT_SERVICE_CYCLES
        }

        if (halted) {
            if (pending) {
                halted = false
            }
            return NOP_CYCLES
        }

        var instructionByte = memoryBus.readByte(programCounter.getAndIncrement())
        val prefixed = instructionByte == 0xCB.toUByte()
        if (prefixed) {
            instructionByte = memoryBus.readByte((programCounter.getAndIncrement()))
        }

        val instruction = InstructionDecoder.decode(instructionByte, prefixed)
            ?: error("Invalid instruction $instructionByte")
        lastBranchTaken = false
        execute(instruction)

        if (enableGlobalInterruptPending) {
            globalInterruptEnabled = true
            enableGlobalInterruptPending = false
        }

        return cyclesFor(instruction, lastBranchTaken)
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

    companion object {
        private const val NOP_CYCLES = 4
        private const val INTERRUPT_SERVICE_CYCLES = 20

        private fun cyclesFor(
            instruction: Instruction,
            branchTaken: Boolean,
        ): Int = when (instruction) {
            // -- Arithmetic / Logic 8-bit --
            is Instruction.Add,
            is Instruction.AddC,
            is Instruction.Sub,
            is Instruction.Sbc,
            is Instruction.And,
            is Instruction.Or,
            is Instruction.Xor,
            is Instruction.Cp -> arithmeticLogic8Cycles(instruction)

            // -- Arithmetic 16-bit --
            is Instruction.AddHl -> 8
            Instruction.AddSp -> 16

            // -- INC / DEC --
            is Instruction.Inc -> incDecCycles(instruction.target)
            is Instruction.Dec -> incDecCycles(instruction.target)

            // -- Flags --
            Instruction.Ccf -> 4
            Instruction.Scf -> 4
            Instruction.Cpl -> 4
            Instruction.Daa -> 4

            // -- Bit operations (unprefixed A rotates) --
            Instruction.Rra -> 4
            Instruction.Rla -> 4
            Instruction.Rrca -> 4
            Instruction.Rlca -> 4

            // -- CB-prefixed bit operations --
            is Instruction.Rl,
            is Instruction.Rlc,
            is Instruction.Rr,
            is Instruction.Rrc,
            is Instruction.Sla,
            is Instruction.Sra,
            is Instruction.Srl,
            is Instruction.Swap,
            is Instruction.Bit,
            is Instruction.Res,
            is Instruction.Set -> cbPrefixedCycles(instruction)

            // -- Loads --
            is Instruction.Ld8 -> ld8Cycles(instruction)
            is Instruction.Ld16 -> 12
            Instruction.LdIncHLA -> 8
            Instruction.LdIncAHL -> 8
            Instruction.LdDecHLA -> 8
            Instruction.LdDecAHL -> 8
            Instruction.LdHlSpOffset -> 12
            Instruction.LdSpHl -> 8
            Instruction.LdMemoryAtData16Sp -> 20

            // -- Stack --
            is Instruction.Push -> 16
            is Instruction.Pop -> 12

            // -- Control flow --
            is Instruction.Jp -> if (branchTaken) 16 else 12
            Instruction.JpHl -> 4
            is Instruction.Jr -> if (branchTaken) 12 else 8
            is Instruction.Call -> if (branchTaken) 24 else 12
            is Instruction.Ret -> if (instruction.condition == JumpCondition.ALWAYS) {
                16
            } else {
                if (branchTaken) 20 else 8
            }
            Instruction.RetI -> 16
            is Instruction.Rst -> 16

            // -- CPU control --
            Instruction.Nop -> 4
            Instruction.Halt -> 4
            Instruction.Stop -> 4
            Instruction.DisableInterrupts -> 4
            Instruction.EnableInterrupts -> 4
        }

        private fun arithmeticLogic8Cycles(instruction: Instruction): Int {
            val target = when (instruction) {
                is Instruction.Add -> instruction.target
                is Instruction.AddC -> instruction.target
                is Instruction.Sub -> instruction.target
                is Instruction.Sbc -> instruction.target
                is Instruction.And -> instruction.target
                is Instruction.Or -> instruction.target
                is Instruction.Xor -> instruction.target
                is Instruction.Cp -> instruction.target
                else -> return 4
            }
            return when (target) {
                is Register8 -> 4
                MemoryAtHl -> 8
                Data8 -> 8
                is MemoryAtRegister16 -> 8
                MemoryAtData16 -> 16
                MemoryAtHighData8 -> 12
                MemoryAtHighC -> 8
            }
        }

        private fun incDecCycles(target: Register): Int = when (target) {
            is Register8 -> 4
            is Register16 -> 8
        }

        private fun cbPrefixedCycles(instruction: Instruction): Int {
            val target = when (instruction) {
                is Instruction.Rl -> instruction.target
                is Instruction.Rlc -> instruction.target
                is Instruction.Rr -> instruction.target
                is Instruction.Rrc -> instruction.target
                is Instruction.Sla -> instruction.target
                is Instruction.Sra -> instruction.target
                is Instruction.Srl -> instruction.target
                is Instruction.Swap -> instruction.target
                is Instruction.Bit -> instruction.target
                is Instruction.Res -> instruction.target
                is Instruction.Set -> instruction.target
                else -> return 8
            }
            return if (target == MemoryAtHl) 16 else 8
        }

        private fun ld8Cycles(instruction: Instruction.Ld8): Int {
            val source = instruction.source
            val target = instruction.target
            return when {
                // LD (nn), A or LD A, (nn)
                source is Register8 && target == MemoryAtData16 -> 16
                source == MemoryAtData16 && target is Register8 -> 16
                // LDH (n), A or LDH A, (n)
                source is Register8 && target == MemoryAtHighData8 -> 12
                source == MemoryAtHighData8 && target is Register8 -> 12
                // LD (C), A or LD A, (C)
                source is Register8 && target == MemoryAtHighC -> 8
                source == MemoryAtHighC && target is Register8 -> 8
                // LD r, (rr) or LD (rr), r
                source is Register8 && target is MemoryAtRegister16 -> 8
                source is MemoryAtRegister16 && target is Register8 -> 8
                // LD r, (HL) or LD (HL), r
                source is Register8 && target == MemoryAtHl -> 8
                source == MemoryAtHl && target is Register8 -> 8
                // LD r, n (8-bit immediate)
                source == Data8 && target is Register8 -> 8
                // LD (HL), n
                source == Data8 && target == MemoryAtHl -> 12
                // LD r, r
                source is Register8 && target is Register8 -> 4
                else -> 4
            }
        }
    }
}

interface ImmutableCPU {
    fun step(): Int
}
