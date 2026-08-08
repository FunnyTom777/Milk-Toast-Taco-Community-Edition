package umml;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

/**
 * Self test for the UMML Renderer (UMML 1.5).
 *
 * <p>Runs fully headless - it draws into an in-memory
 * {@link BufferedImage} via the {@link UMMLGraphics2D} backend and checks
 * pixels, sprite movement, image loading and input state. No window is ever
 * opened, so it is safe to run in build scripts.
 *
 * <p>Run with: java umml.UMMLRendererTest
 */
public final class UMMLRendererTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        boolean ok = runTests();
        System.out.println();
        System.out.println("RENDERER TEST: " + passed + " passed, " + failed + " failed");
        System.exit(ok ? 0 : 1);
    }

    /** Runs every renderer test. Returns true if all passed. */
    public static boolean runTests() {
        passed = 0;
        failed = 0;

        testImageLoading();
        testGraphicsDrawing();
        testSprite();
        testInput();

        return failed == 0;
    }

    // ========================================================================
    // Image loading
    // ========================================================================

    private static void testImageLoading() {
        UMMLImage solid = UMMLImage.solid(10, 20, Color.GREEN);
        check("solid image has the right size", solid.width() == 10 && solid.height() == 20);
        check("solid image pixel is the requested color",
                solid.buffered().getRGB(5, 10) == Color.GREEN.getRGB());
        check("solid image is not a placeholder", !solid.isPlaceholder());

        UMMLImage missing = UMMLImage.load("definitely/not/here.png");
        check("missing image never returns null", missing != null);
        check("missing image becomes the placeholder", missing.isPlaceholder());
        check("placeholder has a usable size", missing.width() > 0 && missing.height() > 0);

        UMMLImage empty = UMMLImage.empty(8, 8);
        check("empty image is transparent",
                (empty.buffered().getRGB(4, 4) >>> 24) == 0);
    }

    // ========================================================================
    // Drawing into a BufferedImage
    // ========================================================================

    private static void testGraphicsDrawing() {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        UMMLGraphics2D g = UMMLGraphics2D.from(img);

        check("backend reports the surface size", g.width() == 64 && g.height() == 64);
        check("backend implements the UMMLGraphics interface", g instanceof UMMLGraphics);

        g.clear(Color.BLACK);
        check("clear paints the whole surface",
                img.getRGB(5, 5) == Color.BLACK.getRGB() && img.getRGB(60, 60) == Color.BLACK.getRGB());

        g.setColor(Color.RED).fillRect(10, 10, 20, 20);
        check("fillRect paints the requested color", img.getRGB(15, 15) == Color.RED.getRGB());
        check("fillRect leaves other areas alone", img.getRGB(5, 5) == Color.BLACK.getRGB());

        g.setColor(Color.BLUE).drawRect(5, 5, 10, 10);
        check("drawRect paints the border", img.getRGB(5, 5) == Color.BLUE.getRGB());
        check("drawRect leaves the middle alone", img.getRGB(9, 9) == Color.BLACK.getRGB());

        g.setColor(Color.YELLOW).fillCircle(32, 32, 8);
        check("fillCircle paints its centre", img.getRGB(32, 32) == Color.YELLOW.getRGB());

        g.setColor(Color.WHITE).drawLine(0, 0, 10, 0);
        check("drawLine paints", img.getRGB(5, 0) == Color.WHITE.getRGB());

        g.setColor(Color.CYAN).drawText("Hi", 2, 40);
        // Just must not throw; text pixels are antialiased so no exact check.

        UMMLImage solid = UMMLImage.solid(9, 9, Color.MAGENTA);
        g.drawImage(solid, 40, 5, 9, 9);
        check("drawImage paints the picture's pixels", img.getRGB(44, 9) == Color.MAGENTA.getRGB());

        g.clear(Color.BLACK);
        g.drawImage(solid, 20, 20, 9, 9, 45);
        check("rotated drawImage keeps its centre", img.getRGB(24, 24) == Color.MAGENTA.getRGB());

        g.drawImage(null, 0, 0, 10, 10);
        check("drawing a null image does not throw", true);
    }

    // ========================================================================
    // Sprite movement, size, collision
    // ========================================================================

    private static void testSprite() {
        UMMLImage img = UMMLImage.solid(16, 8, Color.WHITE);
        UMMLSprite s = new UMMLSprite(img);
        check("sprite sizes itself to its image", s.width() == 16 && s.height() == 8);
        check("new sprite starts at 0,0", s.x() == 0 && s.y() == 0);

        s.setPosition(100, 50);
        check("setPosition moves the sprite", s.x() == 100 && s.y() == 50);
        check("centre is computed from position and size",
                s.centerX() == 108 && s.centerY() == 54);

        s.move(5, -5);
        check("move shifts the sprite", s.x() == 105 && s.y() == 45);

        s.setVelocity(10, -10);
        s.update(0.5);
        check("update moves the sprite by velocity * delta",
                s.x() == 110 && s.y() == 40);
        check("velocity getters report the velocity",
                s.velocityX() == 10 && s.velocityY() == -10);

        s.setSize(20, 20);
        check("setSize changes the sprite size", s.width() == 20 && s.height() == 20);

        UMMLSprite a = new UMMLSprite(Color.RED, 0, 0, 10, 10);
        UMMLSprite b = new UMMLSprite(Color.RED, 5, 5, 10, 10);
        check("overlapping sprites intersect", a.intersects(b));
        b.setPosition(50, 50);
        check("separated sprites do not intersect", !a.intersects(b));
        check("contains detects a point inside", a.contains(5, 5));
        check("contains rejects a point outside", !a.contains(20, 20));

        a.setRotation(90);
        check("rotation is stored in degrees", a.rotation() == 90);
        a.rotate(10);
        check("rotate adds to the rotation", a.rotation() == 100);

        a.setVisible(false);
        check("sprites can be hidden", !a.isVisible());
        a.setVisible(true);
        check("sprites can be shown again", a.isVisible());

        check("solid sprites report their color", new UMMLSprite(Color.ORANGE).color().equals(Color.ORANGE));
        check("picture sprites report their image", s.image() != null);
    }

    // ========================================================================
    // Input state
    // ========================================================================

    private static void testInput() {
        UMMLInput input = new UMMLInput();

        check("keys start up", !input.isDown(KeyEvent.VK_W));
        input.keyDown(KeyEvent.VK_W);
        check("keyDown sets isDown", input.isDown(KeyEvent.VK_W));
        check("keyDown sets wasPressed", input.wasPressed(KeyEvent.VK_W));

        input.endFrame();
        check("endFrame clears wasPressed", !input.wasPressed(KeyEvent.VK_W));
        check("endFrame keeps the key held", input.isDown(KeyEvent.VK_W));

        input.keyDown(KeyEvent.VK_W);
        check("holding a key does not re-trigger wasPressed",
                !input.wasPressed(KeyEvent.VK_W));

        input.keyUp(KeyEvent.VK_W);
        check("keyUp clears isDown", !input.isDown(KeyEvent.VK_W));
        check("keyUp sets wasReleased", input.wasReleased(KeyEvent.VK_W));
        input.endFrame();
        check("endFrame clears wasReleased", !input.wasReleased(KeyEvent.VK_W));

        input.mouseMove(30, 40);
        check("mouse position is tracked", input.mouseX() == 30 && input.mouseY() == 40);
        input.mouseDown(MouseEvent.BUTTON1);
        check("mouseDown sets isMouseDown", input.isMouseDown(MouseEvent.BUTTON1));
        check("mouseDown sets wasMousePressed", input.wasMousePressed(MouseEvent.BUTTON1));
        input.endFrame();
        check("endFrame clears wasMousePressed", !input.wasMousePressed(MouseEvent.BUTTON1));
        input.mouseUp(MouseEvent.BUTTON1);
        check("mouseUp clears isMouseDown", !input.isMouseDown(MouseEvent.BUTTON1));

        check("keyName names letters", UMMLInput.keyName(KeyEvent.VK_W).equals("W"));
        check("keyName names space", UMMLInput.keyName(KeyEvent.VK_SPACE).equals("SPACE"));
        check("keyName names arrows", UMMLInput.keyName(KeyEvent.VK_LEFT).equals("LEFT"));
    }

    // ========================================================================

    private static void check(String label, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("PASS  " + label);
        } else {
            failed++;
            System.out.println("FAIL  " + label);
        }
    }
}
