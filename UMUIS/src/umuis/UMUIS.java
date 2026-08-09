package umuis;

import javax.swing.SwingUtilities;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Top-level API for the Unified MTT User Interface System.
 *
 * <p>UMUIS lets a game describe its menus as XML files and renders them one
 * at a time into a single Swing window that navigates between them. Menus
 * live in a {@code UMUIS/menus} folder at the root of the project - one XML
 * file per menu - and buttons navigate to other menus with their
 * {@code target} attribute.
 */
public final class UMUIS {

    private UMUIS() {}

    /**
     * Finds the project's {@code UMUIS/menus} folder by walking up from the
     * current directory until a folder containing {@code UMUIS/menus} is
     * found. Falls back to {@code UMUIS/menus} next to the current directory.
     */
    public static Path menusDirectory() {
        Path p = Path.of("").toAbsolutePath().normalize();
        while (p != null) {
            Path candidate = p.resolve("UMUIS").resolve("menus");
            if (Files.isDirectory(candidate)) return candidate;
            p = p.getParent();
        }
        return Path.of("UMUIS", "menus").toAbsolutePath().normalize();
    }

    /** Parses a menu XML file into a {@link UMUISMenu}. Throws when invalid. */
    public static UMUISMenu parse(Path file) throws Exception {
        return UMUISParser.parse(file);
    }

    /** Opens the single UMUIS window and shows the given menu. */
    public static void launch(Path startMenu) {
        SwingUtilities.invokeLater(() -> new UMUISWindow(startMenu).setVisible(true));
    }
}
