# Backlog — kgbemu

---

## Epic 1: Build Foundation
**Slice:** 1

### Story 1.1: All expect/actual boundaries compile on every target
**ACs:**
- [ ] `./gradlew :composeApp:compileKotlinAndroid`, `compileKotlinJvm`, and `compileKotlinIosArm64` all succeed with stub `actual` bodies
- [ ] `kotlinx-datetime` version is declared in `libs.versions.toml` and resolves without conflict

#### Task 1.1.1: Stub all expect/actual declarations
**Owner:** Arthur
**Domain:** infra
**Status:** todo
**Dependencies:** none
**ACs:**
- [ ] `expect class LoopDriver`, `expect suspend fun pickRomFile()`, `expect object SaveStorage` declared in `commonMain`
- [ ] `actual` stubs with `TODO("not yet implemented")` bodies exist in `androidMain`, `jvmMain`, `iosMain`
- [ ] All three Kotlin compile tasks succeed

#### Task 1.1.2: Add kotlinx-datetime to dependency graph
**Owner:** Arthur
**Domain:** infra
**Status:** todo
**Dependencies:** none
**ACs:**
- [ ] `kotlinx-datetime` added to `libs.versions.toml` and `commonMain` dependencies in `build.gradle.kts`
- [ ] `import kotlinx.datetime.Clock` compiles in a `commonMain` file
- [ ] No version conflict with existing Compose Multiplatform dependencies (`./gradlew dependencies` shows single resolved version)

#### Task 1.1.3: Validate ImageBitmap pixel-write API on Android and Desktop (SPIKE-002)
**Owner:** Harry
**Domain:** android
**Status:** todo
**Dependencies:** Task 1.1.1
**ACs:**
- [ ] A throwaway `commonTest` or `androidTest` test writes a known `IntArray(160*144)` of ARGB values into a `Bitmap` via `Bitmap.setPixels()` on Android and reads back the same values
- [ ] Equivalent test passes on Desktop (JVM) using `org.jetbrains.skia.Bitmap` or `BufferedImage`
- [ ] Decision documented in `TECH_SPEC.md` OQ-2 with the confirmed API name per platform

---

## Epic 2: ROM Loading Core
**Slice:** 1

### Story 2.1: Valid ROM is loaded and parsed into a Cartridge
**ACs:**
- [ ] Loading a valid 32 KB ROM-only `.gb` file returns a `RomOnlyCartridge`
- [ ] Loading a file shorter than 0x150 bytes returns `RomError.Truncated`
- [ ] Loading a file with bad Nintendo logo bytes returns `RomError.InvalidHeader`
- [ ] Loading a file with a failing header checksum returns `RomError.InvalidHeader`
- [ ] Loading a file with cartridge type byte 0x05 (MBC2, unsupported) returns `RomError.UnsupportedMapper(0x05)`

#### Task 2.1.1: Cartridge interface + RomOnlyCartridge
**Owner:** Hermione
**Domain:** backend
**Status:** todo
**Dependencies:** none
**ACs:**
- [ ] `Cartridge` interface declared in `commonMain` with `readRom`, `writeRom`, `readRam`, `writeRam`, `hasBattery`, `savableState`, `loadState`
- [ ] `RomOnlyCartridge` reads correct bytes at any address in 0x0000–0x7FFF
- [ ] `RomOnlyCartridge.writeRom()` is a no-op (writes ignored)
- [ ] `RomOnlyCartridge.hasBattery()` returns false
- [ ] Unit tests pass for all read/write scenarios

#### Task 2.1.2: CartridgeLoader with header validation
**Owner:** Hermione
**Domain:** backend
**Status:** todo
**Dependencies:** Task 2.1.1
**ACs:**
- [ ] `CartridgeLoader.load(ByteArray)` returns `Result<Cartridge, RomError>`
- [ ] All 5 validation scenarios from Story 2.1 ACs have passing unit tests
- [ ] All array accesses in the parser are bounded by validated `bytes.size`; no index derived from ROM-embedded values without clamping
- [ ] Type byte 0x00 instantiates `RomOnlyCartridge`; types 0x01–0x03 and 0x0F–0x13 return `UnsupportedMapper` (MBC1/MBC3 implemented in Slice 4)

