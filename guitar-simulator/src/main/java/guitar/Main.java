package guitar;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Application entry point. Boots the GUI on the Event Dispatch Thread.
 *
 * <p>Run from IntelliJ IDEA: just run the {@code main} method of this class.
 * Or from Gradle: {@code ./gradlew run}.
 */
public final class Main {

    private Main() {
        // No instances.
    }

    public static void main(String[] args) {
        // Use the platform-native look & feel so the app feels at home on macOS.
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Fallback to the default Metal L&F silently.
            System.err.println("Could not set system L&F: " + e.getMessage());
        }

        // All Swing UI creation must happen on the EDT.
        SwingUtilities.invokeLater(() -> {
            GuitarApp app = new GuitarApp();
            app.setVisible(true);
        });
    }
}
