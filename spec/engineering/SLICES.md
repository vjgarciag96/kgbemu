# Vertical Slices — kgbemu

## Spikes

| ID | Question | Blocks | Urgency |
|---|---|---|---|
| SPIKE-001 | Do all three `expect/actual` stubs (`LoopDriver`, `FilePicker`, `SaveStorage`) compile cleanly on Android, Desktop, and iOS targets? | Slice 1 | Run immediately — gates all implementation |
| SPIKE-002 | Does `Bitmap.setPixels()` + `asImageBitmap()` on Android and `Bitmap.installPixels()` on Desktop both accept the same `IntArray(160×144)` ARGB layout without a copy? | Slice 2 | Run during Slice 1 walking skeleton |
| SPIKE-003 | Does a PPU implementation with fixed 172 T-cycle Drawing mode pass the dmg-acid2 reference test, or is variable-width Drawing required? | Slice 2 | Run before PPU rendering task starts |

---

## Slice 1: Walking Skeleton
**Delivers:** A user loads a `.gb` ROM on Android and sees a blank (grey) 160×144 game screen — CPU is ticking real opcodes behind it. No graphics yet.
**Why here:** Proves the entire pipeline end-to-end on a real device: file acquisition → header parsing → emulator init → loop timing → Compose rendering → ViewModel lifecycle. Every subsequent slice builds on this without re-proving the pipeline.
**Epics:** Epic 1 (Build Foundation), Epic 2 (Android Walking Skeleton)
**Depends on:** none
**Unblocks:** Slice 2
**Spikes required:** SPIKE-001 (must pass before any task ships), SPIKE-002 (validated within this slice)

---

## Slice 2: Graphics
**Delivers:** A user sees actual game graphics — background tiles, window layer, sprites — rendered at full speed. The dmg-acid2 test ROM passes in CI.
**Why here:** Without visible output, nothing else is verifiable. Timer and PPU are independent of each other's implementation but both must land before EmulatorLoop can produce correct frames.
**Epics:** Epic 3 (Test Infrastructure), Epic 4 (Timer), Epic 5 (PPU + Frame Pipeline)
**Depends on:** Slice 1
**Unblocks:** Slice 3
**Spikes required:** SPIKE-002 (if not resolved in Slice 1), SPIKE-003

---

## Slice 3: Controls + Desktop
**Delivers:** A user can play the game with touch controls on Android and keyboard controls on Desktop. Pause, reset, and the menu sheet all work.
**Why here:** The game is viewable after Slice 2 but uncontrollable. Joypad, pause/reset, and the Desktop target are independent of MBC or save logic.
**Epics:** Epic 6 (Joypad), Epic 7 (Pause + Reset), Epic 8 (Desktop)
**Depends on:** Slice 2
**Unblocks:** Slice 4

---

## Slice 4: Banked ROMs + Saves
**Delivers:** A user can load and play MBC1 and MBC3 cartridges (Pokémon, Zelda, Tetris DX etc.). MBC3 battery saves are persisted between sessions.
**Why here:** Most commercial Game Boy games use MBC1 or MBC3. ROM-only games (Tetris, Dr. Mario) are playable after Slice 3; this slice unlocks the broader library.
**Epics:** Epic 9 (MBC1), Epic 10 (MBC3 + SaveStorage)
**Depends on:** Slice 3
**Unblocks:** Slice 5

---

## Slice 5: iOS
**Delivers:** The game runs on iOS (iPhone/iPad) with touch controls and file loading.
**Why here:** The shared KMP code is complete after Slice 4. iOS only requires platform `actual` implementations and CI wiring.
**Epics:** Epic 11 (iOS Platform)
**Depends on:** Slice 4
**Unblocks:** v1 complete
