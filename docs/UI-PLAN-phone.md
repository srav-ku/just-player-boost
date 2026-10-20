# Stremio Boost Player — Phone UI Plan

Status: planning only. No code changed. Baseline frozen at commit `35b88bc`, tag `ui-baseline-2026-10-20`.

---

## 0. Why the app needs a shell, not just a player

Today `PlayerActivity` is both the launcher icon and the `VIEW` intent target. That means tapping the app icon drops you into a file/open-document void — wrong for an app whose real job is **"the player Stremio hands streams to"**.

Target model:

- **HomeActivity** = launcher. Setup hub + status + shortcuts.
- **PlayerActivity** = unchanged intent surface (all `VIEW` filters stay), only receives streams.
- Settings / Subtitles = normal screens reached from Home.

Risk: if the launcher activity changes, Stremio handoff must still resolve. Mitigation: PlayerActivity keeps *every* intent-filter and `android:exported="true"`; HomeActivity only claims `MAIN` + `LAUNCHER`. Verified by a real Stremio → play round-trip after the shell lands (same method used for the sync fix).

---

## 1. Home screen

Single vertical scroll, dark, orange accent `#FB8C00`. No bottom nav — four destinations would be clutter for a companion app.

### 1.1 Header
- App name + build (`0.218`) small on the right.
- Status chip row (live):
  - **Sync**: `Signed in · chanducharan2030@gmail.com` or `Not signed in`.
  - **Last sync**: relative time (`2 min ago`) once a push has happened.

### 1.2 Primary action
- Large card: **Open Stremio** → launches `com.stremio.one`. This is the 80% path: you browse there, playback lands here.
- Secondary text link: **Play a local file** (keeps the old open-document path, not gone).

### 1.3 Setup checklist (collapses to a one-line "All set ✓" when complete)
Each row: state icon, title, one-line why, chevron/deep link.

