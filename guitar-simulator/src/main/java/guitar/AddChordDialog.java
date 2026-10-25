package guitar;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Modal dialog used to define a new chord and bind it to a hotkey (0..9).
 *
 * <p>The user:
 * <ol>
 *   <li>Types a chord name.</li>
 *   <li>Picks a hotkey digit (0..9).</li>
 *   <li>Clicks on each string's row to set its state: muted (X) -> open (O)
 *       -> fret 1 -> fret 2 -> ... -> fret 15 -> back to muted.</li>
 *   <li>Presses Save.</li>
 * </ol>
 *
 * <p>Clicking anywhere along a string's row cycles that string's fret value
 * through the sequence: muted (-1) -> open (0) -> 1 -> 2 -> ... -> 15 -> -1.
 */
public final class AddChordDialog extends JDialog {

    private static final int MAX_FRET = 15;

    private final EditableFretboard fretboard = new EditableFretboard();
    private final JTextField nameField = new JTextField("My chord", 16);
    private final JComboBox<Integer> hotkeyBox = new JComboBox<>();
    private final ChordLibrary library;
    private final Runnable onSaved;     // called after a successful save so the parent can refresh

    public AddChordDialog(java.awt.Frame owner, ChordLibrary library, Runnable onSaved) {
        super(owner, "Add Chord & Bind Hotkey", true);
        this.library = library;
        this.onSaved = onSaved;
        buildUi();
        pack();
        setLocationRelativeTo(owner);
    }

