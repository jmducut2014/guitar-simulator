# 🎸 Guitar Simulator

> A small Java desktop app that lets you simulate playing a guitar with your
> keyboard. Built for people who can't use a real guitar but still want to
> practice chord shapes and hear how they sound.

![Java](https://img.shields.io/badge/Java-17%2B-orange)
![Platform](https://img.shields.io/badge/platforms-macOS%20%7C%20Windows%20%7C%20Linux-blue)
![License](https://img.shields.io/badge/license-MIT-green)

---

## 🚀 Install

### macOS / Linux (Terminal)
```bash
brew install openjdk@21
curl -L https://raw.githubusercontent.com/jmducut2014/guitar-simulator/main/install.sh | bash
```
The jar lands on your Desktop. Double-click it (or `java -jar ~/Desktop/guitar-simulator.jar`).

### Windows (PowerShell)
```powershell
winget install EclipseAdoptium.Temurin.21.JDK
irm https://raw.githubusercontent.com/jmducut2014/guitar-simulator/main/install.ps1 | iex
```
The jar lands on your Desktop. Double-click it (or `java -jar "$env:USERPROFILE\Desktop\guitar-simulator.jar"`).

> The install scripts verify Java 17+ is installed and tell you where to get it if not.

---

## ⌨️ Controls

| Key            | Action                                            |
|----------------|---------------------------------------------------|
| `Down arrow`   | Strum down (high E first)                         |
| `Up arrow`     | Strum up (low E first)                            |
| `0`..`9`       | Load the chord bound to that digit onto the fretboard |
| `Space`        | Rest the hand on the strings — instant palm-mute |

---

## 🎯 First run

1. Launch the app — you'll see an empty fretboard and an empty chord list.
2. Click **Add Chord / Bind Hotkey...**.
3. Type a name, pick a hotkey digit, then click on each string's row to
   cycle its state: `X` (muted) → `O` (open) → `1` → `2` → ... → `15` → `X`.
4. Click **Save**.
5. Press the hotkey on your keyboard — the fretboard lights up with your chord.
6. Hit `↓` to strum. Hit `Space` to mute. Hit `↑` to strum upward.

Want a head start? Click **Load Preset...** in the dialog to grab one of the
10 built-in chords: **C, G, D, A, E, Am, Em, Dm, F barre, B7**.

---

## 🔨 Build from source

```bash
git clone https://github.com/jmducut2014/guitar-simulator.git
cd guitar-simulator
./gradlew run          # run the app
./gradlew jar          # produce build/libs/guitar-simulator.jar
```

Or open the folder in **IntelliJ IDEA** (Java 17+ SDK), open
`src/main/java/guitar/Main.java`, click the green ▶.

---

## 🎚️ Audio details

- **Tuning:** E A D G B E (standard)
- **Synthesis:** Karplus-Strong plucked-string algorithm — same approach used
  for guitar synthesis since the 1980s, so the sound is rich rather than a
  sine beep
- **Format:** 44,100 Hz, 16-bit, signed, mono
- **Strum spread:** 25 ms between adjacent strings so direction is audible
- **Decay:** 0.996 per KS cycle — a long natural ring

---

## 📁 Project layout

```
guitar-simulator/
├── install.sh / install.ps1   ← one-line installers
├── publish.sh                  ← helper to upload a new release jar
├── build.gradle / settings.gradle
└── src/main/java/guitar/
    ├── Main.java               ← entry point
    ├── GuitarApp.java          ← main window + key bindings
    ├── GuitarPanel.java        ← read-only fretboard renderer
    ├── AddChordDialog.java     ← "Add Chord & Bind Hotkey" modal dialog
    ├── Chord.java / ChordLibrary.java
    ├── ChordListModel.java     ← Swing list model + cell renderer
    ├── AudioEngine.java        ← real-time 6-string mixer
    └── GuitarString.java       ← Karplus-Strong for one string
```

No third-party dependencies — only the standard Java library
(`javax.swing`, `java.awt`, `javax.sound.sampled`).

---

## 📝 License

MIT — do whatever you want, just don't blame me if you bust a string.
