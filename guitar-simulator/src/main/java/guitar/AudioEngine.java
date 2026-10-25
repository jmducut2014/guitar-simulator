package guitar;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Real-time audio engine for the guitar simulator.
 *
 * <p>Runs a dedicated audio thread that continuously mixes the six
 * {@link GuitarString}s and writes 16-bit mono PCM to a
 * {@link SourceDataLine}. GUI threads never touch strings directly;
 * they enqueue commands ({@link #strum(int[], boolean)}, {@link #mute()},
 * {@link #silenceAll()}) that the audio thread runs on its own clock.
 *
 * <p>Strums are "spread" across the strings with a small delay so the
 * strum actually sounds like a strum rather than a simultaneous block.
 */
public final class AudioEngine {

    public static final int SAMPLE_RATE = 44100;

    // Standard tuning, low E to high E. We index strings 0..5 from HIGH E to
    // LOW E (matches the visual fretboard top-to-bottom), so this array is in
    // reverse order compared to a chord diagram.
    private static final double[] OPEN_FREQS = {
            329.63, // string 0 = high E
            246.94, // string 1 = B
            196.00, // string 2 = G
            146.83, // string 3 = D
            110.00, // string 4 = A
            82.41,  // string 5 = low E
    };

    private static final int STRUM_STEP_MS = 25;     // gap between adjacent strings
    private static final int BUFFER_SAMPLES = 1024;  // per write to the line

    private final GuitarString[] strings = new GuitarString[6];
    private final SourceDataLine line;
    private final Thread audioThread;
    private final ConcurrentLinkedQueue<Runnable> commands = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, r -> {
        Thread t = new Thread(r, "guitar-scheduler");
        t.setDaemon(true);
        return t;
    });

    private volatile boolean running = true;

    public AudioEngine() {
        for (int i = 0; i < 6; i++) {
            strings[i] = new GuitarString();
        }

        AudioFormat format = new AudioFormat(
                SAMPLE_RATE,
                16,      // bits per sample
                1,       // mono
                true,    // signed
                false    // little-endian
        );

        try {
            line = AudioSystem.getSourceDataLine(format);
            line.open(format, BUFFER_SAMPLES * 4);   // generous buffer to avoid underruns
            line.start();
        } catch (LineUnavailableException e) {
            throw new RuntimeException("Could not open audio line: " + e.getMessage(), e);
        }

        audioThread = new Thread(this::audioLoop, "guitar-audio");
        audioThread.setDaemon(true);
        audioThread.start();
    }

    /* ---------- public API (called from the EDT) ---------- */

    /**
     * Strum all 6 strings with the given fret positions. Strings marked -1
     * (muted) are skipped. When {@code down} is true, strum from high E
     * (top string) to low E (bottom) — the natural down-strum direction.
     * When {@code down} is false, strum in reverse.
     */
    public void strum(int[] frets, boolean down) {
        if (frets.length != 6) {
            throw new IllegalArgumentException("frets must have 6 entries");
        }
        // Always enqueue to keep things on the audio thread's clock. We use
        // the scheduler to stagger each pluck by STRUM_STEP_MS.
        int order = down ? 0 : 5;
        for (int i = 0; i < 6; i++) {
            int stringIdx = down ? i : (5 - i);
            int fret = frets[stringIdx];
            if (fret < 0) continue;        // muted
            double freq = OPEN_FREQS[stringIdx] * Math.pow(2.0, fret / 12.0);
            int delay = (down ? i : (5 - i)) * STRUM_STEP_MS;
            final int idx = stringIdx;
            final double f = freq;
            scheduler.schedule(() -> commands.add(() -> strings[idx].pluck(f, SAMPLE_RATE)),
                    delay, TimeUnit.MILLISECONDS);
        }
    }

    /** Palm-mute: instantly damp all strings. */
    public void mute() {
        commands.add(() -> {
            for (GuitarString s : strings) s.mute();
        });
    }

    /** Free all string buffers (used on shutdown). */
    public void silenceAll() {
        commands.add(() -> {
            for (GuitarString s : strings) s.silence();
        });
    }

    /** Stop the audio thread and close the line. Safe to call once at shutdown. */
    public void shutdown() {
        running = false;
        scheduler.shutdownNow();
        try {
            audioThread.join(500);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        line.drain();
        line.close();
    }

    /* ---------- audio thread ---------- */

    private void audioLoop() {
        byte[] out = new byte[BUFFER_SAMPLES * 2]; // 16-bit mono -> 2 bytes/sample

        while (running) {
            // Drain any queued commands first so the latest state is applied
            // before we render this block.
            Runnable cmd;
            while ((cmd = commands.poll()) != null) {
                try {
                    cmd.run();
                } catch (Throwable t) {
                    System.err.println("Audio command failed: " + t);
                }
            }

            for (int i = 0; i < BUFFER_SAMPLES; i++) {
                double sample = 0.0;
                for (int s = 0; s < 6; s++) {
                    sample += strings[s].tick();
                }
                // Normalize roughly to [-0.5, 0.5] to avoid clipping.
                sample *= 0.15;

                short val = (short) Math.max(Short.MIN_VALUE,
                        Math.min(Short.MAX_VALUE, sample * Short.MAX_VALUE));
                out[i * 2]     = (byte) (val & 0xff);
                out[i * 2 + 1] = (byte) ((val >> 8) & 0xff);
            }

            line.write(out, 0, out.length);
        }
    }
}
