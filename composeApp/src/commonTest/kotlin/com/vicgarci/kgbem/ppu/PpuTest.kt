package com.vicgarci.kgbem.ppu

import com.vicgarci.kgbem.cartridge.RomOnlyCartridge
import com.vicgarci.kgbem.cpu.MemoryBus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PpuTest {

    private fun createTestPpu(): Triple<Ppu, MemoryBus, RecordingFrameSink> {
        val cartridge = RomOnlyCartridge(ByteArray(0x8000))
        val bus = MemoryBus(cartridge)
        val sink = RecordingFrameSink()
        val ppu = Ppu(bus, sink)
        return Triple(ppu, bus, sink)
    }

    @Test
    fun oneFullFrameProducesExactlyOneFrame() {
        val (ppu, bus, sink) = createTestPpu()

        ppu.step(70224) // 456 * 154 = one full frame

        assertEquals(1, sink.frames.size)
    }

    @Test
    fun vblankInterruptFlagSetAfterFrame() {
        val (ppu, bus, sink) = createTestPpu()

        ppu.step(70224)

        val ifFlags = bus.readByte(0xFF0Fu.toUShort()).toInt()
        assertTrue((ifFlags and 0x01) != 0, "VBlank interrupt flag should be set")
    }

    @Test
    fun lyResetsToZeroAfterFullFrame() {
        val (ppu, bus, sink) = createTestPpu()

        ppu.step(70224)

        val ly = bus.readByte(0xFF44u.toUShort())
        assertEquals(0u.toUByte(), ly, "LY should be 0 after a complete frame")
    }

    @Test
    fun statModeBitsReflectOamSearchAtStart() {
        val (ppu, bus, _) = createTestPpu()

        val stat = bus.readByte(0xFF41u.toUShort()).toInt()
        assertEquals(2, stat and 0x03, "Mode should be OAM Search (2) at start")
    }

    @Test
    fun statModeBitsReflectDrawingAfterOamSearch() {
        val (ppu, bus, _) = createTestPpu()

        ppu.step(80) // OAM search duration

        val stat = bus.readByte(0xFF41u.toUShort()).toInt()
        assertEquals(3, stat and 0x03, "Mode should be Drawing (3) after OAM search")
    }

    @Test
    fun statModeBitsReflectHblankAfterDrawing() {
        val (ppu, bus, _) = createTestPpu()

        ppu.step(80 + 172) // OAM search + Drawing

        val stat = bus.readByte(0xFF41u.toUShort()).toInt()
        assertEquals(0, stat and 0x03, "Mode should be HBlank (0) after drawing")
    }

    @Test
    fun statModeBitsReflectVblankAtScanline144() {
        val (ppu, bus, _) = createTestPpu()

        ppu.step(456 * 144) // Complete all visible scanlines

        val stat = bus.readByte(0xFF41u.toUShort()).toInt()
        assertEquals(1, stat and 0x03, "Mode should be VBlank (1) at scanline 144")
    }

    @Test
    fun lyIncrementsThroughScanlines() {
        val (ppu, bus, _) = createTestPpu()

        ppu.step(456) // One full scanline

        val ly = bus.readByte(0xFF44u.toUShort()).toInt()
        assertEquals(1, ly, "LY should be 1 after one scanline")
    }

    @Test
    fun lycCoincidenceSetsStatBit2() {
        val (ppu, bus, _) = createTestPpu()

        // Set LYC to 1
        bus.writeByte(0xFF45u.toUShort(), 1u.toUByte())

        ppu.step(456) // Advance to scanline 1

        val stat = bus.readByte(0xFF41u.toUShort()).toInt()
        assertTrue((stat and 0x04) != 0, "STAT coincidence flag (bit 2) should be set when LY == LYC")
    }

    @Test
    fun lycCoincidenceFiresStatInterruptWhenEnabled() {
        val (ppu, bus, _) = createTestPpu()

        // Set LYC to 1 and enable LYC interrupt in STAT (bit 6)
        bus.writeByte(0xFF45u.toUShort(), 1u.toUByte())
        bus.writeByte(0xFF41u.toUShort(), 0x40u.toUByte())

        ppu.step(456) // Advance to scanline 1

        val ifFlags = bus.readByte(0xFF0Fu.toUShort()).toInt()
        assertTrue((ifFlags and 0x02) != 0, "LCD STAT interrupt should fire when LY == LYC and STAT bit 6 is set")
    }

    @Test
    fun twoFullFramesProduceTwoFrames() {
        val (ppu, bus, sink) = createTestPpu()

        ppu.step(70224 * 2)

        assertEquals(2, sink.frames.size)
    }

    @Test
    fun frontBufferIsAllWhiteWhenLcdOff() {
        val (ppu, _, sink) = createTestPpu()

        // LCDC defaults to 0x00 (LCD off), so output should be all white
        ppu.step(70224)

        val frame = sink.frames.first()
        val white = 0xFFFFFFFF.toInt()
        assertTrue(frame.all { it == white }, "Frame buffer should be all white when LCD is off")
        assertEquals(160 * 144, frame.size, "Frame should be 160x144 pixels")
    }
}
