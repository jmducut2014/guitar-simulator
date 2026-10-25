package guitar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A small in-memory library of chords the user has defined or picked.
 *
 * <p>Stores up to 10 hotkey-bound chords (digits 0..9) plus a list of preset
 * chords for inspiration. The audio engine and GUI read from this class.
 */
public final class ChordLibrary {

    // Index 0..9 corresponds to hotkeys 0..9. null means "not bound".
    private final Chord[] hotkeySlots = new Chord[10];

    private final List<Chord> presets = new ArrayList<>();

    public ChordLibrary() {
        loadPresets();
    }

    /* ---------- hotkey-bound chords ---------- */

    public Chord getChordForHotkey(int digit) {
        if (digit < 0 || digit > 9) return null;
        return hotkeySlots[digit];
    }

    /** Binds a chord to a hotkey (0..9), replacing any existing binding. */
    public void bind(Chord chord) {
        int k = chord.getHotkey();
        if (k < 0 || k > 9) {
            throw new IllegalArgumentException("Hotkey must be 0..9, got " + k);
        }
        // Remove from any other slot first (a chord can only live in one slot).
        for (int i = 0; i < 10; i++) {
            Chord c = hotkeySlots[i];
            if (c != null && c.getName().equals(chord.getName())) {
                hotkeySlots[i] = null;
            }
        }
        hotkeySlots[k] = chord;
    }

    public void unbind(int digit) {
        if (digit < 0 || digit > 9) return;
        hotkeySlots[digit] = null;
    }

    public boolean isHotkeyTaken(int digit) {
        return digit >= 0 && digit <= 9 && hotkeySlots[digit] != null;
    }

    public List<Chord> boundChords() {
        List<Chord> out = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            if (hotkeySlots[i] != null) out.add(hotkeySlots[i]);
        }
        return Collections.unmodifiableList(out);
    }

    /* ---------- preset chords (no hotkeys, just for reference) ---------- */

    public List<Chord> presets() {
        return Collections.unmodifiableList(presets);
    }

    /**
     * A handful of common open-position chords, expressed with high E first
     * (string 0 = high E, string 5 = low E). -1 = muted, 0 = open, n = fret.
     *
     * <p>These serve as quick examples the user can load into the Add Chord
     * dialog to study or re-bind to a hotkey.
     */
    private void loadPresets() {
        // Order of strings: [high E, B, G, D, A, low E]
        presets.add(new Chord("C major",  new int[]{-1, 1, 0, 2, 3, 0}, -1));
        presets.add(new Chord("G major",  new int[]{ 3, 0, 0, 0, 2, 3}, -1));
        presets.add(new Chord("D major",  new int[]{ 2, 3, 2, 0,-1,-1}, -1));
        presets.add(new Chord("A major",  new int[]{ 0, 2, 2, 2, 0,-1}, -1));
        presets.add(new Chord("E major",  new int[]{ 0, 2, 2, 1, 0, 0}, -1));
        presets.add(new Chord("E minor",  new int[]{ 0, 0, 0, 2, 2, 0}, -1));
        presets.add(new Chord("A minor",  new int[]{ 0, 1, 2, 2, 0,-1}, -1));
        presets.add(new Chord("D minor",  new int[]{ 1, 3, 2, 0,-1,-1}, -1));
        presets.add(new Chord("F major (barre)", new int[]{ 1, 1, 2, 3, 3, 1}, -1));
        presets.add(new Chord("B7",        new int[]{ 2, 0, 2, 1, 2,-1}, -1));
    }
}
