# kgbemu — AI Context

kgbemu is a Game Boy (DMG-01) emulator built with Kotlin Multiplatform and Compose Multiplatform. A user loads a `.gb` ROM file and plays the game with on-screen or keyboard controls. The CPU core is complete; the remaining work is graphics (PPU), timing, joypad, ROM banking, and UI integration across Android, Desktop (JVM), and iOS.

## Key files

| File | Purpose |
|---|---|
| `spec/engineering/BACKLOG.md` | Task status — read this first to know current build state |
| `spec/engineering/TECH_SPEC.md` | Authoritative technical spec |
| `spec/engineering/SLICES.md` | Vertical slices and build order |
| `spec/design/DESIGN.md` | UI/UX spec — screen states, interaction model |
| `spec/product/PRD.md` | Product requirements and ACs |

## Stack

- Language: Kotlin (Multiplatform)
- UI: Compose Multiplatform (shared across all targets)
- DI: Hilt (Android only — `androidMain`); manual construction on Desktop/iOS
- Async: Kotlin Coroutines + StateFlow
- Date/time: kotlinx-datetime (for MBC3 RTC)
- Testing: kotlin.test in `commonTest`; JUnit4 on Android
- Platforms: Android (primary), Desktop JVM (`./gradlew run`), iOS (Xcode)

## Module layout

```
composeApp/src/
  commonMain/kotlin/com/vicgarci/kgbem/
    cpu/              # Complete: CPU, Registers, MemoryBus, Instructions, Decoder
    cartridge/        # Cartridge interface, RomOnly/MBC1/MBC3, CartridgeLoader
    ppu/              # Ppu, FrameSink, double-buffer
    timer/            # Timer (DIV/TIMA/TMA/TAC)
    joypad/           # JoypadRegister, InputSource, JoypadState
    emulator/         # EmulatorLoop, CpuInitialiser
    App.kt            # Compose entry point (shared)
  androidMain/        # EmulatorViewModel (@HiltViewModel), LoopDriver, FilePicker, SaveStorage
  jvmMain/            # LoopDriver, FilePicker, SaveStorage (Desktop)
  iosMain/            # LoopDriver (CADisplayLink), FilePicker, SaveStorage
  commonTest/         # Unit + integration tests; RecordingFrameSink; test ROMs
```

## Workflows

| Command | What it does |
|---|---|
| `/build` | Pick next available tasks from BACKLOG.md, implement + review, repeat |
| `/implement` | Implement a single task TDD in character as the assigned owner |
| `/review` | Review an open PR with domain-primary + cross-domain reviewers |
| `/commit` | Generate a commit message consistent with repo style |

## Build rules

- BACKLOG.md is the source of truth for task status. Update it immediately when a task changes state.
- All tasks implement TDD: write failing tests first, then implementation.
- PRs contain only implementation changes — no BACKLOG.md, no spec files.
- Branch from `origin/main` (push main first before branching).
- Commit messages: short imperative summary, capitalised (e.g. "Add Cartridge interface").
- `EmulatorViewModel` lives in `androidMain` — Hilt annotations are Android-only.
- The common emulator loop (`EmulatorLoop`) makes no platform calls.
- ROM bytes only — the common module never receives a URI, File path, or platform handle.
- `FrameSink.onFrame()` emits via `StateFlow.value` (not `emit()`); ImageBitmap conversion on main thread only.
- Save files: atomic write via `.tmp` + rename; app-private dirs only; filename sanitised from ROM title.
