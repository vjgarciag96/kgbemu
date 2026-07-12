# Design — kgbemu UI

## Overview

The UI is minimal by design. The game output fills the available screen real estate; controls and chrome are secondary. There are two screens: a launcher and a game screen.

## Emulator State Machine (UI-visible states)

```
Unloaded → [user picks file] → Loading → Ready → Running
                                            ↓
                                          Error
Running ⇄ Paused
Running/Paused → [Reset] → Ready → Running
Running/Paused → [Load New ROM] → Unloaded
```

| State | Description |
|---|---|
| Unloaded | No ROM selected. Launcher screen shown. |
| Loading | ROM bytes being read and parsed. Spinner shown. |
| Ready | ROM parsed and valid. Emulator initialised. Transitions to Running immediately. |
| Running | Emulator loop active. Game screen shown. |
| Paused | Loop suspended. Game screen with PAUSED overlay. |
| Error | ROM failed to load. Error message shown on Launcher screen. |

## Screen 1: Launcher

**Purpose:** Let the user pick a ROM and start playing.

**States:**

- **Unloaded (default):** kgbemu wordmark centred. Single "Open ROM" button below.
- **Loading:** Spinner replaces the wordmark. "Open ROM" button disabled.
- **Error:** Error message below the button (see ROM Error Taxonomy in PRD). "Try Again" button re-opens the file picker. Three distinct messages depending on error type.

**Interactions:**
- "Open ROM" → platform file picker (`.gb` filter) → on success → Loading → Ready → transitions automatically to Game Screen (Running state).
- On error → stays on Launcher, shows Error state with specific message.
- "Try Again" → re-opens file picker.

## Screen 2: Game Screen

**Purpose:** Display the running game and provide controls.

**Layout:**
- **Game viewport:** 160×144 pixels, integer-scaled to fill available width (black letterbox bars as needed to maintain aspect ratio).
- **Touch control overlay (Android/iOS):** D-pad cluster, lower-left. A/B buttons, lower-right. Start/Select as small buttons, centre-bottom. Semi-transparent. Does not occlude the game viewport.
- **Header bar:** Menu/back icon (top-left), Pause/Play toggle (top-right).

**States:**

- **Running:** Emulator loop active. Frames rendered at 59.73 Hz. Controls visible.
- **Paused:** Loop suspended. "PAUSED" label centred on the game viewport (white text, semi-transparent black background). Pause/Play icon shows Play. Controls visible.
- **Menu:** Triggered by menu/back icon. Bottom sheet slides up. Emulator pauses while menu is open. Options: Resume, Reset, Load New ROM. Backdrop dismisses the sheet and resumes.

**Interactions:**
- Pause/Play toggle → toggles Running ↔ Paused.
- Menu icon → opens Menu sheet (pauses if running).
- Menu → Resume → closes sheet, resumes.
- Menu → Reset → cold reboot: reinitialise all state, reload ROM, transition to Running.
- Menu → Load New ROM → close Game Screen, return to Launcher (Unloaded state).

## Platform Notes

- **Desktop / Web:** No touch controls rendered. On first launch, a dismissable tooltip shows keyboard bindings: Arrow keys = D-pad, Z = A, X = B, Enter = Start, Right Shift = Select.
- **Android / iOS:** Touch controls only. Keyboard not assumed.
- All text is English only. No i18n in v1.

## Design Constraints

- Background: black. Text: white. No decorative colour — the game's own output fills the screen.
- No animations except the game itself and the menu bottom sheet slide.
- Compose Multiplatform shared UI across all platforms.
- Integer scaling only for the game viewport (no bilinear/nearest-neighbour interpolation artefacts).