#### Task 2.1.3: MemoryBus refactored to delegate to Cartridge
**Owner:** Hermione
**Domain:** backend
**Status:** todo
**Dependencies:** Task 2.1.1
**ACs:**
- [ ] `MemoryBus` holds a `Cartridge` reference; reads at 0x0000–0x7FFF delegate to `cartridge.readRom()`; writes to `cartridge.writeRom()`
- [ ] Reads at 0xA000–0xBFFF delegate to `cartridge.readRam()`; writes to `cartridge.writeRam()`
- [ ] All existing CPU unit tests pass with a `RomOnlyCartridge` stub injected into `MemoryBus`
- [ ] No raw `ByteArray` for cartridge data remains in `MemoryBus`

---

## Epic 3: Emulator Loop (Blind)
**Slice:** 1

### Story 3.1: Emulator loop ticks CPU with correct post-boot state
**ACs:**
- [ ] After `EmulatorLoop.runFrame()`, exactly 70,224 T-cycles have been advanced
- [ ] CPU register A is 0x01 and PC is 0x0100 before the first `runFrame()` call

#### Task 3.1.1: Post-boot CPU initialisation
**Owner:** Hermione
**Domain:** backend
**Status:** todo
**Dependencies:** Task 2.1.3
**ACs:**
- [ ] `CpuInitialiser.applyPostBootState(cpu, bus)` sets all registers to DMG post-boot values (A=0x01, F=0xB0, B=0x00, C=0x13, D=0x00, E=0xD8, H=0x01, L=0x4D, SP=0xFFFE, PC=0x0100)
- [ ] Key memory addresses initialised: LCDC=0x91, BGP=0xFC, OBP0=0xFF, OBP1=0xFF (full table from TECH_SPEC §13)
- [ ] Unit test asserts all register values after `applyPostBootState`

#### Task 3.1.2: EmulatorLoop.runFrame() ticking CPU only
**Owner:** Hermione
**Domain:** backend
**Status:** todo
**Dependencies:** Task 3.1.1, Task 2.1.3
**ACs:**
- [ ] `EmulatorLoop.runFrame()` calls `cpu.step()` in a loop until ≥ 70,224 T-cycles accumulated
- [ ] `FrameSink.onFrame()` is called exactly once per `runFrame()` (with a null/empty pixel array at this stage — PPU not wired yet)
- [ ] Integration test: construct `EmulatorLoop` with `RomOnlyCartridge` (NOP-filled ROM), `RecordingFrameSink`; call `runFrame()` 3 times; assert sink received 3 calls; assert no exception thrown
- [ ] `RecordingFrameSink` implemented in `commonTest` for use across all integration tests

---

## Epic 4: Android Walking Skeleton
**Slice:** 1

### Story 4.1: User opens a ROM on Android and sees a blank game screen
**ACs:**
- [ ] Tapping "Open ROM" launches the system file picker filtered to `*/*` (`.gb` files selectable)
- [ ] A valid ROM transitions the UI from LauncherScreen to GameScreen showing a 160×144 grey placeholder
- [ ] An invalid ROM shows the correct error message on LauncherScreen
- [ ] App does not crash when backgrounded during emulation

#### Task 4.1.1: Metro DI setup
**Owner:** Harry
**Domain:** android
**Status:** todo
**Dependencies:** Task 1.1.1
**ACs:**
- [ ] `dev.zacsweers.metro` plugin added to root and `composeApp` `build.gradle.kts`; version in `libs.versions.toml`
- [ ] `AppGraph` interface declared in `commonMain` with `@DependencyGraph` and `AppScope` scope annotation
- [ ] `EmulatorController` in `commonMain` annotated `@Inject @SingleIn(AppScope::class)`
- [ ] `AndroidModule` in `androidMain` with `@ContributesTo(AppScope::class)` providing `LoopDriver`, `InputSource`, `SaveStorage` Android actuals
- [ ] `createGraph<AppGraph>()` called in `MainActivity.onCreate`; no runtime crash
- [ ] `./gradlew :composeApp:assembleDebug` succeeds

