package guitar;

import java.util.Objects;

/**
 * A guitar chord.
 *
 * <p>A chord is described by the fret each string is held at. Strings are
 * indexed 0..5 from the highest pitch (high E) down to the lowest (low E),
 * which matches the way the fretboard is drawn on screen (high E on top).
 *
 * <p>Special fret values:
 * <ul>
 *   <li>{@code -1} = muted (do not play this string when strumming)</li>
 *   <li>{@code 0}  = open string (play with no finger pressed)</li>
 *   <li>{@code n > 0} = press fret n on this string</li>
 * </ul>
 */
public final class Chord {

    private final String name;
    private final int[] frets;        // length 6, indexed 0..5 (high E -> low E)
    private final int hotkey;         // 0..9, or -1 if not bound to a hotkey

    public Chord(String name, int[] frets, int hotkey) {
        this.name = Objects.requireNonNull(name, "name");
        if (frets.length != 6) {
            throw new IllegalArgumentException("frets must have 6 entries, got " + frets.length);
        }
        // Defensive copy.
        this.frets = frets.clone();
        this.hotkey = hotkey;
    }

    /** Display name shown in the UI. */
    public String getName() {
        return name;
    }

    /** The 6 fret values, high E first. Returned as a copy so callers can't mutate. */
    public int[] getFrets() {
        return frets.clone();
    }

    /** Fret value for a specific string (0..5, high E first). */
    public int getFret(int stringIndex) {
        return frets[stringIndex];
    }

    /** Hotkey digit 0..9, or -1 if no hotkey is bound. */
    public int getHotkey() {
        return hotkey;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(name);
        sb.append(" [");
        for (int i = 0; i < 6; i++) {
            sb.append(frets[i] < 0 ? "x" : frets[i]);
            if (i < 5) sb.append(' ');
        }
        sb.append("] key=");
        sb.append(hotkey < 0 ? '-' : hotkey);
        return sb.toString();
    }
}
