package guitar;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;

/**
 * Renders a guitar fretboard with the 6 strings horizontal and the frets
 * vertical. The currently active chord's finger positions are drawn as
 * dots, with "O" markers for open strings and "X" markers for muted ones.
 *
 * <p>Strings are drawn top-to-bottom: high E (string 0) at the top, low E
 * (string 5) at the bottom — which matches how a right-handed player
 * sees the neck when looking down at it.
 */
public final class GuitarPanel extends JPanel {

    public static final int NUM_STRINGS = 6;
    public static final int NUM_FRETS = 6;       // visible frets (0 = open area + 5 frets)

    private static final Color WOOD_COLOR = new Color(139, 90, 43);
    private static final Color FRET_COLOR = new Color(180, 180, 180);
    private static final Color STRING_COLOR = new Color(220, 220, 220);
    private static final Color FINGER_COLOR = new Color(80, 140, 220);
    private static final Color OPEN_COLOR = new Color(80, 200, 80);
    private static final Color MUTE_COLOR = new Color(220, 80, 80);
    private static final Color LABEL_COLOR = new Color(230, 230, 230);

    private static final String[] STRING_NAMES = {"e", "B", "G", "D", "A", "E"};

    private int[] frets = new int[]{-1, -1, -1, -1, -1, -1};

    public GuitarPanel() {
        setPreferredSize(new Dimension(560, 360));
        setBackground(Color.DARK_GRAY);
    }

    /** Set the chord to display. Pass an array of 6 frets (high E first). */
    public void setChord(int[] frets) {
        if (frets.length != 6) throw new IllegalArgumentException("need 6 frets");
        this.frets = frets.clone();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Layout: leave room for string-name labels on the left, and a
        // marker column on the right showing open/mute state.
        int leftPad = 60;
        int topPad = 40;
        int bottomPad = 30;
        int rightPad = 40;
        int boardX = leftPad;
        int boardY = topPad;
        int boardW = w - leftPad - rightPad;
        int boardH = h - topPad - bottomPad;

        // The "open string" column sits just to the left of fret 1.
        int openColX = boardX + boardW / 8;
        int fretsStartX = openColX + boardW / 16;
        int fretsEndX = boardX + boardW;
        int fretSpacing = (fretsEndX - fretsStartX) / (NUM_FRETS - 1);

        int stringTop = boardY + 20;
        int stringBottom = boardY + boardH - 20;
        int stringSpacing = (stringBottom - stringTop) / (NUM_STRINGS - 1);

        // Fretboard background (wood).
        g2.setColor(WOOD_COLOR);
        g2.fillRect(openColX - 10, boardY, fretsEndX - openColX + 10, boardH);

        // Headstock area (the open-string column).
        g2.setColor(new Color(60, 40, 20));
        g2.fillRect(boardX, boardY, openColX - boardX, boardH);

        // Nut (white bar between headstock and fretboard).
        g2.setColor(Color.WHITE);
        g2.fillRect(openColX - 3, boardY, 4, boardH);

        // Frets.
        g2.setColor(FRET_COLOR);
        Stroke fretStroke = new BasicStroke(2f);
        g2.setStroke(fretStroke);
        for (int f = 0; f < NUM_FRETS; f++) {
            int x = fretsStartX + f * fretSpacing;
            g2.drawLine(x, boardY, x, boardY + boardH);
        }

        // Strings.
        g2.setStroke(new BasicStroke(1.5f));
        for (int s = 0; s < NUM_STRINGS; s++) {
            int y = stringTop + s * stringSpacing;
            // Slightly thicker lower strings (low E is the heaviest).
            float thickness = 1.2f + (NUM_STRINGS - 1 - s) * 0.4f;
            g2.setStroke(new BasicStroke(thickness));
            g2.setColor(STRING_COLOR);
            g2.drawLine(boardX, y, fretsEndX, y);
        }

        // String name labels (left side).
        g2.setColor(LABEL_COLOR);
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        for (int s = 0; s < NUM_STRINGS; s++) {
            int y = stringTop + s * stringSpacing;
            int labelX = boardX + 25;
            g2.drawString(STRING_NAMES[s], labelX - 6, y + 5);
        }

        // Fret position numbers (top).
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        for (int f = 1; f < NUM_FRETS; f++) {
            int x = fretsStartX + f * fretSpacing;
            g2.drawString(String.valueOf(f), x - 4, boardY - 10);
        }

        // Now draw finger / open / mute markers per string.
        for (int s = 0; s < NUM_STRINGS; s++) {
            int y = stringTop + s * stringSpacing;
            int fret = frets[s];

            if (fret < 0) {
                // Muted -> "X" in the open-string column.
                g2.setColor(MUTE_COLOR);
                g2.setStroke(new BasicStroke(2f));
                int x = openColX - 25;
                g2.drawLine(x - 6, y - 6, x + 6, y + 6);
                g2.drawLine(x - 6, y + 6, x + 6, y - 6);
            } else if (fret == 0) {
                // Open -> "O" in the open-string column.
                g2.setColor(OPEN_COLOR);
                g2.setStroke(new BasicStroke(2f));
                Ellipse2D circle = new Ellipse2D.Double(openColX - 25 - 6, y - 6, 12, 12);
                g2.draw(circle);
            } else {
                // Pressed -> a filled dot at the appropriate fret column.
                int fretIdx = Math.min(fret, NUM_FRETS - 1);
                int x = fretsStartX + fretIdx * fretSpacing - fretSpacing / 2;
                g2.setColor(FINGER_COLOR);
                Ellipse2D dot = new Ellipse2D.Double(x - 10, y - 10, 20, 20);
                g2.fill(dot);
                // Show the fret number on the dot.
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 11));
                String label = String.valueOf(fret);
                int labelW = g2.getFontMetrics().stringWidth(label);
                g2.drawString(label, x - labelW / 2, y + 4);
            }
        }
    }
}
