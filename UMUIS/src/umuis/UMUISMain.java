package umuis;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Command line front end for UMUIS. Opens the single UMUIS window on the
 * given menu:
 *
 * <pre>
 *   java umuis.UMUISMain [menu_xml]
 * </pre>
 *
 * With no argument it opens {@code UMUIS/menus/main.xml} from the project
 * root (found the same way as {@link UMUIS#menusDirectory()}).
 */
public final class UMUISMain {

    private UMUISMain() {}

    public static void main(String[] args) {
        Path menu;
        if (args.length > 0) {
            menu = Path.of(args[0]).toAbsolutePath().normalize();
        } else {
            menu = UMUIS.menusDirectory().resolve("main.xml");
        }
        if (!Files.isRegularFile(menu)) {
            System.err.println("Menu not found: " + menu);
            System.err.println("Usage: java umuis.UMUISMain [menu.xml]");
            System.exit(1);
            return;
        }
        UMUIS.launch(menu);
    }
}