#### Task 4.1.2: EmulatorController (commonMain) + EmulatorViewModel (androidMain)
**Owner:** Harry
**Domain:** android
**Status:** todo
**Dependencies:** Task 4.1.1, Task 3.1.2
**ACs:**
- [ ] `EmulatorController` in `commonMain` exposes `emulatorState: StateFlow<EmulatorState>` and `frameState: StateFlow<IntArray?>`; implements `FrameSink.onFrame()` via `_frameState.value = pixels`
- [ ] `EmulatorViewModel` in `androidMain` wraps `EmulatorController`; implements `DefaultLifecycleObserver`: `onStop` pauses; `onStart` resumes if was Running; `onCleared` stops
- [ ] `EmulatorViewModel` created via `ViewModelProvider` factory using the `AppGraph` instance from `MainActivity`
- [ ] ViewModel survives configuration change (rotation) without restarting the emulator loop

#### Task 4.1.3: FilePicker Android actual + ROM caching
**Owner:** Harry
**Domain:** android
**Status:** todo
**Dependencies:** Task 4.1.1
**ACs:**
- [ ] `actual suspend fun pickRomFile()` registered via `rememberLauncherForActivityResult` called unconditionally at composition root (not inside click handler)
- [ ] `ContentResolver.openInputStream()` called on `Dispatchers.IO`
- [ ] ROM bytes copied to `filesDir/roms/<sanitised-name>.gb` before returning — URI is not retained
- [ ] Returns `null` if user cancels picker
- [ ] Tested on API 26 emulator: file reads correctly

#### Task 4.1.4: LoopDriver Android actual
**Owner:** Harry
**Domain:** android
**Status:** todo
**Dependencies:** Task 3.1.2
**ACs:**
- [ ] `actual class LoopDriver` in `androidMain` runs `EmulatorLoop.runFrame()` on `Dispatchers.Default` coroutine, paced to ~16.74 ms/frame using `System.nanoTime`
- [ ] `stop()` cancels the coroutine; no more `runFrame()` calls after `stop()` returns
- [ ] `start()` launches a new coroutine; safe to call after `stop()`
- [ ] Manual test: LoopDriver runs for 5 seconds on Pixel 6 without ANR or visible jank in Compose

#### Task 4.1.5: LauncherScreen
**Owner:** Harry
**Domain:** android
**Status:** todo
**Dependencies:** Task 4.1.2, Task 4.1.3
**ACs:**
- [ ] Unloaded state: kgbemu wordmark + "Open ROM" button visible
- [ ] Loading state: spinner shown, button disabled
- [ ] Error — Truncated: message "ROM file is incomplete or corrupted." + "Try Again" button
- [ ] Error — InvalidHeader: message "ROM header is invalid. The file may be corrupted." + "Try Again" button
- [ ] Error — UnsupportedMapper: message "This cartridge type is not supported yet." + "Try Again" button
- [ ] "Try Again" re-invokes `pickRomFile()`

#### Task 4.1.6: GameScreen with grey placeholder viewport
**Owner:** Harry
**Domain:** android
**Status:** todo
**Dependencies:** Task 4.1.2
**ACs:**
- [ ] GameScreen displayed when `emulatorState` is Running
- [ ] 160×144 grey rectangle rendered, integer-scaled using `Modifier.aspectRatio(160f / 144f)` — no distortion on tall phones
- [ ] Header bar visible with menu icon (top-left) and pause/play icon (top-right)
- [ ] Screen transitions correctly from LauncherScreen on ROM load success

---

## Epic 5: Test Infrastructure
**Slice:** 2

