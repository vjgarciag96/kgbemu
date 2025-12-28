package com.vicgarci.kgbem.cpu

import com.vicgarci.kgbem.cpu.FlagsRegister.Companion.toFlagsRegister
import com.vicgarci.kgbem.cpu.FlagsRegister.Companion.toUByte
import kotlin.toUByte

class CPU(
    private val registers: Registers,
    private val programCounter: ProgramCounter,
    private val memoryBus: MemoryBus,
    private val stackPointer: StackPointer = StackPointer(),
) {

    fun step() {
        var instructionByte = memoryBus.readByte(programCounter.getAndIncrement())
        val prefixed = instructionByte == 0xCB.toUByte()
        if (prefixed) {
            instructionByte = memoryBus.readByte((programCounter.getAndIncrement()))
        }

        val address = when (val instruction = InstructionDecoder.decode(instructionByte, prefixed)) {
            is Instruction -> execute(instruction)
            null -> error("Invalid instruction $instructionByte")
        }

        if (address != null) {
            programCounter.setTo(address)
        }
    }

    fun execute(instruction: Instruction): UShort? {
        when (instruction) {
            is Instruction.Add -> add(instruction.target)
            is Instruction.AddHl -> addHl(instruction.target)
            is Instruction.AddC -> addC(instruction.target)
            is Instruction.Sub -> sub(instruction.target)
            is Instruction.Sbc -> sbc(instruction.target)
            is Instruction.And -> and(instruction.target)
            is Instruction.Or -> or(instruction.target)
            is Instruction.Xor -> xor(instruction.target)
            is Instruction.Cp -> cp(instruction.target)
            is Instruction.Inc -> inc(instruction.target)
            is Instruction.Dec -> dec(instruction.target)
            Instruction.Ccf -> ccf()
            Instruction.Scf -> scf()
            Instruction.Rra -> rra()
            is Instruction.Rr -> rr(instruction.target)
            is Instruction.Rrc -> rrc(instruction.target)
            Instruction.Rla -> rla()
            is Instruction.Rl -> rl(instruction.target)
            is Instruction.Rlc -> rlc(instruction.target)
            Instruction.Rrca -> rrca()
            Instruction.Rlca -> rlca()
            Instruction.Cpl -> cpl()
            is Instruction.Bit -> bit(instruction.index, instruction.target)
            is Instruction.Res -> res(instruction.index, instruction.target)
            is Instruction.Set -> set(instruction.index, instruction.target)
            is Instruction.Srl -> srl(instruction.target)
            is Instruction.Sra -> sra(instruction.target)
            is Instruction.Sla -> sla(instruction.target)
            is Instruction.Swap -> swap(instruction.target)
            is Instruction.Jp -> return jump(instruction.condition)
            is Instruction.Ld -> load(instruction.target)
            is Instruction.Pop -> pop(instruction.target)
            is Instruction.Push -> push(instruction.target)
            is Instruction.Call -> return call(instruction.condition)
            is Instruction.Ret -> return ret(instruction.condition)
            is Instruction.Jr -> jumpRelative(instruction.condition)
            is Instruction.Rst -> rst(instruction.address)
            Instruction.Nop -> Unit
        }

        return null
    }

    private fun add(target: ArithmeticTarget) {
        val targetValue = getArithmeticTargetValue(target)
        val (sum, carry, halfCarry) = overflowAdd(
            registers.a,
            targetValue,
        )
        registers.a = sum.toUByte()
        val flags = FlagsRegister(
            zero = sum == 0.toUShort(),
            subtract = false,
            halfCarry = halfCarry,
            carry = carry,
        )
        registers.f = flags.toUByte()
    }

    private fun addHl(target: ArithmeticTarget) {
        val targetValue = getArithmeticTargetValue(target)
        val (sum, carry, halfCarry) = overflowAdd(
            registers.hl,
            targetValue.toUShort(),
        )

        registers.hl = sum
        val flags = FlagsRegister(
            zero = sum == 0.toUShort(),
            subtract = false,
            halfCarry = halfCarry,
            carry = carry,
        )
        registers.f = flags.toUByte()
    }

    private fun addC(
        target: ArithmeticTarget
    ) {
        val targetValue = getArithmeticTargetValue(target)
        val carryToAdd = if (registers.f.toFlagsRegister().carry) 0b1.toUByte() else 0b0.toUByte()
        val (sum, carry, halfCarry) = overflowAdd(
            registers.a.toUShort(),
            (targetValue + carryToAdd).toUShort(),
        )

        registers.a = sum.toUByte()
        val flags = FlagsRegister(
            zero = sum == 0.toUShort(),
            subtract = false,
            halfCarry = halfCarry,
            carry = carry,
        )
        registers.f = flags.toUByte()
    }

    private fun sub(target: ArithmeticTarget) {
        val targetValue = getArithmeticTargetValue(target)
        val (sub, halfBorrow, borrow) = sub(registers.a, targetValue)

        registers.a = sub.toUByte()
        val flags = FlagsRegister(
            zero = sub == 0.toUShort(),
            subtract = true,
            halfCarry = halfBorrow,
            carry = borrow,
        )
        registers.f = flags.toUByte()
    }

    private fun sbc(target: ArithmeticTarget) {
        val targetValue = getArithmeticTargetValue(target)
        val carryToAdd = if (registers.f.toFlagsRegister().carry) 0b1.toUByte() else 0b0.toUByte()

        val (sub, halfBorrow, borrow) = sub(
            registers.a.toUShort(),
            (targetValue + carryToAdd).toUShort(),
        )

        registers.a = sub.toUByte()
        val flags = FlagsRegister(
            zero = sub == 0.toUShort(),
            subtract = true,
            halfCarry = halfBorrow,
            carry = borrow,
        )
        registers.f = flags.toUByte()
    }

    private fun and(target: ArithmeticTarget) {
        val targetValue = getArithmeticTargetValue(target)
        registers.a = registers.a and targetValue
    }

    private fun or(target: ArithmeticTarget) {
        val targetValue = getArithmeticTargetValue(target)
        registers.a = registers.a or targetValue
    }

    private fun xor(target: ArithmeticTarget) {
        val targetValue = getArithmeticTargetValue(target)
        registers.a = registers.a xor targetValue
    }

    private fun cp(target: ArithmeticTarget) {
        val targetValue = getArithmeticTargetValue(target)

        val (sub, halfBorrow, borrow) = sub(registers.a, targetValue)
        val flags = FlagsRegister(
            zero = sub == 0.toUShort(),
            subtract = true,
            halfCarry = halfBorrow,
            carry = borrow,
        )
        registers.f = flags.toUByte()
    }

    private fun inc(target: ArithmeticTarget) {
        updateArithmeticTarget(target) { register ->
            val (sum, _, halfCarry) = overflowAdd(register, 0x1.toUByte())

            val flags = registers.f.toFlagsRegister().copy(
                zero = sum == 0x0.toUShort(),
                subtract = false,
                halfCarry = halfCarry,
            )
            registers.f = flags.toUByte()

            sum.toUByte()
        }
    }

    private fun dec(target: ArithmeticTarget) {
        updateArithmeticTarget(target) { register ->
            val (sub, halfBorrow, _) = sub(register, 0x1.toUByte())

            val flags = registers.f.toFlagsRegister().copy(
                zero = sub == 0x0.toUShort(),
                subtract = true,
                halfCarry = halfBorrow,
            )
            registers.f = flags.toUByte()

            sub.toUByte()
        }
    }

    private fun ccf() {
        val flags = registers.f.toFlagsRegister()
        registers.f = flags.copy(
            subtract = false,
            halfCarry = false,
            carry = !flags.carry,
        ).toUByte()
    }

    private fun scf() {
        val flags = registers.f.toFlagsRegister()
        registers.f = flags.copy(
            subtract = false,
            halfCarry = false,
            carry = true,
        ).toUByte()
    }

    private fun rra() {
        val leastSignificantBit = registers.a and 0b1.toUByte()
        val flags = registers.f.toFlagsRegister()
        val carryBit = if (flags.carry) 0b1 else 0b0
        val rotatedA = registers.a.toInt() ushr 1
        val rotatedCarry = carryBit shl 7
        registers.a = ((rotatedA or rotatedCarry) and 0xFF).toUByte()
        registers.f = flags.copy(
            carry = leastSignificantBit == 0b1.toUByte(),
        ).toUByte()
    }

    private fun rr(target: ArithmeticTarget) {
        updateArithmeticTarget(target) { targetValue ->
            val leastSignificantBit = targetValue and 0b1.toUByte()
            val flags = registers.f.toFlagsRegister()
            val carryBit = if (flags.carry) 0b1 else 0b0
            val rotatedValue = targetValue.toInt() ushr 1
            val rotatedCarry = carryBit shl 7
            val result = ((rotatedValue or rotatedCarry) and 0xFF).toUByte()
            registers.f = FlagsRegister(
                zero = result == 0b0.toUByte(),
                subtract = false,
                halfCarry = false,
                carry = leastSignificantBit == 0b1.toUByte(),
            ).toUByte()

            result
        }
    }

    private fun rrc(target: ArithmeticTarget) {
        updateArithmeticTarget(target) { targetValue ->
            val leastSignificantBit = targetValue and 0b1.toUByte()
            val bitToWrapAround = leastSignificantBit.toInt() shl 7
            val rotatedValue = targetValue.toInt() ushr 1
            val result = ((rotatedValue or bitToWrapAround) and 0xFF).toUByte()
            registers.f = FlagsRegister(
                zero = result == 0b0.toUByte(),
                subtract = false,
                halfCarry = false,
                carry = leastSignificantBit == 0b1.toUByte(),
            ).toUByte()

            result
        }
    }

    private fun rla() {
        val mostSignificantBit = (registers.a and (0b1 shl 7).toUByte()).toInt() ushr 7
        val flags = registers.f.toFlagsRegister()
        val carryBit = if (flags.carry) 0b1 else 0b0
        val rotatedA = registers.a.toInt() shl 1
        registers.a = ((rotatedA or carryBit) and 0xFF).toUByte()
        registers.f = flags.copy(
            carry = (mostSignificantBit and 0xFF).toUByte() == 0b1.toUByte(),
        ).toUByte()
    }

    private fun rl(target: ArithmeticTarget) {
        updateArithmeticTarget(target) { targetValue ->
            val mostSignificantBit = (targetValue and (0b1 shl 7).toUByte()).toInt() ushr 7
            val flags = registers.f.toFlagsRegister()
            val carryBit = if (flags.carry) 0b1 else 0b0
            val rotatedValue = targetValue.toInt() shl 1
            val result = ((rotatedValue or carryBit) and 0xFF).toUByte()
            registers.f = FlagsRegister(
                zero = result == 0b0.toUByte(),
                subtract = false,
                halfCarry = false,
                carry = (mostSignificantBit and 0xFF).toUByte() == 0b1.toUByte(),
            ).toUByte()

            result
        }
    }

    private fun rlc(target: ArithmeticTarget) {
        updateArithmeticTarget(target) { targetValue ->
            val mostSignificantBit = (targetValue and (0b1 shl 7).toUByte()).toInt() ushr 7
            val rotatedValue = targetValue.toInt() shl 1
            val result = ((rotatedValue or mostSignificantBit) and 0xFF).toUByte()
            registers.f = FlagsRegister(
                zero = result == 0b0.toUByte(),
                subtract = false,
                halfCarry = false,
                carry = (mostSignificantBit and 0xFF).toUByte() == 0b1.toUByte(),
            ).toUByte()

            result
        }
    }

    private fun rrca() {
        val leastSignificantBit = registers.a and 0b1.toUByte()
        val flags = registers.f.toFlagsRegister()
        val rotatedA = (registers.a.toInt() ushr 1) or (leastSignificantBit.toInt() shl 7)
        registers.a = (rotatedA and 0xFF).toUByte()
        registers.f = flags.copy(
            carry = leastSignificantBit == 0b1.toUByte(),
        ).toUByte()
    }

    private fun rlca() {
        val mostSignificantBit = (registers.a and (0b1 shl 7).toUByte()).toInt() ushr 7
        val flags = registers.f.toFlagsRegister()
        val rotatedA = registers.a.toInt() shl 1
        registers.a = ((rotatedA or mostSignificantBit) and 0xFF).toUByte()
        registers.f = flags.copy(
            carry = (mostSignificantBit and 0xFF).toUByte() == 0b1.toUByte(),
        ).toUByte()

    }

    private fun cpl() {
        registers.a = registers.a.inv()
    }

    private fun bit(
        index: Int,
        target: ArithmeticTarget,
    ) {
        val targetValue = getArithmeticTargetValue(target)
        val bitSet = ((targetValue.toInt() ushr index).toUByte() and 0b1.toUByte()) == 0b1.toUByte()
        val flags = registers.f.toFlagsRegister()
        registers.f = flags.copy(
            zero = !bitSet,
            subtract = false,
            halfCarry = true,
        ).toUByte()
    }

    private fun res(
        index: Int,
        target: ArithmeticTarget,
    ) {
        updateArithmeticTarget(target) { targetValue ->
            val mask = (0b1 shl index).toUByte().inv()
            targetValue and mask
        }
    }

    private fun set(
        index: Int,
        target: ArithmeticTarget,
    ) {
        updateArithmeticTarget(target) { targetValue ->
            val mask = (0b1 shl index).toUByte()
            targetValue or mask
        }
    }

    private fun srl(
        target: ArithmeticTarget,
    ) {
        updateArithmeticTarget(target) { targetValue ->
            val leastSignificantBit = targetValue and 0b1.toUByte()
            val shiftedValue = ((targetValue.toInt() ushr 1) and 0xFF).toUByte()

            registers.f = registers.f.toFlagsRegister().copy(
                zero = shiftedValue == 0b0.toUByte(),
                carry = leastSignificantBit == 0b1.toUByte(),
            ).toUByte()

            shiftedValue
        }
    }

    private fun sra(
        target: ArithmeticTarget,
    ) {
        updateArithmeticTarget(target) { targetValue ->
            val leastSignificantBit = targetValue and 0b1.toUByte()
            val mostSignificantBit = targetValue and (0b1 shl 7).toUByte()
            val shiftedValue = ((targetValue.toInt() ushr 1) and 0xFF).toUByte() or mostSignificantBit

            registers.f = FlagsRegister(
                zero = shiftedValue == 0b0.toUByte(),
                subtract = false,
                halfCarry = false,
                carry = leastSignificantBit == 0b1.toUByte(),
            ).toUByte()

            shiftedValue
        }
    }

    private fun sla(
        target: ArithmeticTarget,
    ) {
        updateArithmeticTarget(target) { targetValue ->
            val mostSignificantBit = targetValue and (0b1 shl 7).toUByte()
            val shiftedValue = ((targetValue.toInt() shl 1) and 0xFF).toUByte()

            registers.f = FlagsRegister(
                zero = shiftedValue == 0b0.toUByte(),
                subtract = false,
                halfCarry = false,
                carry = ((mostSignificantBit.toInt() ushr 7) and 0xFF).toUByte() == 0b1.toUByte(),
            ).toUByte()

            shiftedValue
        }
    }

    private fun swap(
        target: ArithmeticTarget,
    ) {
        updateArithmeticTarget(target) { targetValue ->
            val upperNibble = targetValue and 0xF0.toUByte()
            val lowerNibble = targetValue and 0x0F.toUByte()

            val shiftedUpperNibble = ((upperNibble.toInt() ushr 4) and 0xFF).toUByte()
            val shiftedLowerNibble = ((lowerNibble.toInt() shl 4) and 0xFF).toUByte()

            val result = shiftedUpperNibble or shiftedLowerNibble

            registers.f = FlagsRegister(
                zero = result == 0b0.toUByte(),
                subtract = false,
                halfCarry = false,
                carry = false,
            ).toUByte()

            result
        }
    }

    private fun jump(condition: JumpCondition): UShort? {
        val jump = evaluateJumpCondition(condition)

        return if (jump) {
            val leastSignificantByte = memoryBus.readByte(programCounter.getAndIncrement())
            val mostSignificantByte = memoryBus.readByte(programCounter.getAndIncrement())
            ((mostSignificantByte.toInt() shl 8) or (leastSignificantByte.toInt())).toUShort()
        } else {
            // even if we don't need to jump, we need to "consume" the jump's 16 bit address
            programCounter.increaseBy(stepSize = 2)
            null
        }
    }

    private fun jumpRelative(condition: JumpCondition) {
        val jump = evaluateJumpCondition(condition)

        return if (jump) {
            val offset = memoryBus.readByte(programCounter.getAndIncrement())
            val signedOffset = offset.toByte().toInt()
            programCounter.increaseBy(stepSize = signedOffset)
        } else {
            // even if we don't need to jump, we need to "consume" the jump's 8 bit address
            programCounter.increaseBy(stepSize = 1)
        }
    }

    private fun load(target: ArithmeticTarget) {
        val byteToLoad = memoryBus.readByte(programCounter.getAndIncrement())
        updateArithmeticTarget(target) { byteToLoad }
    }

    private fun pop(target: StackTarget) {
        val leastSignificantByte = memoryBus.readByte(stackPointer.getAndIncrement())
        val mostSignificantByte = memoryBus.readByte(stackPointer.getAndIncrement())

        when (target) {
            StackTarget.BC -> {
                registers.b = mostSignificantByte
                registers.c = leastSignificantByte
            }
            StackTarget.DE -> {
                registers.d = mostSignificantByte
                registers.e = leastSignificantByte
            }
            StackTarget.HL -> {
                registers.h = mostSignificantByte
                registers.l = leastSignificantByte
            }
            StackTarget.AF -> {
                registers.a = mostSignificantByte
                registers.f = leastSignificantByte and 0xF0.toUByte() // lower nibble of F is always 0
            }
        }
    }

    private fun push(target: StackTarget) {
        when (target) {
            StackTarget.BC -> {
                memoryBus.writeByte(stackPointer.decrementAndGet(), registers.b)
                memoryBus.writeByte(stackPointer.decrementAndGet(), registers.c)
            }
            StackTarget.DE -> {
                memoryBus.writeByte(stackPointer.decrementAndGet(), registers.d)
                memoryBus.writeByte(stackPointer.decrementAndGet(), registers.e)
            }
            StackTarget.HL -> {
                memoryBus.writeByte(stackPointer.decrementAndGet(), registers.h)
                memoryBus.writeByte(stackPointer.decrementAndGet(), registers.l)
            }
            StackTarget.AF -> {
                memoryBus.writeByte(stackPointer.decrementAndGet(), registers.a)
                memoryBus.writeByte(stackPointer.decrementAndGet(), registers.f)
            }
        }
    }

    private fun call(condition: JumpCondition): UShort? {
        val call = evaluateJumpCondition(condition)

        return if (call) {
            // read address to call
            val leastSignificantByte = memoryBus.readByte(programCounter.getAndIncrement())
            val mostSignificantByte = memoryBus.readByte(programCounter.getAndIncrement())
            val address = ((mostSignificantByte.toInt() shl 8) or (leastSignificantByte.toInt())).toUShort()
            
            pushProgramCounterToStack()

            address
        } else {
            // even if we don't need to call, we need to "consume" the call's 16 bit address
            programCounter.increaseBy(stepSize = 2)
            null
        }
    }

    private fun ret(condition: JumpCondition): UShort? {
        val ret = evaluateJumpCondition(condition)

        return if (ret) {
            val leastSignificantByte = memoryBus.readByte(stackPointer.getAndIncrement())
            val mostSignificantByte = memoryBus.readByte(stackPointer.getAndIncrement())
            ((mostSignificantByte.toInt() shl 8) or (leastSignificantByte.toInt())).toUShort()
        } else {
            null
        }
    }

    private fun rst(address: UByte) {
        pushProgramCounterToStack()
        programCounter.setTo(address.toUShort())
    }

    private fun updateArithmeticTarget(
        target: ArithmeticTarget,
        update: (UByte) -> UByte,
    ) {
        when (target) {
            ArithmeticTarget.A -> registers.a = update(registers.a)
            ArithmeticTarget.B -> registers.b = update(registers.b)
            ArithmeticTarget.C -> registers.c = update(registers.c)
            ArithmeticTarget.D -> registers.d = update(registers.d)
            ArithmeticTarget.E -> registers.e = update(registers.e)
            ArithmeticTarget.H -> registers.h = update(registers.h)
            ArithmeticTarget.L -> registers.l = update(registers.l)
        }
    }

    private fun getArithmeticTargetValue(
        target: ArithmeticTarget,
    ): UByte {
        return when (target) {
            ArithmeticTarget.A -> registers.a
            ArithmeticTarget.B -> registers.b
            ArithmeticTarget.C -> registers.c
            ArithmeticTarget.D -> registers.d
            ArithmeticTarget.E -> registers.e
            ArithmeticTarget.H -> registers.h
            ArithmeticTarget.L -> registers.l
        }
    }

    private fun evaluateJumpCondition(condition: JumpCondition): Boolean {
        val flags = registers.f.toFlagsRegister()
        return when (condition) {
            JumpCondition.NOT_ZERO -> !flags.zero
            JumpCondition.ZERO -> flags.zero
            JumpCondition.CARRY -> flags.carry
            JumpCondition.NOT_CARRY -> !flags.carry
            JumpCondition.ALWAYS -> true
        }
    }

    private fun pushProgramCounterToStack() {
        val pc = programCounter.get()
        memoryBus.writeByte(stackPointer.decrementAndGet(), ((pc.toInt() and 0xFF00) ushr 8).toUByte())
        memoryBus.writeByte(stackPointer.decrementAndGet(), (pc.toInt() and 0x00FF).toUByte())
    }
}