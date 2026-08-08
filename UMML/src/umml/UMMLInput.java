package umml;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Keyboard and mouse state for the current frame.
 *
 * <p>You get one of these from {@link UMMLRenderer#input()}. The renderer
 * feeds it keyboard and mouse events from the window and clears the "just
 * pressed" state at the start of every frame, so everything here always
 * describes the <b>current frame</b>.
 *
 * <pre>
 * UMMLInput input = renderer.input();
 *
 * if (input.isDown(KeyEvent.VK_LEFT))   player.move(-200 * delta, 0);
 * if (input.isDown(KeyEvent.VK_UP))     player.move(0, -200 * delta);
 *
 * if (input.wasPressed(KeyEvent.VK_SPACE)) fire();      // once per press
 * if (input.isMouseDown(MouseEvent.BUTTON1)) grab();
 *
 * int wx = input.mouseX();   // mouse position in the window
 * int wy = input.mouseY();
 * </pre>
 *
 * <p>Keys are referred to with the standard {@link KeyEvent} {@code VK_*}
 * constants, and mouse buttons with the {@link java.awt.event.MouseEvent}
 * {@code BUTTON*} constants. For a readable name of a key (for an on-screen
 * "press SPACE to start" hint) use {@link #keyName(int)}.
 */
public final class UMMLInput {

    private final Set<Integer> down = new HashSet<>();
    private final Set<Integer> justPressed = new HashSet<>();
    private final Set<Integer> justReleased = new HashSet<>();

    private final Set<Integer> mouseDown = new HashSet<>();
    private final Set<Integer> mouseJustPressed = new HashSet<>();

    private int mouseX;
    private int mouseY;

    UMMLInput() {}

    // ========================================================================
    // Keyboard
    // ========================================================================

    /** True while a key is held down. */
    public boolean isDown(int keyCode) {
        return down.contains(keyCode);
    }

    /** True on exactly the frame a key went down (fires once per press). */
    public boolean wasPressed(int keyCode) {
        return justPressed.contains(keyCode);
    }

    /** True on the frame a key was released. */
    public boolean wasReleased(int keyCode) {
        return justReleased.contains(keyCode);
    }

    /** True while any key is held down. */
    public boolean anyKeyDown() {
        return !down.isEmpty();
    }

    /** All key codes currently held down. */
    public Set<Integer> keysDown() {
        return new HashSet<>(down);
    }

    // ========================================================================
    // Mouse
    // ========================================================================

    /** The mouse's x position in window pixels. */
    public int mouseX() {
        return mouseX;
    }

    /** The mouse's y position in window pixels. */
    public int mouseY() {
        return mouseY;
    }

    /** True while a mouse button is held down. */
    public boolean isMouseDown(int button) {
        return mouseDown.contains(button);
    }

    /** True on exactly the frame a mouse button went down. */
    public boolean wasMousePressed(int button) {
        return mouseJustPressed.contains(button);
    }

    // ========================================================================
    // Keys by name / helpers
    // ========================================================================

    private static final Map<Integer, String> KEY_NAMES = new HashMap<>();

    static {
        KEY_NAMES.put(KeyEvent.VK_SPACE, "SPACE");
        KEY_NAMES.put(KeyEvent.VK_ENTER, "ENTER");
        KEY_NAMES.put(KeyEvent.VK_SHIFT, "SHIFT");
        KEY_NAMES.put(KeyEvent.VK_CONTROL, "CTRL");
        KEY_NAMES.put(KeyEvent.VK_ALT, "ALT");
        KEY_NAMES.put(KeyEvent.VK_TAB, "TAB");
        KEY_NAMES.put(KeyEvent.VK_ESCAPE, "ESC");
        KEY_NAMES.put(KeyEvent.VK_BACK_SPACE, "BACKSPACE");
        KEY_NAMES.put(KeyEvent.VK_UP, "UP");
        KEY_NAMES.put(KeyEvent.VK_DOWN, "DOWN");
        KEY_NAMES.put(KeyEvent.VK_LEFT, "LEFT");
        KEY_NAMES.put(KeyEvent.VK_RIGHT, "RIGHT");
        for (char c = 'A'; c <= 'Z'; c++) {
            KEY_NAMES.put(KeyEvent.getExtendedKeyCodeForChar(c), String.valueOf(c));
        }
        for (char d = '0'; d <= '9'; d++) {
            KEY_NAMES.put(KeyEvent.getExtendedKeyCodeForChar(d), String.valueOf(d));
        }
    }

    /**
     * A readable name for a key code, e.g. {@code keyName(VK_SPACE)}
     * returns "SPACE", {@code keyName(VK_W)} returns "W". Used for
     * "press X to jump" style on-screen hints. Returns "?" for unknown keys.
     */
    public static String keyName(int keyCode) {
        String name = KEY_NAMES.get(keyCode);
        if (name != null) return name;
        if (keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z) {
            return String.valueOf((char) ('A' + (keyCode - KeyEvent.VK_A)));
        }
        if (keyCode >= KeyEvent.VK_0 && keyCode <= KeyEvent.VK_9) {
            return String.valueOf((char) ('0' + (keyCode - KeyEvent.VK_0)));
        }
        return "?";
    }

    // ========================================================================
    // Fed by the renderer. Not for games to call.
    // ========================================================================

    void keyDown(int keyCode) {
        if (!down.contains(keyCode)) {
            justPressed.add(keyCode);
        }
        down.add(keyCode);
    }

    void keyUp(int keyCode) {
        down.remove(keyCode);
        justReleased.add(keyCode);
    }

    void mouseMove(int x, int y) {
        this.mouseX = x;
        this.mouseY = y;
    }

    void mouseDown(int button) {
        if (!mouseDown.contains(button)) {
            mouseJustPressed.add(button);
        }
        mouseDown.add(button);
    }

    void mouseUp(int button) {
        mouseDown.remove(button);
    }

    /** Clears the per-frame "just" state. Called at the start of each frame. */
    void endFrame() {
        justPressed.clear();
        justReleased.clear();
        mouseJustPressed.clear();
    }
}