### Story 5.1: PPU and EmulatorLoop can be tested headlessly
**ACs:**
- [ ] `RecordingFrameSink` captures frames in `commonTest` without any Compose/platform dependency
- [ ] Test code can load `dmg-acid2.gb` bytes in `commonTest` via a cross-platform resource loader

#### Task 5.1.1: RecordingFrameSink in commonTest
**Owner:** Neville
**Domain:** qa
**Status:** todo
**Dependencies:** none
**ACs:**
- [ ] `RecordingFrameSink : FrameSink` in `commonTest` stores each `IntArray` received in `frames: List<IntArray>`
- [ ] Each frame is copied (`copyOf()`) so mutations to the PPU buffer don't affect recorded frames
- [ ] `lastFrame` convenience property returns `frames.last()` or null

#### Task 5.1.2: KMP test resource loader
**Owner:** Neville
**Domain:** qa
**Status:** todo
**Dependencies:** none
**ACs:**
- [ ] `expect fun loadTestResource(path: String): ByteArray` declared in `commonTest`
- [ ] `actual` implementations for JVM (uses `ClassLoader.getResourceAsStream`) and Android (`InstrumentationRegistry.getContext().assets`)
- [ ] A smoke test asserts `loadTestResource("dmg-acid2.gb").size > 1000` passes on JVM target
- [ ] `dmg-acid2.gb` committed to `composeApp/src/commonTest/resources/`

---

## Epic 6: Timer
**Slice:** 2

### Story 6.1: Timer advances correctly and fires interrupts
**ACs:**
- [ ] DIV increments at 16,384 Hz (once per 256 T-cycles)
- [ ] TIMA increments at all 4 TAC-selected frequencies
- [ ] TIMA overflow fires interrupt and loads TMA with a 4 T-cycle delay
- [ ] Writing to DIV resets it to 0 and may trigger a TIMA increment per the falling-edge quirk

#### Task 6.1.1: Timer implementation
**Owner:** Hermione
**Domain:** backend
**Status:** todo
**Dependencies:** Task 2.1.3
**ACs:**
- [ ] `Timer` class with `step(cycles: Int)` advances a 16-bit internal counter; DIV (0xFF04) is the upper byte
- [ ] TIMA increments at correct T-cycle intervals for all 4 TAC modes (1024, 16, 64, 256 T-cycles)
- [ ] TIMA overflow: TMA loaded into TIMA after exactly 4 T-cycles; Timer interrupt flag (0xFF0F bit 2) set
- [ ] Writing any value to 0xFF04: internal counter zeroed; if the TAC-selected bit was 1 before zeroing, TIMA incremented immediately
- [ ] Unit tests: parametrised for all 4 TAC frequencies; overflow + delay; DIV-reset TIMA trigger; TAC enable/disable

---

## Epic 7: PPU + Frame Pipeline
**Slice:** 2

### Story 7.1: PPU renders correct frames at 59.73 Hz
**ACs:**
- [ ] dmg-acid2.gb produces a frame matching the published reference image (pixel hash comparison)
- [ ] PPU fires VBlank interrupt after scanline 143 each frame
- [ ] OAM DMA triggered by write to 0xFF46 halts CPU for exactly 640 T-cycles

#### Task 7.1.1: PPU mode state machine
**Owner:** Hermione
**Domain:** backend
**Status:** todo
**Dependencies:** Task 2.1.3, Task 5.1.1
**ACs:**
- [ ] `Ppu.step(cycles: Int)` advances the mode state machine: OAM Search (80) → Drawing (172) → HBlank (204) per scanline; VBlank after scanline 143
- [ ] STAT register (0xFF41) updated with current mode on each transition
- [ ] LYC=LY interrupt fires when LY matches LYC and STAT bit 6 is set
- [ ] VBlank interrupt flag (0xFF0F bit 0) set on VBlank entry
- [ ] `ppu.swapBuffers()` and `frameSink.onFrame(frontBuffer)` called exactly once per frame (on VBlank)
- [ ] Integration test using `RecordingFrameSink`: 154 scanlines × 456 T-cycles = 70,224 T-cycles; sink called once

