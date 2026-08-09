package umuis;

/**
 * One widget in a {@link UMUISMenu}. Each element maps to a Swing component
 * and is positioned absolutely inside the menu window via its bounds.
 *
 * <p>The {@code type} chooses the Swing component. The optional {@code target}
 * attribute (buttons) points at another menu XML to navigate to, and the
 * optional {@code action} attribute runs a built-in behaviour
 * ({@code quit}, {@code close}, {@code back}, {@code reload}).
 */
public final class UMUISElement {

    public static final String TYPE_LABEL = "label";
    public static final String TYPE_BUTTON = "button";
    public static final String TYPE_TEXTFIELD = "textfield";

    private final String type;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final String text;
    private final String align;
    private final int size;
    private final boolean bold;
    private final Integer color;
    private final String target;
    private final String action;
    private final String id;

    public UMUISElement(String type, int x, int y, int width, int height, String text,
                        String align, int size, boolean bold, Integer color,
                        String target, String action, String id) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.text = text;
        this.align = align;
        this.size = size;
        this.bold = bold;
        this.color = color;
        this.target = target;
        this.action = action;
        this.id = id;
    }

    public String type() {
        return type;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public String text() {
        return text;
    }

    public String align() {
        return align;
    }

    public int size() {
        return size;
    }

    public boolean bold() {
        return bold;
    }

    /** RGB value of the element's foreground color, or null for the default. */
    public Integer color() {
        return color;
    }

    /** The menu XML to navigate to when this element is activated. */
    public String target() {
        return target;
    }

    /** A built-in action: quit, close, back, or reload. */
    public String action() {
        return action;
    }

    public String id() {
        return id;
    }
}
