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

        val address =
            when (val instruction = InstructionDecoder.decode(instructionByte, prefixed)) {
                is Instruction -> execute(instruction)
                null -> error("Invalid instruction $instructionByte")
            }

        if (address != null) {
            programCounter.setTo(address)
        }

        if (enableGlobalInterruptPending) {
            globalInterruptEnabled = true
            enableGlobalInterruptPending = false
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
            Instruction.JpHl -> return registers.hl
            is Instruction.Ld -> load(instruction.source, instruction.target)
            is Instruction.Pop -> pop(instruction.target)
            is Instruction.Push -> push(instruction.target)
            is Instruction.Call -> return call(instruction.condition)
            is Instruction.Ret -> ret(instruction.condition)
            is Instruction.Jr -> jumpRelative(instruction.condition)
            is Instruction.Rst -> rst(instruction.address)
            Instruction.LdDecAHL -> loadDecAhl()
            Instruction.LdIncAHL -> loadIncAhl()
            Instruction.LdDecHLA -> loadDecHla()
            Instruction.LdIncHLA -> loadIncHla()
            Instruction.Daa -> daa()
            Instruction.Halt -> halt()
            Instruction.Stop -> stop()
            Instruction.DisableInterrupts -> disableGlobalInterrupt()
            Instruction.EnableInterrupts -> enableGlobalInterrupt()
            Instruction.RetI -> returnAndEnableInterrupts()
            Instruction.AddSp -> addSp()
            Instruction.LdHlSpOffset -> loadHlSpOffset()
            Instruction.LdSpHl -> loadSpHl()
            Instruction.LdMemoryAtData16Sp -> storeSpAtImmediate()
            Instruction.Nop -> Unit
        }

        return null
    }

    private fun add(target: Operand8) {
        val targetValue = getOperandValue(target)
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

    private fun addHl(target: Register16) {
        val targetValue = getRegister16Value(target)
        val (sum, carry, halfCarry) = overflowAdd(
            registers.hl,
            targetValue,
        )

        registers.hl = sum
        val flags = registers.f.toFlagsRegister().copy(
            subtract = false,
            halfCarry = halfCarry,
            carry = carry,
        )
        registers.f = flags.toUByte()
    }

    private fun addC(
        target: Operand8
    ) {
        val targetValue = getOperandValue(target)
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

    private fun sub(target: Operand8) {
        val targetValue = getOperandValue(target)
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

    private fun sbc(target: Operand8) {
        val targetValue = getOperandValue(target)
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

    private fun and(target: Operand8) {
        val targetValue = getOperandValue(target)
        registers.a = registers.a and targetValue
    }

    private fun or(target: Operand8) {
        val targetValue = getOperandValue(target)
        registers.a = registers.a or targetValue
    }

    private fun xor(target: Operand8) {
        val targetValue = getOperandValue(target)
        registers.a = registers.a xor targetValue
    }

    private fun cp(target: Operand8) {
        val targetValue = getOperandValue(target)

        val (sub, halfBorrow, borrow) = sub(registers.a, targetValue)
        val flags = FlagsRegister(
            zero = sub == 0.toUShort(),
            subtract = true,
            halfCarry = halfBorrow,
            carry = borrow,
        )
        registers.f = flags.toUByte()
    }

    private fun addSp() {
        stackPointer.setTo(addSignedToSp())
    }

    private fun loadHlSpOffset() {
        registers.hl = addSignedToSp()
    }

    private fun loadSpHl() {
        stackPointer.setTo(registers.hl)
    }

    private fun storeSpAtImmediate() {
        val address = readImmediate16()
        val sp = stackPointer.get()
        val low = (sp.toInt() and 0x00FF).toUByte()
        val high = ((sp.toInt() and 0xFF00) ushr 8).toUByte()
        memoryBus.writeByte(address, low)
        memoryBus.writeByte((address + 1u).toUShort(), high)
    }

    private fun inc(target: Register) {
        when (target) {
            is Register16 -> inc(target)
            is Register8 -> inc(target)
        }
    }

    private fun inc(target: Register8) {
        updateOperand(target) { register ->
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
        updateOperand(target) { value ->
            (value.toInt() + 1).toUShort()
        }
    }

    private fun dec(target: Register) {
        when (target) {
            is Register16 -> dec(target)
            is Register8 -> dec(target)
        }
    }

    private fun dec(target: Register8) {
        updateOperand(target) { register ->
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
        updateOperand(target) { value ->
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

    private fun rr(target: Operand8) {
        updateOperand(target) { targetValue ->
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

    private fun rrc(target: Operand8) {
        updateOperand(target) { targetValue ->
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

    private fun rl(target: Operand8) {
        updateOperand(target) { targetValue ->
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

    private fun rlc(target: Operand8) {
        updateOperand(target) { targetValue ->
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
        target: Operand8,
    ) {
        val targetValue = getOperandValue(target)
        val bitSet =
            ((targetValue.toInt() ushr index).toUByte() and 0b1.toUByte()) == 0b1.toUByte()
        val flags = registers.f.toFlagsRegister()
        registers.f = flags.copy(
            zero = !bitSet,
            subtract = false,
            halfCarry = true,
        ).toUByte()
    }

    private fun res(
        index: Int,
        target: Operand8,
    ) {
        updateOperand(target) { targetValue ->
            val mask = (0b1 shl index).toUByte().inv()
            targetValue and mask
        }
    }

    private fun set(
        index: Int,
        target: Operand8,
    ) {
        updateOperand(target) { targetValue ->
            val mask = (0b1 shl index).toUByte()
            targetValue or mask
        }
    }

    private fun srl(
        target: Operand8,
    ) {
        updateOperand(target) { targetValue ->
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
        target: Operand8,
    ) {
        updateOperand(target) { targetValue ->
            val leastSignificantBit = targetValue and 0b1.toUByte()
            val mostSignificantBit = targetValue and (0b1 shl 7).toUByte()
            val shiftedValue =
                ((targetValue.toInt() ushr 1) and 0xFF).toUByte() or mostSignificantBit

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
        target: Operand8,
    ) {
        updateOperand(target) { targetValue ->
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
        target: Operand8,
    ) {
        updateOperand(target) { targetValue ->
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
            readImmediate16()
        } else {
            // even if we don't need to jump, we need to "consume" the jump's 16 bit address
            programCounter.increaseBy(stepSize = 2)
            null
        }
    }

    private fun jumpRelative(condition: JumpCondition) {
        val jump = evaluateJumpCondition(condition)

        return if (jump) {
            val offset = readImmediate8()
            val signedOffset = offset.toByte().toInt()
            programCounter.increaseBy(stepSize = signedOffset)
        } else {
            // even if we don't need to jump, we need to "consume" the jump's 8 bit address
            programCounter.increaseBy(stepSize = 1)
        }
    }

    private fun load(source: Operand, target: Operand) {
        when (target) {
            is Register16 -> load(source, target)
            is Operand8 -> load(source, target)
            else -> error("Invalid load target: $target")
        }
    }

    private fun load(
        source: Operand,
        target: Operand8,
    ) {
        require(source is Operand8) { "Invalid load source for 8-bit register: $source" }
        val byteToLoad = when (source) {
            MemoryAtHl -> memoryBus.readByte(registers.hl)
            is MemoryAtRegister16 -> readMemoryAtRegister16(source.register)
            MemoryAtData16 -> readMemoryAtImmediate16()
            MemoryAtHighData8 -> readMemoryAtHighData8()
            MemoryAtHighC -> readMemoryAtHighC()
            Data8 -> readImmediate8()
            Register8.A -> registers.a
            Register8.B -> registers.b
            Register8.C -> registers.c
            Register8.D -> registers.d
            Register8.E -> registers.e
            Register8.H -> registers.h
            Register8.L -> registers.l
        }
        updateOperand(target) { byteToLoad }
    }

    private fun load(
        source: Operand,
        target: Register16,
    ) {
        require(source is Data16) { "Invalid load source for 16-bit register: $source" }
        updateOperand(target) { readImmediate16() }
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
                registers.f =
                    leastSignificantByte and 0xF0.toUByte() // lower nibble of F is always 0
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
            val address =
                ((mostSignificantByte.toInt() shl 8) or (leastSignificantByte.toInt())).toUShort()

            pushProgramCounterToStack()

            address
        } else {
            // even if we don't need to call, we need to "consume" the call's 16 bit address
            programCounter.increaseBy(stepSize = 2)
            null
        }
    }

    private fun ret(condition: JumpCondition) {
        val ret = evaluateJumpCondition(condition)

        if (ret) {
            returnFromSubroutine()
        }
    }

    private fun returnAndEnableInterrupts() {
        returnFromSubroutine()
        enableGlobalInterrupt()
    }

    private fun rst(address: UByte) {
        pushProgramCounterToStack()
        programCounter.setTo(address.toUShort())
    }

    private fun loadDecAhl() {
        registers.a = memoryBus.readByte(registers.hl)
        registers.hl = (registers.hl.toInt() - 1).toUShort()
    }

    private fun loadIncAhl() {
        registers.a = memoryBus.readByte(registers.hl)
        registers.hl = (registers.hl.toInt() + 1).toUShort()
    }

    private fun loadDecHla() {
        memoryBus.writeByte(registers.hl, registers.a)
        registers.hl = (registers.hl.toInt() - 1).toUShort()
    }

    private fun loadIncHla() {
        memoryBus.writeByte(registers.hl, registers.a)
        registers.hl = (registers.hl.toInt() + 1).toUShort()
    }

    private fun daa() {
        var correction = 0
        val flags = registers.f.toFlagsRegister()

        if (flags.subtract) {
            if (flags.halfCarry) {
                correction = correction or 0x06
            }
            if (flags.carry) {
                correction = correction or 0x60
            }
            registers.a = (registers.a.toInt() - correction).toUByte()
        } else {
            if (flags.carry || registers.a > 0x99.toUByte()) {
                correction = correction or 0x60
                registers.f = registers.f.toFlagsRegister().copy(carry = true).toUByte()
            }
            if (flags.halfCarry || (registers.a and 0x0F.toUByte()) > 0x09.toUByte()) {
                correction = correction or 0x06
            }
            registers.a = (registers.a.toInt() + correction).toUByte()
        }

        registers.f = registers.f.toFlagsRegister().copy(
            zero = registers.a == 0.toUByte(),
            halfCarry = false,
        ).toUByte()
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

    private fun readImmediate8(): UByte {
        return memoryBus.readByte(programCounter.getAndIncrement())
    }

    private fun readImmediate16(): UShort {
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

    private fun addSignedToSp(): UShort {
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

    private fun halt() {
        halted = true
    }

    private fun stop() {
        halted = true
    }

    private fun disableGlobalInterrupt() {
        globalInterruptEnabled = false
        enableGlobalInterruptPending = false
    }

    private fun enableGlobalInterrupt() {
        enableGlobalInterruptPending = true
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

    private fun pushProgramCounterToStack() {
        val pc = programCounter.get()
        memoryBus.writeByte(
            stackPointer.decrementAndGet(),
            ((pc.toInt() and 0xFF00) ushr 8).toUByte()
        )
        memoryBus.writeByte(stackPointer.decrementAndGet(), (pc.toInt() and 0x00FF).toUByte())
    }

    private fun returnFromSubroutine() {
        val leastSignificantByte = memoryBus.readByte(stackPointer.getAndIncrement())
        val mostSignificantByte = memoryBus.readByte(stackPointer.getAndIncrement())
        val addressHigh = mostSignificantByte.toInt() shl 8
        val addressLow = leastSignificantByte.toInt()
        val returnAddress = (addressHigh or addressLow).toUShort()
        programCounter.setTo(returnAddress)
    }
}