#### Task 7.1.2: PPU background tile rendering
**Owner:** Hermione
**Domain:** backend
**Status:** todo
**Dependencies:** Task 7.1.1
**ACs:**
- [ ] Tile map read from 0x9800 or 0x9C00 per LCDC bit 3
- [ ] Tile data read from 0x8000 (unsigned indexing) or 0x8800 (signed indexing) per LCDC bit 4
- [ ] SCX/SCY scroll applied correctly; wraps at tile map boundary
- [ ] 2-bit colour → BGP register → ARGB grey value mapped correctly
- [ ] Unit test: VRAM loaded with a known tile pattern; assert specific pixels in `frontBuffer` after one frame

#### Task 7.1.3: PPU window layer
**Owner:** Hermione
**Domain:** backend
**Status:** todo
**Dependencies:** Task 7.1.2
**ACs:**
- [ ] Window rendered when LCDC bit 5 set, LY ≥ WY, and pixel X ≥ (WX − 7)
- [ ] Window has its own internal line counter (increments each scanline the window is active)
- [ ] Window tile map read from 0x9800 or 0x9C00 per LCDC bit 6
- [ ] Unit test: set WY=0, WX=7, load window tile map; assert window pixels appear at correct X position

#### Task 7.1.4: PPU sprite rendering + OBJ-to-BG priority
**Owner:** Hermione
**Domain:** backend
**Status:** todo
**Dependencies:** Task 7.1.2
**ACs:**
- [ ] OAM scanned for sprites on current scanline; max 10 per scanline enforced (by OAM index order)
- [ ] Sprite pixel rendered above BG when OBJ-to-BG priority bit = 0
- [ ] Sprite pixel hidden behind BG colours 1–3 when OBJ-to-BG priority bit = 1 (only shows over BG colour 0)
- [ ] OBP0 / OBP1 palette selected per sprite attribute bit 4
- [ ] Sprite colour 0 is always transparent
- [ ] Unit test: two overlapping sprites; assert lower OAM index wins; assert priority bit behaviour

#### Task 7.1.5: OAM DMA
**Owner:** Hermione
**Domain:** backend
**Status:** todo
**Dependencies:** Task 7.1.1
**ACs:**
- [ ] Write to 0xFF46 triggers transfer of 160 bytes from `(value << 8)` to OAM (0xFE00–0xFE9F)
- [ ] `MemoryBus` sets a `dmaActive` flag for 640 T-cycles; during this time all reads outside HRAM (0xFF80–0xFFFE) return 0xFF and writes are ignored
- [ ] DMA completes after exactly 640 T-cycles; `dmaActive` cleared
- [ ] Unit test: write 0xFF46=0xC0; step 639 T-cycles; assert dmaActive true; step 1 more; assert dmaActive false; assert OAM contains source bytes

#### Task 7.1.6: FrameSink → ViewModel → ImageBitmap pipeline
**Owner:** Harry
**Domain:** android
**Status:** todo
**Dependencies:** Task 7.1.1, Task 4.1.2, Task 1.1.3
**ACs:**
- [ ] `EmulatorViewModel.onFrame(pixels)` sets `_frameState.value = pixels` (thread-safe via StateFlow)
- [ ] `GameScreen` collects `frameState` with `collectAsStateWithLifecycle()`; on each new `IntArray`, calls `androidBitmap.setPixels(it, 0, 160, 0, 0, 160, 144)` on the main thread
- [ ] `androidBitmap` and `imageBitmap` are `remember`-ed across recompositions (no per-frame allocation)
- [ ] Manual test on Pixel 6: 60 seconds of dmg-acid2 run; no OOM; no ANR; frame counter shows ≥57 fps

#### Task 7.1.7: dmg-acid2 CI test
**Owner:** Neville
**Domain:** qa
**Status:** todo
**Dependencies:** Task 7.1.4, Task 5.1.1, Task 5.1.2
**ACs:**
- [ ] `commonTest`: construct `EmulatorLoop` with dmg-acid2 ROM; run 200 frames; capture last frame via `RecordingFrameSink`
- [ ] SHA-256 hash of last frame pixels matches the reference value (computed from the published dmg-acid2 reference image)
- [ ] Test runs in `./gradlew :composeApp:jvmTest` without requiring a display

