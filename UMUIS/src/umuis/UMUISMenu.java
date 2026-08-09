package umuis;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * A parsed UMUIS menu: the window title and size, an optional background
 * color, and the list of elements to render. Menus are described in XML and
 * loaded one at a time by {@link UMUISWindow} into a single shared window.
 */
public final class UMUISMenu {

    private final Path source;
    private final String title;
    private final int width;
    private final int height;
    private final Integer background;
    private final List<UMUISElement> elements;

    public UMUISMenu(Path source, String title, int width, int height,
                     Integer background, List<UMUISElement> elements) {
        this.source = source;
        this.title = title;
        this.width = width;
        this.height = height;
        this.background = background;
        this.elements = List.copyOf(elements);
    }

    /** The XML file this menu was loaded from. */
    public Path source() {
        return source;
    }

    /** The window title, or an empty string when the menu has none. */
    public String title() {
        return title;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /** RGB value of the menu background, or null for the default. */
    public Integer background() {
        return background;
    }

    /** All elements of this menu, in document order. */
    public List<UMUISElement> elements() {
        return Collections.unmodifiableList(elements);
    }
}
