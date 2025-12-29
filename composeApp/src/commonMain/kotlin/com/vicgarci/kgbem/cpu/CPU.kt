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

    private fun add(target: Register8) {
        val targetValue = getRegisterValue(target)
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

    private fun addHl(target: Register8) {
        val targetValue = getRegisterValue(target)
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
        target: Register8
    ) {
        val targetValue = getRegisterValue(target)
        val carry = if (registers.f.toFlagsRegister().carry) 0x01.toUByte() else 0x00.toUByte()
        val (sum, carryOut, halfCarry) = overflowAdd(registers.a, targetValue, carry)

        registers.a = sum.toUByte()
        val flags = FlagsRegister(
            zero = sum == 0.toUShort(),
            subtract = false,
            halfCarry = halfCarry,
            carry = carryOut,
        )
        registers.f = flags.toUByte()
    }

    private fun sub(target: Register8) {
        val targetValue = getRegisterValue(target)
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

    private fun sbc(target: Register8) {
        val targetValue = getRegisterValue(target)
        val carry = if (registers.f.toFlagsRegister().carry) 0x01.toUByte() else 0x00.toUByte()
        val (result, halfBorrow, borrow) = sub(registers.a, targetValue, carry)

        registers.a = result.toUByte()
        val flags = FlagsRegister(
            zero = result == 0.toUShort(),
            subtract = true,
            halfCarry = halfBorrow,
            carry = borrow,
        )
        registers.f = flags.toUByte()
    }

    private fun and(target: Register8) {
        val targetValue = getRegisterValue(target)
        registers.a = registers.a and targetValue
    }

    private fun or(target: Register8) {
        val targetValue = getRegisterValue(target)
        registers.a = registers.a or targetValue
    }

    private fun xor(target: Register8) {
        val targetValue = getRegisterValue(target)
        registers.a = registers.a xor targetValue
    }

    private fun cp(target: Register8) {
        val targetValue = getRegisterValue(target)

        val (sub, halfBorrow, borrow) = sub(registers.a, targetValue)
        val flags = FlagsRegister(
            zero = sub == 0.toUShort(),
            subtract = true,
            halfCarry = halfBorrow,
            carry = borrow,
        )
        registers.f = flags.toUByte()
    }

    private fun inc(target: OpDestination) {
        when (target) {
            is Register16 -> inc(target)
            is Register8 -> inc(target)
        }
    }

    private fun inc(target: Register8) {
        updateRegister(target) { register ->
            val (sum, _, halfCarry) = overflowAdd(register, 0x1.toUByte())

            val flags = registers.f.toFlagsRegister().copy(
                zero = sum == 0.toUShort(),
                subtract = false,
                halfCarry = halfCarry,
            )
            registers.f = flags.toUByte()

            sum.toUByte()
        }
    }

    private fun inc(target: Register16) {
        updateRegister(target) { value ->
            (value.toInt() + 1).toUShort()
        }
    }

    private fun dec(target: OpDestination) {
        when (target) {
            is Register16 -> dec(target)
            is Register8 -> dec(target)
        }
    }

    private fun dec(target: Register8) {
        updateRegister(target) { register ->
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

    private fun dec(target: Register16) {
        updateRegister(target) { value ->
            (value.toInt() - 1).toUShort()
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

    private fun rr(target: Register8) {
        updateRegister(target) { targetValue ->
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

    private fun rrc(target: Register8) {
        updateRegister(target) { targetValue ->
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

    private fun rl(target: Register8) {
        updateRegister(target) { targetValue ->
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

    private fun rlc(target: Register8) {
        updateRegister(target) { targetValue ->
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
        target: Register8,
    ) {
        val targetValue = getRegisterValue(target)
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
        target: Register8,
    ) {
        updateRegister(target) { targetValue ->
            val mask = (0b1 shl index).toUByte().inv()
            targetValue and mask
        }
    }

    private fun set(
        index: Int,
        target: Register8,
    ) {
        updateRegister(target) { targetValue ->
            val mask = (0b1 shl index).toUByte()
            targetValue or mask
        }
    }

    private fun srl(
        target: Register8,
    ) {
        updateRegister(target) { targetValue ->
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
        target: Register8,
    ) {
        updateRegister(target) { targetValue ->
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
        target: Register8,
    ) {
        updateRegister(target) { targetValue ->
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
        target: Register8,
    ) {
        updateRegister(target) { targetValue ->
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

    private fun load(target: Register8) {
        val byteToLoad = memoryBus.readByte(programCounter.getAndIncrement())
        updateRegister(target) { byteToLoad }
    }

    private fun pop(target: Register16) {
        val leastSignificantByte = memoryBus.readByte(stackPointer.getAndIncrement())
        val mostSignificantByte = memoryBus.readByte(stackPointer.getAndIncrement())

        when (target) {
            Register16.BC -> {
                registers.b = mostSignificantByte
                registers.c = leastSignificantByte
            }
            Register16.DE -> {
                registers.d = mostSignificantByte
                registers.e = leastSignificantByte
            }
            Register16.HL -> {
                registers.h = mostSignificantByte
                registers.l = leastSignificantByte
            }
            Register16.AF -> {
                registers.a = mostSignificantByte
                registers.f = leastSignificantByte and 0xF0.toUByte() // lower nibble of F is always 0
            }
            else -> error("Invalid pop target: $target")
        }
    }

    private fun push(target: Register16) {
        when (target) {
            Register16.BC -> {
                memoryBus.writeByte(stackPointer.decrementAndGet(), registers.b)
                memoryBus.writeByte(stackPointer.decrementAndGet(), registers.c)
            }
            Register16.DE -> {
                memoryBus.writeByte(stackPointer.decrementAndGet(), registers.d)
                memoryBus.writeByte(stackPointer.decrementAndGet(), registers.e)
            }
            Register16.HL -> {
                memoryBus.writeByte(stackPointer.decrementAndGet(), registers.h)
                memoryBus.writeByte(stackPointer.decrementAndGet(), registers.l)
            }
            Register16.AF -> {
                memoryBus.writeByte(stackPointer.decrementAndGet(), registers.a)
                memoryBus.writeByte(stackPointer.decrementAndGet(), registers.f)
            }
            else -> error("Invalid push target: $target")
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

    private fun updateRegister(
        target: Register8,
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
        }
    }

    private fun updateRegister(
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