---

## Epic 8: Joypad
**Slice:** 3

### Story 8.1: Joypad input is registered within one emulated frame
**ACs:**
- [ ] Pressing A on touch overlay sets bit 0 of JoypadRegister low (active low) within one `runFrame()` call
- [ ] Joypad interrupt fires on any button press
- [ ] Both select bits low returns ANDed nibbles

#### Task 8.1.1: JoypadRegister
**Owner:** Hermione
**Domain:** backend
**Status:** todo
**Dependencies:** Task 2.1.3
**ACs:**
- [ ] `JoypadRegister.read()` returns correct byte for direction-only, button-only, and both-select-low cases
- [ ] `@Volatile var state: JoypadState`; `update(newState)` sets it; `read()` uses it
- [ ] Joypad interrupt flag (0xFF0F bit 4) set when any button transitions false→true in `update()`
- [ ] Unit tests: each button individually; direction nibble; button nibble; both select bits 0 (expect ANDed nibble); interrupt fires on press, not on hold or release

#### Task 8.1.2: InputSource interface + EmulatorLoop wiring
**Owner:** Hermione
**Domain:** backend
**Status:** todo
**Dependencies:** Task 8.1.1
**ACs:**
- [ ] `InputSource` interface declared in `commonMain`
- [ ] `EmulatorLoop` accepts an `InputSource`; before each `runFrame()`, reads `inputSource.state.value` and calls `joypadRegister.update()`
- [ ] Unit test: construct loop with a `FakeInputSource`; set A=true; run one frame; assert interrupt flag set

---

## Epic 9: Touch + Keyboard Controls
**Slice:** 3

### Story 9.1: User can control the game on Android with touch and Desktop with keyboard
**ACs:**
- [ ] D-pad, A, B, Start, Select touch areas registered on Android
- [ ] Arrow keys, Z, X, Enter, Right Shift mapped correctly on Desktop

#### Task 9.1.1: TouchInputSource (Android)
**Owner:** Harry
**Domain:** android
**Status:** todo
**Dependencies:** Task 8.1.2
**ACs:**
- [ ] `TouchInputSource` implements `InputSource`; emits `JoypadState` on touch down/up events
- [ ] Touch zones do not intercept touches intended for the game viewport

#### Task 9.1.2: Touch control overlay layout
**Owner:** Harry
**Domain:** android
**Status:** todo
**Dependencies:** Task 9.1.1
**ACs:**
- [ ] D-pad cluster in lower-left; A/B buttons in lower-right; Start/Select in centre-bottom
- [ ] Controls are semi-transparent and do not occlude the game viewport
- [ ] Touch targets meet minimum 48dp size on Android

#### Task 9.1.3: Pause + Resume
**Owner:** Harry
**Domain:** android
**Status:** todo
**Dependencies:** Task 4.1.4
**ACs:**
- [ ] Tapping pause icon calls `loopDriver.stop()`; `emulatorState` transitions to `Paused`
- [ ] "PAUSED" overlay appears centred on game viewport
- [ ] Tapping play icon calls `loopDriver.start()`; `emulatorState` transitions to `Running`
- [ ] Last rendered frame retained on screen while paused (no black screen)

#### Task 9.1.4: Menu sheet + Reset + Load New ROM
**Owner:** Harry
**Domain:** android
**Status:** todo
**Dependencies:** Task 9.1.3
**ACs:**
- [ ] Menu icon opens bottom sheet; emulator pauses automatically
- [ ] Resume closes sheet and resumes
- [ ] Reset cold-boots: `CpuInitialiser.applyPostBootState()` called; ROM reloaded from `filesDir` cache; loop restarts
- [ ] Load New ROM stops loop and returns to LauncherScreen (Unloaded state)
- [ ] Backdrop dismiss closes sheet and resumes

