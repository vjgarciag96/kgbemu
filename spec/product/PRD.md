# PRD — kgbemu: Game Boy Emulator

## Overview

kgbemu is a Game Boy (DMG-01) emulator built on a single shared Kotlin Multiplatform codebase. The user loads a `.gb` ROM file and plays the game with on-screen or keyboard controls. The emulator must run commercially-released Game Boy games at full speed with accuracy sufficient for normal gameplay.

## Target Platforms (v1)

- **Android** — primary target. Touch controls. File picker via system intent.
- **Desktop (JVM)** — developer use via `./gradlew run`. Keyboard controls. No packaging or distribution in v1.
- **iOS** — included via KMP/Xcode since the shared code is identical. Touch controls.
- **Web** — deferred to v2. Excluded from v1 scope.

## Goals

- Run Game Boy ROM files accurately on Android, Desktop, and iOS from a single shared Kotlin codebase.
- Achieve playable frame rate (59.73 Hz) on mid-range Android and desktop hardware.
- Support the most common cartridge types (ROM-only, MBC1, MBC3) to cover the majority of commercial games.
- Provide a minimal but complete UI: ROM picker, game display, controls, and pause/reset.

## Non-Goals (v1)

- Sound (APU) — deferred post-MVP.
- Game Boy Color (CGB) — DMG-01 only.
- Link cable / serial emulation.
- Save states — only battery-backed SRAM saves for MBC3.
- Multiplayer.
- Web target.
- Desktop distribution packaging (no DMG/MSI/DEB).

## Boot ROM

The Nintendo boot ROM is not included. On startup, CPU registers and memory are initialised to the values they hold after the boot ROM completes on real hardware (post-boot state, as documented in Pan Docs). This skips the Nintendo logo scroll. A future milestone may display a custom kgbemu splash screen in its place.

## ROM Error Taxonomy

Three distinct error classes, each with its own UI state:

| Error | Cause | User-visible message |
|---|---|---|
| `Truncated` | File is shorter than the minimum valid header | "ROM file is incomplete or corrupted." |
| `InvalidHeader` | Header checksum fails or Nintendo logo bytes don't match | "ROM header is invalid. The file may be corrupted." |
| `UnsupportedMapper` | Cartridge type byte indicates an MBC not supported in v1 | "This cartridge type is not supported yet." |

## User Stories

### US-1: Load a ROM
As a user, I can open a `.gb` ROM file from local storage so that I can start playing a game.

**Acceptance Criteria:**
- [ ] A file picker allows selecting `.gb` files on Android and Desktop.
- [ ] A `Truncated` ROM shows the "incomplete or corrupted" error message.
- [ ] An `InvalidHeader` ROM shows the "header invalid" error message.
- [ ] An `UnsupportedMapper` ROM shows the "not supported" error message.
- [ ] A valid ROM is loaded into memory and the emulator transitions to Ready state automatically.

### US-2: Play a Game
As a user, I can see the game running on screen at full speed so that the game is playable.

**Acceptance Criteria:**
- [ ] The 160×144 LCD output advances exactly 70,224 T-cycles per frame.
- [ ] Background tiles, the window layer, and sprites render correctly — verified by dmg-acid2 test ROM passing its reference frame in CI.
- [ ] The game does not freeze or crash on the Tetris or Dr. Mario ROMs within 5 minutes of play (manual verification; commercial ROMs not included in CI).
- [ ] Playable frame rate on a Pixel 6 / Android 12 device: ≥ 57 fps sustained over 60 seconds.

### US-3: Control the Game
As a user, I can press the D-pad and A/B/Start/Select buttons to interact with the game.

**Acceptance Criteria:**
- [ ] On-screen touch controls work on Android and iOS.
- [ ] Keyboard controls work on Desktop (arrow keys = D-pad, Z = A, X = B, Enter = Start, Right Shift = Select).
- [ ] Joypad input is registered within one emulated frame (≤ 70,224 T-cycles) of the button press.

### US-4: Pause and Reset
As a user, I can pause the emulator and reset to the beginning of the game.

**Acceptance Criteria:**
- [ ] Pause suspends the emulator loop; the screen retains the last rendered frame.
- [ ] Reset performs a cold reboot: reinitialises all hardware state and reloads the ROM from byte 0.

## Known Risks

- **PPU timing accuracy:** Incorrect scanline mode timing causes graphical glitches on games that rely on mid-scanline LCDC/STAT effects.
- **MBC3 RTC complexity:** The real-time clock adds non-trivial state to save and restore.
- **Frame buffer → Compose:** Writing raw ARGB pixels into a Compose surface at 60 fps without GC pressure needs early validation.
- **iOS CI:** `assembleXCFramework` must run before the Xcode build step; CI job ordering must be explicit.
