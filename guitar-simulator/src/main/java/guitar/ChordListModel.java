package guitar;

import javax.swing.AbstractListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * Swing {@link javax.swing.ListModel} backed by the chord library's currently
 * bound chords. Refreshed by {@link GuitarApp#refreshChordList()} whenever the
 * user adds or unbinds a chord.
 */
final class ChordListModel extends AbstractListModel<Chord> {

    private final List<Chord> items = new ArrayList<>();

    void setItems(List<Chord> newItems) {
        int oldSize = items.size();
        items.clear();
        items.addAll(newItems);
        if (oldSize > 0) fireIntervalRemoved(this, 0, oldSize - 1);
        if (!items.isEmpty()) fireIntervalAdded(this, 0, items.size() - 1);
    }

    @Override
    public int getSize() {
        return items.size();
    }

    @Override
    public Chord getElementAt(int index) {
        return items.get(index);
    }
}

/**
 * Renders a chord list entry as: {@code [3] G major   (0 2 3 0 x x)}
 * where the bracketed digit is the hotkey and the parenthesis shows the
 * fret layout from high E to low E.
 */
final class ChordListCellRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                 boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof Chord) {
            Chord c = (Chord) value;
            int[] f = c.getFrets();
            StringBuilder sb = new StringBuilder();
            sb.append('[').append(c.getHotkey()).append("]  ");
            sb.append(c.getName()).append("   (");
            for (int i = 0; i < 6; i++) {
                sb.append(f[i] < 0 ? "x" : f[i]);
                if (i < 5) sb.append(' ');
            }
            sb.append(')');
            setText(sb.toString());
        }
        return this;
    }
}