#### Task 9.1.5: LoopDriver Desktop actual + FilePicker Desktop actual
**Owner:** Arthur
**Domain:** infra
**Status:** todo
**Dependencies:** Task 3.1.2
**ACs:**
- [ ] `actual class LoopDriver` (JVM) uses `withFrameNanos` in a coroutine on `Dispatchers.Default`
- [ ] `actual suspend fun pickRomFile()` shows `java.awt.FileDialog` with `.gb` filter on `Dispatchers.IO`
- [ ] `./gradlew :composeApp:run` launches, loads a ROM, and renders frames without crash

#### Task 9.1.6: KeyboardInputSource (Desktop)
**Owner:** Harry
**Domain:** android
**Status:** todo
**Dependencies:** Task 8.1.2, Task 9.1.5
**ACs:**
- [ ] Arrow keys → D-pad; Z → A; X → B; Enter → Start; Right Shift → Select
- [ ] Key down sets button true; key up sets button false
- [ ] First-launch tooltip displayed showing key bindings; dismissed on any key press

---

## Epic 10: MBC1 Cartridge
**Slice:** 4

### Story 10.1: MBC1 ROMs load and bank-switch correctly
**ACs:**
- [ ] ROM bank switching selects correct 16 KB bank at 0x4000–0x7FFF
- [ ] Banks 0x00/0x20/0x40/0x60 remap to 0x01/0x21/0x41/0x61
- [ ] Mode 0/1 switching behaves correctly for RAM and ROM upper-bits selection

#### Task 10.1.1: Mbc1Cartridge
**Owner:** Hermione
**Domain:** backend
**Status:** todo
**Dependencies:** Task 2.1.2
**ACs:**
- [ ] `CartridgeLoader` returns `Mbc1Cartridge` for type bytes 0x01–0x03
- [ ] ROM bank 0 (0x0000–0x3FFF) always reads from physical bank 0
- [ ] ROM bank N (0x4000–0x7FFF) reads from the selected bank; write to 0x2000–0x3FFF sets bank number
- [ ] Bank 0x00 writes remapped to 0x01; 0x20→0x21; 0x40→0x41; 0x60→0x61
- [ ] Mode 0: upper 2 bits (0x4000–0x5FFF writes) extend ROM bank number; RAM fixed at bank 0
- [ ] Mode 1: upper 2 bits select RAM bank; ROM bank 0 in 0x0000–0x3FFF may be remapped
- [ ] RAM enable/disable via 0x0000–0x1FFF writes (0x0A enables)
- [ ] Unit tests: parametrised for each bank boundary and mode; remap quirk cases covered

---

## Epic 11: MBC3 + SaveStorage
**Slice:** 4

### Story 11.1: MBC3 ROMs run and battery saves persist
**ACs:**
- [ ] MBC3 ROM and RAM bank switching works
- [ ] RTC registers return correct elapsed time values
- [ ] Save file written on pause/close; loaded on next launch with the same ROM
- [ ] Corrupted save file (wrong length) is handled gracefully

#### Task 11.1.1: Mbc3Cartridge
**Owner:** Hermione
**Domain:** backend
**Status:** todo
**Dependencies:** Task 2.1.2, Task 1.1.2
**ACs:**
- [ ] ROM bank switching: 0x4000–0x7FFF selects bank 1–127 via writes to 0x2000–0x3FFF
- [ ] RAM bank switching: 0x0000–0x03 selects RAM bank; 0x08–0x0C selects RTC register
- [ ] RTC register reads: seconds/minutes/hours/day-lo/day-hi computed from `kotlinx-datetime` elapsed time since base epoch
- [ ] RTC halt (bit 6 of day-hi) stops RTC advancement
- [ ] Latch write sequence (0x00 then 0x01 to 0x6000–0x7FFF) latches current RTC values
- [ ] Unit tests: bank select; RTC read after known elapsed time; halt; latch

