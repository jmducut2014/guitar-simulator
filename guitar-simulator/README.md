# Guitar Simulator

A small Java/Swing desktop app that lets you simulate playing a guitar with
your keyboard. Designed for people who can't use a real guitar but still want
to practice chord shapes and hear how they sound.

Built and tested with **Java 17+** and **IntelliJ IDEA** on **macOS**.

---

## Features

- **Visual fretboard** that shows the active chord's finger positions, with
  `O` markers for open strings and `X` markers for muted strings.
- **Karplus-Strong string synthesis** — the same classic algorithm used for
  guitar synthesis since the 1980s, so the sound is reasonably realistic
  rather than a sine beep.
- **Strum direction** matters: down-strum plays high E first, up-strum plays
  low E first, just like a real guitar.
- **Palm-mute** on Space (immediately dampens all strings).
- **Custom chord editor** — click on each string row to cycle its state
  (`X` -> `O` -> `1` -> `2` -> ... -> `15` -> `X` again).
- **Preset chord library** — 10 common open-position chords you can load
  into the editor with one click (C, G, D, A, E, Am, Em, Dm, F barre, B7).
- **10 hotkey slots** (digits 0..9) — bind any chord you've defined to a
  hotkey and recall it instantly while playing.

---

## Keyboard controls

| Key            | Action                                                        |
|----------------|---------------------------------------------------------------|
| `Down arrow`   | Strum down (high E first)                                    |
| `Up arrow`     | Strum up (low E first)                                       |
| `0`..`9`       | Select the chord bound to that digit (load it onto the fretboard) |
| `Space`        | Rest the hand on the strings — instant palm-mute             |

The keyboard bindings work window-wide, so you don't need to click anything
to play once you've bound a few chords.

---

## How to run in IntelliJ IDEA (macOS)

### Option A — Open the folder directly

1. Launch IntelliJ IDEA.
2. `File` > `Open...`
3. Pick the `guitar-simulator` folder (the one containing this `README.md`).
4. IntelliJ detects the Gradle project (`build.gradle` + `settings.gradle`)
   and imports it automatically. If asked to use the bundled Gradle, say yes.
5. Make sure the project SDK is **Java 17 or later**:
   `File` > `Project Structure` > `Project` > `SDK` > 17 (or 21).
6. Open `src/main/java/guitar/Main.java` and click the green play triangle
   next to the `main` method.

### Option B — Run from the Gradle tool window

After importing as above:

1. Open the **Gradle** tool window (View > Tool Windows > Gradle).
2. `guitar-simulator` > `Tasks` > `application` > `run` — double-click.
3. Or in a terminal inside the project:
   ```bash
   ./gradlew run
   ```
   (On the first run, Gradle generates the `gradlew` wrapper if you don't
   have one already. If you don't, use IntelliJ's "Reload Gradle Project"
   button — the green elephant icon in the Gradle tool window — and then
   use the run configuration in Option A.)

### Option C — Build a runnable jar

```bash
./gradlew jar
```

Output appears at `build/libs/guitar-simulator.jar`. Run it with:

```bash
java -jar build/libs/guitar-simulator.jar
```

---

## macOS notes

- The first time the app runs and plays sound, macOS may prompt you to allow
  microphone access. **This is misleading** — the app only writes audio, it
  does not record. The prompt is triggered by the audio subsystem itself.
  Click **Allow** or just dismiss the dialog; audio output still works
  without it.
- On macOS 14+ (Sonoma and later), make sure IntelliJ is running under
  Java 17 or newer so it inherits the modern macOS look-and-feel.

---

## Using the app — first run

1. Launch the app. You'll see an empty fretboard and an empty chord list.
2. Click **Add Chord / Bind Hotkey...**.
3. Type a name (e.g., "My G").
4. Pick a hotkey digit (the dialog auto-selects the first free one).
5. Click on each string's row to set its fret:
   - high `e` (top)  -> set to `3`
   - `B`             -> set to `0` (open)
   - `G`             -> set to `0`
   - `D`             -> set to `0`
   - `A`             -> set to `2`
   - low `E`         -> set to `3`
6. Click **Save**. The chord appears in the right-hand list bound to key `0`
   (or whatever digit you picked).
7. Focus the main window and press `0` on your keyboard — the fretboard
   now shows your G chord. Press **Down arrow** to strum. Press **Space**
   to mute. Press **Up arrow** to strum upward.
8. Repeat to build up your own library of bound chords (e.g. key 1 = C,
   key 2 = D, etc.) and "play" them by switching hotkeys and strumming.

---

## Project layout

```
guitar-simulator/
├── README.md
├── build.gradle            # Gradle build config (no external deps)
├── settings.gradle        # Project name
└── src/main/java/guitar/
    ├── Main.java           # entry point — launches the GUI on the EDT
    ├── GuitarApp.java      # main window + key bindings
    ├── GuitarPanel.java    # read-only fretboard renderer
    ├── AddChordDialog.java # modal "Add Chord & Bind Hotkey" dialog
    ├── Chord.java          # immutable chord data (name + 6 frets + hotkey)
    ├── ChordLibrary.java   # hotkey slots + preset chord list
    ├── ChordListModel.java # Swing list model + cell renderer
    ├── AudioEngine.java    # 44.1kHz 16-bit mono real-time mixer
    └── GuitarString.java  # Karplus-Strong synthesis for one string
```

There are no third-party dependencies — only the standard Java library
(`javax.swing`, `java.awt`, `javax.sound.sampled`).

---

## Tuning and audio details

- Tuning: **E A D G B E** (standard guitar tuning).
  - Low E:  82.41 Hz
  - A:       110.00 Hz
  - D:       146.83 Hz
  - G:       196.00 Hz
  - B:       246.94 Hz
  - High E: 329.63 Hz
- Fret pitch = open string frequency × 2^(fret / 12).
- Audio format: **44,100 Hz, 16-bit, signed, little-endian, mono**.
- Strum spread: **25 ms** between adjacent strings — enough to hear the
  strum direction without sounding like a slow arpeggio.
- Decay factor per KS cycle: **0.996** (a long, natural ring).

---

## Troubleshooting

**"No sound when I strum"**

- Make sure your Mac's output is not muted and the app's volume is up.
- Make sure you selected a chord (press a 0-9 hotkey) before strumming.
  The status bar at the bottom will tell you if no chord is selected.
- Check the console for `LineUnavailableException` — this means the
  audio line was busy. Quit other audio apps (e.g. GarageBand) and retry.

**"Pressing 0-9 does nothing"**

- The window needs focus for key bindings to fire. Click anywhere on the
  main window (not in the dialog) to focus it.

**"The chord list is empty after starting"**

- That's expected — no chords are bound at startup. Click
  **Add Chord / Bind Hotkey...** to add your first one, or use the
  **Load Preset...** button inside that dialog to grab one of the
  built-in chords (C, G, D, A, E, Am, Em, Dm, F barre, B7).
