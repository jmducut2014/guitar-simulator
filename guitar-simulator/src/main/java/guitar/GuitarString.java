package guitar;

/**
 * A single physical guitar string rendered with the Karplus-Strong algorithm.
 *
 * <p>The classic KS plucked-string synthesis:
 * <ol>
 *   <li>Fill a buffer of length {@code sampleRate / frequency} with white noise.</li>
 *   <li>For each output sample, read the front of the buffer, then replace it
 *       with the average of itself and its neighbour, scaled by a small decay.</li>
 *   <li>Advance the read pointer around the buffer.</li>
 * </ol>
 *
 * <p>The result is a decaying, harmonically-rich waveform that sounds
 * convincingly like a plucked string. Decay close to 1.0 gives a long ring.
 *
 * <p>This class is intentionally <em>not</em> synchronized: only the audio
 * thread in {@link AudioEngine} ever touches a string instance. All mutations
 * come through the audio thread via its command queue.
 */
final class GuitarString {

    private static final double DECAY = 0.996;     // ring length — close to 1.0
    private static final double DAMPEN_ON_MUTE = 0.10; // fraction of amplitude left when palm-muted

    private double[] buffer;
    private int length;
    private int position;
    private double amplitude = 1.0;   // current gain (used for fade-out on mute)

    /** Pluck (or re-pluck) this string at the given frequency. */
    void pluck(double frequency, int sampleRate) {
        length = (int) Math.round(sampleRate / frequency);
        if (length < 2) length = 2;
        buffer = new double[length];
        for (int i = 0; i < length; i++) {
            buffer[i] = Math.random() * 2.0 - 1.0;
        }
        position = 0;
        amplitude = 1.0;
    }

    /** Produce one output sample. Returns 0 if the string is silent. */
    double tick() {
        if (buffer == null) {
            return 0.0;
        }
        double out = buffer[position] * amplitude;
        double next = (buffer[position] + buffer[(position + 1) % length]) * 0.5 * DECAY;
        buffer[position] = next;
        position = (position + 1) % length;
        return out;
    }

    /**
     * "Rest the hand on the strings": instantly drop amplitude and drain the
     * buffer so the string goes silent within a few ms, then fully clear it.
     */
    void mute() {
        amplitude = DAMPEN_ON_MUTE;
        if (buffer != null) {
            for (int i = 0; i < length; i++) buffer[i] *= 0.05;
        }
        // After this, the next few ticks will be near-silent, and the buffer
        // will naturally decay to nothing. We don't null it out so a strum
        // immediately afterwards works without allocation thrash.
    }

    /** Hard-stop: free the buffer entirely. Used on shutdown. */
    void silence() {
        buffer = null;
        length = 0;
        position = 0;
        amplitude = 0.0;
    }
}