    private void buildUi() {
        setLayout(new BorderLayout(8, 8));

        // Top: name + hotkey.
        JPanel top = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 8));
        top.add(new JLabel("Name:"));
        top.add(nameField);
        for (int i = 0; i <= 9; i++) hotkeyBox.addItem(i);
        top.add(new JLabel("Hotkey:"));
        top.add(hotkeyBox);
        // Pre-pick the first free slot.
        for (int i = 0; i <= 9; i++) {
            if (!library.isHotkeyTaken(i)) {
                hotkeyBox.setSelectedItem(i);
                break;
            }
        }
        top.add(new JLabel("  (0-9 will play this chord when pressed)"));
        add(top, BorderLayout.NORTH);

        // Center: interactive fretboard.
        fretboard.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(fretboard, BorderLayout.CENTER);

        // Bottom: hint + buttons.
        JPanel bottom = new JPanel(new BorderLayout());
        JLabel hint = new JLabel("  Click on a string's row to cycle:  X  ->  O  ->  1  ->  2  ->  ...  ->  X again");
        hint.setFont(hint.getFont().deriveFont(Font.PLAIN, 11f));
        hint.setForeground(Color.DARK_GRAY);
        bottom.add(hint, BorderLayout.NORTH);

        JPanel buttons = new JPanel();
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());
        JButton save = new JButton("Save");
        save.addActionListener(e -> save());
        JButton presetBtn = new JButton("Load Preset...");
        presetBtn.addActionListener(e -> showPresetPicker());
        buttons.add(presetBtn);
        buttons.add(cancel);
        buttons.add(save);
        bottom.add(buttons, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);
    }

    private void showPresetPicker() {
        List<Chord> presets = library.presets();
        String[] names = presets.stream().map(Chord::getName).toArray(String[]::new);
        Object chosen = JOptionPane.showInputDialog(
                this, "Pick a preset to load into the editor:", "Load Preset",
                JOptionPane.PLAIN_MESSAGE, null, names, names[0]);
        if (chosen != null) {
            for (Chord c : presets) {
                if (c.getName().equals(chosen)) {
                    nameField.setText(c.getName());
                    fretboard.setFrets(c.getFrets());
                    return;
                }
            }
        }
    }

    private void save() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a chord name.", "Missing name", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int hotkey = (Integer) hotkeyBox.getSelectedItem();
        int[] frets = fretboard.getFrets();
        Chord chord = new Chord(name, frets, hotkey);
        library.bind(chord);
        if (onSaved != null) onSaved.run();
        dispose();
    }

    /* ---------- interactive fretboard ---------- */

    private static final class EditableFretboard extends JPanel {

        private static final int NUM_STRINGS = 6;
        private static final int NUM_COLS = MAX_FRET + 2;   // open col + 15 frets

        private final int[] frets = new int[]{-1, -1, -1, -1, -1, -1};
        private final List<Rectangle> stringRows = new ArrayList<>();   // hit regions per string

        EditableFretboard() {
            setPreferredSize(new Dimension(640, 320));
            setBackground(new Color(245, 245, 240));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    Point p = e.getPoint();
                    for (int s = 0; s < stringRows.size(); s++) {
                        if (stringRows.get(s).contains(p)) {
                            cycleString(s);
                            return;
                        }
                    }
                }
            });
        }

        private void cycleString(int s) {
            int current = frets[s];
            int next;
            if (current < 0) next = 0;
            else if (current >= MAX_FRET) next = -1;
            else next = current + 1;
            frets[s] = next;
            repaint();
        }

        void setFrets(int[] newFrets) {
            System.arraycopy(newFrets, 0, frets, 0, 6);
            repaint();
        }

        int[] getFrets() {
            return frets.clone();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

            stringRows.clear();

            int w = getWidth();
            int h = getHeight();
            int leftPad = 50;
            int topPad = 30;
            int bottomPad = 20;
            int rightPad = 20;
            int boardW = w - leftPad - rightPad;
            int boardH = h - topPad - bottomPad;

            int openColX = leftPad + 30;
            int fretsStartX = openColX + 40;
            int fretSpacing = (w - rightPad - fretsStartX) / (MAX_FRET);
            int stringTop = topPad + 20;
            int stringBottom = topPad + boardH - 20;
            int stringSpacing = (stringBottom - stringTop) / (NUM_STRINGS - 1);

            // Wood background.
            g2.setColor(new Color(180, 130, 70));
            g2.fillRect(openColX - 5, topPad, w - rightPad - openColX + 5, boardH);
            // Headstock.
            g2.setColor(new Color(80, 50, 30));
            g2.fillRect(leftPad, topPad, openColX - leftPad, boardH);
            // Nut.
            g2.setColor(Color.WHITE);
            g2.fillRect(openColX - 3, topPad, 3, boardH);

            // Frets.
            g2.setColor(Color.LIGHT_GRAY);
            for (int f = 0; f <= MAX_FRET; f++) {
                int x = fretsStartX + f * fretSpacing;
                g2.drawLine(x, topPad, x, topPad + boardH);
            }

            // String labels + horizontal string lines + clickable rows.
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 13f));
            String[] names = {"e", "B", "G", "D", "A", "E"};
            for (int s = 0; s < NUM_STRINGS; s++) {
                int y = stringTop + s * stringSpacing;
                // Hit region: spans the full row, a bit of vertical padding.
                stringRows.add(new Rectangle(leftPad, y - stringSpacing / 2 - 2,
                        w - rightPad - leftPad, stringSpacing + 4));

                // String line.
                g2.setColor(new Color(60, 60, 60));
                float thick = 1.5f + (NUM_STRINGS - 1 - s) * 0.3f;
                g2.setStroke(new java.awt.BasicStroke(thick));
                g2.drawLine(leftPad, y, w - rightPad, y);

                // Label.
                g2.setColor(Color.BLACK);
                g2.drawString(names[s], leftPad + 10, y + 4);
            }

            // Fret position numbers along the top.
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 10f));
            g2.setColor(Color.DARK_GRAY);
            for (int f = 0; f <= MAX_FRET; f++) {
                int x = fretsStartX + f * fretSpacing;
                g2.drawString(String.valueOf(f), x - 3, topPad - 8);
            }

            // Markers per string.
            for (int s = 0; s < NUM_STRINGS; s++) {
                int y = stringTop + s * stringSpacing;
                int f = frets[s];

                if (f < 0) {
                    // X
                    g2.setColor(Color.RED);
                    g2.setStroke(new java.awt.BasicStroke(2f));
                    int x = openColX - 18;
                    g2.drawLine(x - 6, y - 6, x + 6, y + 6);
                    g2.drawLine(x - 6, y + 6, x + 6, y - 6);
                } else if (f == 0) {
                    g2.setColor(new Color(0, 150, 0));
                    g2.setStroke(new java.awt.BasicStroke(2f));
                    Ellipse2D c = new Ellipse2D.Double(openColX - 24, y - 8, 16, 16);
                    g2.draw(c);
                } else {
                    int x = fretsStartX + (f - 1) * fretSpacing + fretSpacing / 2;
                    g2.setColor(new Color(50, 110, 220));
                    Ellipse2D dot = new Ellipse2D.Double(x - 12, y - 12, 24, 24);
                    g2.fill(dot);
                    g2.setColor(Color.WHITE);
                    g2.setFont(g2.getFont().deriveFont(Font.BOLD, 11f));
                    String lbl = String.valueOf(f);
                    int lw = g2.getFontMetrics().stringWidth(lbl);
                    g2.drawString(lbl, x - lw / 2, y + 4);
                }
            }
        }
    }
}
