package com.vicgarci.kgbem.cartridge

import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class Mbc3Cartridge(
    romData: ByteArray,
    private val typeId: Int,
    private val clock: Clock = Clock.System
) : Cartridge {

    private val rom: ByteArray = romData.copyOf()

    private val romBankCount: Int = 2 shl (rom[ROM_SIZE_ADDR].toInt() and 0xFF)

    private val ramSize: Int = when (rom[RAM_SIZE_ADDR].toInt() and 0xFF) {
        0x02 -> RAM_BANK_SIZE            // 8 KB  (1 bank)
        0x03 -> RAM_BANK_SIZE * 4        // 32 KB (4 banks)
        else -> 0
    }

    private val ram: ByteArray = ByteArray(ramSize)

    private var ramEnabled: Boolean = false
    private var romBank: Int = 1
    private var ramBankOrRtcRegister: Int = 0  // 0x00-0x03 = RAM bank, 0x08-0x0C = RTC register
    private val isRtcSelected: Boolean get() = ramBankOrRtcRegister in 0x08..0x0C

    // RTC state
    private var rtcBaseInstant: Instant = clock.now()
    private var rtcHalted: Boolean = false
    private var haltedElapsedSeconds: Long = 0

    // RTC register values (writable)
    private var rtcSeconds: Int = 0
    private var rtcMinutes: Int = 0
    private var rtcHours: Int = 0
    private var rtcDayLow: Int = 0
    private var rtcDayHigh: Int = 0  // bit 0 = day counter bit 8, bit 6 = halt, bit 7 = day overflow

    // Latch state
    private var latchReady: Boolean = false
    private var latched: Boolean = false
    private var latchedSeconds: Int = 0
    private var latchedMinutes: Int = 0
    private var latchedHours: Int = 0
    private var latchedDayLow: Int = 0
    private var latchedDayHigh: Int = 0

    // --- Cartridge interface ---

    override fun readRom(address: Int): Int {
        return when {
            address < 0x4000 -> readRomByte(address)
            else -> {
                val bank = romBank % romBankCount
                val offset = address - 0x4000
                readRomByte(bank * ROM_BANK_SIZE + offset)
            }
        }
    }

    override fun writeRom(address: Int, value: Int) {
        when {
            address < 0x2000 -> {
                ramEnabled = (value and 0x0F) == 0x0A
            }
            address < 0x4000 -> {
                var bank = value and 0x7F
                if (bank == 0) bank = 1
                romBank = bank
            }
            address < 0x6000 -> {
                ramBankOrRtcRegister = value and 0xFF
            }
            else -> {
                // Latch clock data: write 0x00 then 0x01
                val v = value and 0xFF
                if (v == 0x00) {
                    latchReady = true
                } else if (v == 0x01 && latchReady) {
                    latchReady = false
                    performLatch()
                } else {
                    latchReady = false
                }
            }
        }
    }

    override fun readRam(address: Int): Int {
        if (!ramEnabled) return 0xFF
        return if (isRtcSelected) {
            readRtcRegister(ramBankOrRtcRegister)
        } else {
            val bank = ramBankOrRtcRegister and 0x03
            val offset = address - 0xA000
            val physicalAddress = bank * RAM_BANK_SIZE + offset
            if (physicalAddress < ramSize) ram[physicalAddress].toInt() and 0xFF else 0xFF
        }
    }

    override fun writeRam(address: Int, value: Int) {
        if (!ramEnabled) return
        if (isRtcSelected) {
            writeRtcRegister(ramBankOrRtcRegister, value and 0xFF)
        } else {
            val bank = ramBankOrRtcRegister and 0x03
            val offset = address - 0xA000
            val physicalAddress = bank * RAM_BANK_SIZE + offset
            if (physicalAddress < ramSize) {
                ram[physicalAddress] = value.toByte()
            }
        }
    }

    override fun hasBattery(): Boolean = typeId in BATTERY_TYPES

    override fun savableState(): ByteArray? {
        return if (ramSize > 0 && hasBattery()) ram.copyOf() else null
    }

    override fun loadState(bytes: ByteArray) {
        if (ramSize > 0 && hasBattery()) {
            bytes.copyInto(ram, endIndex = minOf(bytes.size, ram.size))
        }
    }

    // --- RTC ---

    private fun computeRtc() {
        val elapsed = if (rtcHalted) {
            haltedElapsedSeconds
        } else {
            val now = clock.now()
            (now - rtcBaseInstant).inWholeSeconds
        }

        val totalSeconds = elapsed
        rtcSeconds = (totalSeconds % 60).toInt()
        val totalMinutes = totalSeconds / 60
        rtcMinutes = (totalMinutes % 60).toInt()
        val totalHours = totalMinutes / 60
        rtcHours = (totalHours % 24).toInt()
        val totalDays = totalHours / 24
        rtcDayLow = (totalDays and 0xFF).toInt()
        val dayHigh = rtcDayHigh and 0xC0 // preserve halt + overflow flags
        rtcDayHigh = dayHigh or ((totalDays shr 8).toInt() and 0x01)
        if (totalDays > 511) {
            rtcDayHigh = rtcDayHigh or 0x80 // set overflow
        }
    }

    private fun readRtcRegister(register: Int): Int {
        if (latched) {
            return when (register) {
                0x08 -> latchedSeconds
                0x09 -> latchedMinutes
                0x0A -> latchedHours
                0x0B -> latchedDayLow
                0x0C -> latchedDayHigh
                else -> 0xFF
            }
        }
        computeRtc()
        return when (register) {
            0x08 -> rtcSeconds
            0x09 -> rtcMinutes
            0x0A -> rtcHours
            0x0B -> rtcDayLow
            0x0C -> rtcDayHigh
            else -> 0xFF
        }
    }

    private fun writeRtcRegister(register: Int, value: Int) {
        // Writing to RTC registers resets the base instant to reflect the new value
        computeRtc() // sync current state first
        when (register) {
            0x08 -> {
                val diff = value - rtcSeconds
                adjustBase(diff.toLong())
                rtcSeconds = value
            }
            0x09 -> {
                val diff = (value - rtcMinutes) * 60L
                adjustBase(diff)
                rtcMinutes = value
            }
            0x0A -> {
                val diff = (value - rtcHours) * 3600L
                adjustBase(diff)
                rtcHours = value
            }
            0x0B -> {
                val diff = (value - rtcDayLow) * 86400L
                adjustBase(diff)
                rtcDayLow = value
            }
            0x0C -> {
                val oldHalt = (rtcDayHigh and 0x40) != 0
                val newHalt = (value and 0x40) != 0
                rtcDayHigh = value and 0xC1

                if (!oldHalt && newHalt) {
                    // Halting: store elapsed time
                    haltedElapsedSeconds = (clock.now() - rtcBaseInstant).inWholeSeconds
                    rtcHalted = true
                } else if (oldHalt && !newHalt) {
                    // Resuming: adjust base so elapsed stays the same
                    rtcBaseInstant = clock.now() - haltedElapsedSeconds.seconds
                    rtcHalted = false
                }
            }
        }
    }

    private fun adjustBase(diffSeconds: Long) {
        if (rtcHalted) {
            haltedElapsedSeconds += diffSeconds
        } else {
            rtcBaseInstant = rtcBaseInstant - diffSeconds.seconds
        }
    }

    private fun performLatch() {
        computeRtc()
        latchedSeconds = rtcSeconds
        latchedMinutes = rtcMinutes
        latchedHours = rtcHours
        latchedDayLow = rtcDayLow
        latchedDayHigh = rtcDayHigh
        latched = true
    }

    private fun readRomByte(physicalAddress: Int): Int {
        return if (physicalAddress < rom.size) rom[physicalAddress].toInt() and 0xFF else 0xFF
    }

    companion object {
        const val TYPE_MBC3_TIMER_BATTERY = 0x0F
        const val TYPE_MBC3_TIMER_RAM_BATTERY = 0x10
        const val TYPE_MBC3_ONLY = 0x11
        const val TYPE_MBC3_RAM = 0x12
        const val TYPE_MBC3_RAM_BATTERY = 0x13

        private val BATTERY_TYPES = setOf(
            TYPE_MBC3_TIMER_BATTERY,
            TYPE_MBC3_TIMER_RAM_BATTERY,
            TYPE_MBC3_RAM_BATTERY
        )

        private const val ROM_SIZE_ADDR = 0x0148
        private const val RAM_SIZE_ADDR = 0x0149
        private const val ROM_BANK_SIZE = 0x4000   // 16 KB
        private const val RAM_BANK_SIZE = 0x2000    // 8 KB
    }
}