#### Task 11.1.2: SaveStorage Android actual
**Owner:** Harry
**Domain:** android
**Status:** todo
**Dependencies:** Task 11.1.1
**ACs:**
- [ ] `actual object SaveStorage` in `androidMain` reads/writes to `context.filesDir/saves/<romTitle>.sav`
- [ ] Write is atomic: write to `.tmp`, then `File.renameTo()` to `.sav`
- [ ] Load with wrong-length file: logs warning, returns null (caller zero-pads RAM)
- [ ] Unit test (Robolectric): save and load a 8192-byte payload; assert round-trip equality; assert `.tmp` file absent after successful save

#### Task 11.1.3: SaveStorage Desktop actual
**Owner:** Arthur
**Domain:** infra
**Status:** todo
**Dependencies:** Task 11.1.1
**ACs:**
- [ ] `actual object SaveStorage` in `jvmMain` reads/writes to `${user.home}/.kgbemu/saves/<romTitle>.sav`
- [ ] Atomic write via `.tmp` + rename
- [ ] Corrupted-length load returns null

#### Task 11.1.4: Save/load integration in EmulatorViewModel
**Owner:** Harry
**Domain:** android
**Status:** todo
**Dependencies:** Task 11.1.2
**ACs:**
- [ ] `onCleared()` calls `saveStorage.save(romTitle, cartridge.savableState())` if `cartridge.hasBattery()`
- [ ] On ROM load success, `saveStorage.load(romTitle)` called; if non-null, `cartridge.loadState(bytes)` called before loop starts
- [ ] Corruption-safe: if `loadState` throws, log and start with clean state

---

## Epic 12: iOS Platform
**Slice:** 5

### Story 12.1: Game runs on iOS with touch controls and file loading
**ACs:**
- [ ] `./gradlew assembleXCFramework` succeeds and Xcode project builds
- [ ] ROM can be loaded on iOS device via UIDocumentPickerViewController
- [ ] Touch controls work on iPhone

#### Task 12.1.1: LoopDriver iOS actual (CADisplayLink)
**Owner:** Sirius
**Domain:** ios
**Status:** todo
**Dependencies:** Task 3.1.2
**ACs:**
- [ ] `actual class LoopDriver` in `iosMain` uses `CADisplayLink` to call `runFrame()` each vsync
- [ ] `stop()` invalidates the display link; `start()` creates a new one
- [ ] `CADisplayLink` runs on the main run loop; `runFrame()` dispatched to a background queue to avoid blocking the UI thread
- [ ] Xcode project builds and runs on iOS Simulator without crash

#### Task 12.1.2: FilePicker iOS actual
**Owner:** Sirius
**Domain:** ios
**Status:** todo
**Dependencies:** none
**ACs:**
- [ ] `actual suspend fun pickRomFile()` presents `UIDocumentPickerViewController` for `public.data` UTType
- [ ] Selected file read via `Data(contentsOf:)` and returned as `ByteArray`
- [ ] Returns null if user cancels

#### Task 12.1.3: SaveStorage iOS actual
**Owner:** Sirius
**Domain:** ios
**Status:** todo
**Dependencies:** Task 11.1.1
**ACs:**
- [ ] `actual object SaveStorage` writes to `Documents/saves/<romTitle>.sav` via atomic write
- [ ] Corrupted-length load returns null

#### Task 12.1.4: TouchInputSource iOS
**Owner:** Sirius
**Domain:** ios
**Status:** todo
**Dependencies:** Task 8.1.2
**ACs:**
- [ ] Touch control overlay rendered via Compose Multiplatform on iOS
- [ ] Touch events update `JoypadState` correctly

#### Task 12.1.5: iOS CI job
**Owner:** Arthur
**Domain:** infra
**Status:** todo
**Dependencies:** Task 12.1.1
**ACs:**
- [ ] CI (GitHub Actions `macos-latest` runner) runs `./gradlew :composeApp:assembleXCFramework`
- [ ] Job runs on every PR; failure blocks merge
- [ ] Job placed after JVM test job (depends on it passing first)