| # | Item | Done when | Action |
|---|------|-----------|--------|
| 1 | Make this the player Stremio uses | User confirms | Opens a 3-step guide with screenshots: Stremio → Settings → Playback → Default player → External |
| 2 | Sign in to Stremio | authKey stored | Email/password inline, same login used for progress sync |
| 3 | Subtitle preferences | language chosen | Opens Subtitle screen |
| 4 | Battery optimisation off | `isIgnoringBatteryOptimizations` | Deep link to the system dialog (Stremio's local server must not be killed) |

### 1.4 Quick settings (real prefs only, no decoration)
- **Default subtitle language** (value shown on the right).
- **Auto-search subtitles** on/off.
- **Resume behaviour**: `Ask` / `Always resume` / `Always start`.
- **Boost speed memory**: shows last used boost (`2.0×`).

### 1.5 Tools
- **Test playback** — bundled 15 s sample; proves decoder + UI without Stremio.
- **Reset saved positions** — destructive, confirm dialog; clears `positionsJson`.
- **Export log** — share sheet with the last 2 000 lines (`JP_API`, `JP_SYNC`, `JP_SUB`).
- **Copy device info** — model, Android version, codec support (for bug reports).

### 1.6 About
- Version/build, GitHub commit, changelog, open-source licences.

### 1.7 First run
Same screen, but checklist items 1–3 presented as a 3-card **wizard** with Skip. Flag `onboardingDone`; afterwards it's a normal list. No fake progress, no marketing.

---

## 2. Settings map

Current `root_preferences.xml` already has the right features but flat and unstyled (General / Subtitle / Dangerous / Stremio sync). Reorganise into four screens with value summaries:

**Playback** — decoder (HW/SW), aspect ratio default, seek step, resume behaviour, keep screen on, boost default + hold sensitivity, audio preference (prefer commentary? no — prefer original language).

**Subtitles** — default language, secondary language, auto-search on open, "remember subtitle per title" (existing behaviour), style: size / bold / outline / background / vertical position, OpenSubtitles account (optional), clear subtitle cache.

**Sync** — Stremio email + password, sign-in state, "push now", last pushed value + title, sign out (clears authKey only, never positions).

**Device** — battery optimisation, PiP allowed, notifications permission, storage access for local files, diagnostics.

Visual: standard Material preference rows restyled — 56 dp min height, 16 dp side padding, section headers 12 sp letterspaced uppercase, switch accent orange, no boxed cards inside cards.

---

## 3. Player UI — phone version of the PC reference

Reference measured from your PC screenshot (1917×1078): accent **#FB8C00**, unplayed track **#414241**, title **#D0D0D0**, icons **#C9C9C9**, played-bar left margin ≈ 4.2 % of width, track 3 dp, knob ~14 dp.

### 3.1 Structure (landscape, edge-to-edge)

```
┌───────────────────────────────────────────────┐
│ ‹  Obsession                              ●   │  top row, 56dp, scrim 96dp
│                                               │
│                  ( video )                    │
│                                               │
│ ──────────●───────────────────────────────    │  seek, 20dp side pad
│ 00:15:22                            01:49:03  │  time row, 6dp below track
│                                               │
│  CC  ⏮        ▶            ⏱  ⛶  ⋯            │  action row, 56dp
└───────────────────────────────────────────────┘
```

### 3.2 Element table (position + size + behaviour)

| Element | PC reference | Phone spec | Why |
|---|---|---|---|
| Back | top-left, 24 dp | 40×40 dp hit box, icon 24 dp, 16 dp from edge | thumb + fat-finger tolerance |
| Title | 12–13 sp, grey | 13 sp/500, `#D0D0D0`, single line, ellipsis, max 55 % width | long torrent filenames |
| Seek track | thin line, full width minus 4 % margins | 3 dp track, 40 dp touch band, 20 dp side padding | drag needs vertical slop |
| Knob | 9 dp dot | 14 dp dot, 44 dp invisible touch target, grows to 18 dp on drag | finger size |
| Played fill | orange | `#FB8C00`; buffered portion `#6E6E6E` at 50 % | keep buffered info |
| Times | under the bar, both ends | 12 sp tabular, elapsed left / duration right, second line 6 dp under track | same model, bigger |
| Play/pause | centre | 32 dp icon, 56 dp touch, centre of action row | primary action |
| ±10 s | none (PC uses arrows) | **removed from the bar** → double-tap left/right thirds; icon flashes, `+10 s` label | reference is clean; gestures are faster |
| Volume | slider in-left cluster | **removed** → hardware keys + vertical drag on right third with HUD | wasted width on phone |
| Subtitles | CC-ish icon, right cluster | **moved to left cluster, first position** — most-used control in this app | one-hand reach |
| Audio track | waveform icon, right | left cluster, second | related to subtitles |
| Speed | circular arrow, right | right cluster, first; long-press anywhere still = boost hold | existing boost muscle memory |
| Aspect | box icon, right | right cluster, second | unchanged function |
| Overflow | `…` | right cluster, last → bottom sheet: playback speed, decoder, PiP, sync now, player settings | everything secondary |
| Loading | teal ring | 32 dp ring, `#FB8C00` at 70 %, no pulsing card | matches accent |

### 3.3 Gestures & states

- Tap: show/hide controls. Auto-hide 3 s; never hide while paused or while a sheet is open.
- Double-tap left/right third: −10 s / +10 s, ripple + label; respects seek-step pref.
- Drag on the bar: scrub + time bubble above the knob; commit on release.
- Vertical drag, right third: volume; left third: brightness (existing `BrightnessControl` reused, no new logic).
- Long-press anywhere: temporary boost hold + pill, unchanged.
- Buffering: ring + knob pulse; bar stays visible.
- Error/end: bar stays, play becomes replay, next-episode chip appears for series in the last 30 s (only if the intent carries a next item — otherwise hidden, no dead UI).
- Sheet open: controls stay, video dims 20 %, sheet radius 24 dp, orange selection.

### 3.4 Insets & orientation
- Landscape locked by default (pref to allow sensor). Cutout inset honoured on the side that has it; gesture bar 24 dp clearance under the action row.
- Portrait (if enabled): video 16:9 letterboxed top, same control stack below, seek full width.

### 3.5 Type & colour tokens
```
bg          #000000        accent        #FB8C00
surface     #121212        track         #414241
text        #FFFFFF 92%    text-sec      #B3B3B3
scrim       #000000 → 0    icon          #E6E6E6
radius      24dp sheets    ripple        accent 12%
```

---

## 4. Build order (no logic edits until step 5)

1. **Tokens** — `colors.xml`, `dimens.xml`, type styles. Nothing else changes.
2. **Control bar restyle** — same ids, same click listeners: move subtitle/audio left, drop rew/ffwd from the bar, restyle progress + times, new knob drawable, scrim weights.
3. **Sheets restyle** — SubtitleSheet / TrackSheet / SpeedSheet / AspectSheet / PremiumSheet: radius, colours, row height.
4. **Home shell** — HomeActivity + onboarding + checklist + tools; PlayerActivity loses only the LAUNCHER category (keeps every VIEW filter).
5. **Settings reorganisation** — reuse existing preference keys; add only new ones actually needed.
6. **Gestures** (optional, touches PlayerActivity) — double-tap seek, brightness/volume drags, auto-hide timer.

Each step is a separate build + screenshot review. Nothing ships until a real Stremio stream plays with the new UI.

---

## 5. Verification matrix

- States: loading, playing, paused, controls hidden, scrubbing, sheet open, error, end.
- Screens: home first-run, home configured, checklist complete, each settings screen, subtitle screen.
- Device: SM-M336BU 1080×2408, landscape + portrait.
- Regression: Stremio handoff plays; position push still logs `datastorePut success`; subtitle auto-load still works; boost hold still works.

---

## 6. Decisions needed

1. Keep the visible ±10 s buttons, or double-tap only?
2. Volume slider on the phone, or hardware keys only?
3. Home primary action = "Open Stremio" (recommended), or should the app also show a local-library entry first?
