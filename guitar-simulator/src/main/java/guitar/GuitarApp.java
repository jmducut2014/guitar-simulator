package guitar;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * The main application window. Holds:
 * <ul>
 *   <li>Center: a {@link GuitarPanel} showing the active chord's finger positions.</li>
 *   <li>East: a list of hotkey-bound chords plus an "Add Chord" button.</li>
 *   <li>South: a status bar that reports the most recent action.</li>
 * </ul>
 *
 * <p>Keyboard bindings are installed via the JComponent input map / action
 * map mechanism, scoped to {@code WHEN_IN_FOCUSED_WINDOW} so they fire even
 * when focus is on the chord list or buttons.
 *
 * <ul>
 *   <li><b>Down arrow</b> — strum down (high E first)</li>
 *   <li><b>Up arrow</b>   — strum up (low E first)</li>
 *   <li><b>0..9</b>        — select the chord bound to that digit (if any)</li>
 *   <li><b>Space</b>       — palm-mute (rest the hand on the strings)</li>
 * </ul>
 */
public final class GuitarApp extends JFrame {

    private final AudioEngine audio = new AudioEngine();
    private final ChordLibrary library = new ChordLibrary();

    private final GuitarPanel fretboard = new GuitarPanel();
    private final JLabel statusLabel = new JLabel(" ");
    private final JLabel activeChordLabel = new JLabel("No chord selected");
    private final JList<Chord> chordList = new JList<>();
    private final ChordListModel chordListModel = new ChordListModel();

    private int activeHotkey = -1;

    public GuitarApp() {
        super("Guitar Simulator");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                audio.shutdown();
            }
            @Override
            public void windowClosing(WindowEvent e) {
                audio.shutdown();
            }
        });

        buildUi();
        installKeyBindings();
        refreshChordList();
        setSize(900, 520);
        setMinimumSize(new Dimension(800, 460));
        setLocationRelativeTo(null);
    }

    private void buildUi() {
        setLayout(new BorderLayout(8, 8));

        // Center: fretboard.
        JPanel center = new JPanel(new BorderLayout());
        center.setBorder(new EmptyBorder(8, 8, 8, 8));
        center.add(fretboard, BorderLayout.CENTER);

        JPanel activePanel = new JPanel(new BorderLayout());
        activePanel.setBackground(new Color(40, 40, 50));
        activePanel.setBorder(new EmptyBorder(6, 10, 6, 10));
        activeChordLabel.setForeground(Color.WHITE);
        activeChordLabel.setFont(activeChordLabel.getFont().deriveFont(Font.BOLD, 16f));
        activePanel.add(activeChordLabel, BorderLayout.WEST);
        center.add(activePanel, BorderLayout.NORTH);

        add(center, BorderLayout.CENTER);

        // East: chord list + Add Chord button.
        Box right = Box.createVerticalBox();
        right.setBorder(new TitledBorder("Bound chords"));
        chordList.setModel(chordListModel);
        chordList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        chordList.setCellRenderer(new ChordListCellRenderer());
        chordList.setVisibleRowCount(12);
        JScrollPane scroll = new JScrollPane(chordList);
        scroll.setPreferredSize(new Dimension(260, 220));
        right.add(scroll);

        JButton addBtn = new JButton("Add Chord / Bind Hotkey...");
        addBtn.setToolTipText("Define a new chord (or load a preset) and bind it to a 0-9 key.");
        addBtn.setAlignmentX(LEFT_ALIGNMENT);
        addBtn.addActionListener(e -> openAddChordDialog());
        right.add(Box.createVerticalStrut(8));
        right.add(addBtn);

        JButton unbindBtn = new JButton("Unbind selected");
        unbindBtn.setToolTipText("Remove the hotkey binding from the selected chord.");
        unbindBtn.setAlignmentX(LEFT_ALIGNMENT);
        unbindBtn.addActionListener(e -> {
            Chord c = chordList.getSelectedValue();
            if (c != null) {
                library.unbind(c.getHotkey());
                refreshChordList();
                setStatus("Unbound hotkey " + c.getHotkey() + " (" + c.getName() + ")");
            }
        });
        right.add(Box.createVerticalStrut(4));
        right.add(unbindBtn);

        add(right, BorderLayout.EAST);

        // South: status bar + key reference.
        JPanel south = new JPanel(new BorderLayout());
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 12f));
        statusLabel.setBorder(new EmptyBorder(4, 10, 4, 10));
        south.add(statusLabel, BorderLayout.WEST);

        JLabel keys = new JLabel("  \u2193 strum down   \u2191 strum up   0-9 chord   Space mute  ");
        keys.setFont(keys.getFont().deriveFont(Font.PLAIN, 12f));
        keys.setForeground(Color.DARK_GRAY);
        south.add(keys, BorderLayout.EAST);
        south.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        add(south, BorderLayout.SOUTH);

        setStatus("Ready. Click \"Add Chord / Bind Hotkey...\" to set up your first chord.");
    }

    private void openAddChordDialog() {
        AddChordDialog dlg = new AddChordDialog(this, library, this::refreshChordList);
        dlg.setVisible(true);
    }

    private void refreshChordList() {
        chordListModel.setItems(library.boundChords());
    }

    private void setStatus(String text) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(text));
    }

    private void setActiveChord(Chord chord) {
        if (chord == null) {
            activeHotkey = -1;
            fretboard.setChord(new int[]{-1, -1, -1, -1, -1, -1});
            activeChordLabel.setText("No chord selected");
            return;
        }
        activeHotkey = chord.getHotkey();
        fretboard.setChord(chord.getFrets());
        activeChordLabel.setText("Active: " + chord.getName() + "  (key " + chord.getHotkey() + ")");
    }

    /* ---------- key bindings ---------- */

    private void installKeyBindings() {
        javax.swing.InputMap im = getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
        javax.swing.ActionMap am = getRootPane().getActionMap();

        // Down arrow = strum down.
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "strumDown");
        am.put("strumDown", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                strum(true);
            }
        });

        // Up arrow = strum up.
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "strumUp");
        am.put("strumUp", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                strum(false);
            }
        });

        // Space = mute.
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "mute");
        am.put("mute", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                audio.mute();
                setStatus("Muted (hand on strings).");
            }
        });

        // 0..9 = select chord.
        for (int d = 0; d <= 9; d++) {
            String key = "chord" + d;
            im.put(KeyStroke.getKeyStroke(Character.forDigit(d, 10), 0), key);
            final int digit = d;
            am.put(key, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    selectHotkey(digit);
                }
            });
        }
    }

    private void selectHotkey(int digit) {
        Chord c = library.getChordForHotkey(digit);
        if (c == null) {
            setStatus("Hotkey " + digit + " is not bound to any chord.");
            return;
        }
        setActiveChord(c);
        setStatus("Selected " + c.getName() + " (key " + digit + "). Now press \u2191 or \u2193 to strum.");
    }

    private void strum(boolean down) {
        Chord c = library.getChordForHotkey(activeHotkey);
        if (c == null) {
            setStatus("Select a chord first (press a 0-9 key).");
            return;
        }
        audio.strum(c.getFrets(), down);
        setStatus("Strum " + (down ? "down" : "up") + " on " + c.getName() + ".");
    }
}
