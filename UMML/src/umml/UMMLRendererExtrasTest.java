package umml;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Self test for the UMML Renderer extras (UMML 2.5): tile maps,
 * particle effects and sprite animations.
 *
 * <p>Runs fully headless - it checks the model classes and the drawing
 * backend directly against in-memory {@link BufferedImage}s. No window is
 * ever opened, so it is safe to run in build scripts.
 *
 * <p>Run with: java umml.UMMLRendererExtrasTest
 */
public final class UMMLRendererExtrasTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        boolean ok = runTests();
        System.out.println();
        System.out.println("RENDERER EXTRAS TEST: " + passed + " passed, " + failed + " failed");
        System.exit(ok ? 0 : 1);
    }

    /** Runs every extras test. Returns true if all passed. */
    public static boolean runTests() {
        passed = 0;
        failed = 0;

        testAnimation();
        testAnimationOnSprite();
        testTilemap();
        testTilemapDrawing();
        testParticles();

        return failed == 0;
    }

    // ========================================================================
    // Sprite animations
    // ========================================================================

    private static void testAnimation() {
        UMMLImage f0 = UMMLImage.solid(8, 8, Color.RED);
        UMMLImage f1 = UMMLImage.solid(8, 8, Color.GREEN);
        UMMLImage f2 = UMMLImage.solid(8, 8, Color.BLUE);
        UMMLAnimation anim = new UMMLAnimation(f0, f1, f2);

        check("animation holds all its frames", anim.frameCount() == 3);
        check("animation starts on frame 0", anim.frame() == 0);
        check("currentImage returns the first frame", anim.currentImage() == f0);
        check("frame(i) returns the requested frame", anim.frame(1) == f1);
        check("frame out of range is null", anim.frame(99) == null);
        check("width comes from the current frame", anim.width() == 8 && anim.height() == 8);

        anim.setFrameDuration(0.5);
        anim.update(0.49);
        check("update before the frame ends stays put", anim.frame() == 0);
        anim.update(0.01);
        check("update past the frame duration advances", anim.frame() == 1);
        anim.update(1.0);
        check("a looping animation wraps back to 0", anim.frame() == 0);

        anim.setFrame(1);
        check("setFrame jumps straight to a frame", anim.frame() == 1);
        anim.setFrame(99);
        check("setFrame clamps to the last frame", anim.frame() == 2);
        anim.setFrame(-5);
        check("setFrame clamps to the first frame", anim.frame() == 0);

        anim.stop();
        check("stop pauses playback", !anim.isPlaying());
        check("stop rewinds to frame 0", anim.frame() == 0);
        anim.update(5);
        check("a paused animation does not advance", anim.frame() == 0);
        anim.play();
        check("play resumes playback", anim.isPlaying());

        UMMLAnimation oneShot = new UMMLAnimation(f0, f1);
        oneShot.setLoop(false);
        final int[] finished = {0};
        oneShot.setOnFinished(() -> finished[0]++);
        oneShot.update(0.5);
        oneShot.update(0.5);
        check("a non-looping animation stops on the last frame", oneShot.frame() == 1);
        check("a non-looping animation stops playing", !oneShot.isPlaying());
        check("the finished callback fires once", finished[0] == 1);
        oneShot.update(5);
        check("the finished callback does not fire again", finished[0] == 1);

        UMMLAnimation empty = new UMMLAnimation();
        check("an empty animation has no frames", empty.frameCount() == 0);
        check("an empty animation has no current frame", empty.currentImage() == null);
        check("an empty animation reports frame -1", empty.frame() == -1);
        empty.update(1.0);
        check("updating an empty animation does not throw", true);

        UMMLImage sheet = makeTwoByTwoSheet();
        UMMLAnimation fromSheet = new UMMLAnimation(sheet, 8, 8);
        check("a sprite sheet with 4 frames makes a 4-frame animation", fromSheet.frameCount() == 4);
        check("sheet frame 0 is the top-left tile",
                fromSheet.frame(0).buffered().getRGB(0, 0) == Color.RED.getRGB());
        check("sheet frame 3 is the bottom-right tile",
                fromSheet.frame(3).buffered().getRGB(0, 0) == Color.YELLOW.getRGB());

        UMMLAnimation run = new UMMLAnimation(sheet, 8, 8, 2, 2);
        check("a frame range reads just those frames", run.frameCount() == 2);
        check("the range starts at the requested frame",
                run.frame(0).buffered().getRGB(0, 0) == Color.BLUE.getRGB());
    }

    private static void testAnimationOnSprite() {
        UMMLImage f0 = UMMLImage.solid(8, 8, Color.RED);
        UMMLImage f1 = UMMLImage.solid(8, 8, Color.GREEN);
        UMMLAnimation anim = new UMMLAnimation(f0, f1);
        anim.setFrameDuration(0.25);

        UMMLSprite s = new UMMLSprite(f0, 10, 10);
        s.setAnimation(anim);
        check("attaching an animation shows its first frame", s.image() == f0);
        check("attaching an animation stores it", s.animation() == anim);

        s.update(0.3);
        check("updating the sprite advances the animation", s.image() == f1);
        check("the sprite still reports its animation", s.animation() == anim);

        s.clearAnimation();
        check("clearAnimation leaves the current frame", s.image() == f1);
        check("clearAnimation detaches the animation", s.animation() == null);

        s.setColor(Color.WHITE);
        check("setting a colour drops any animation", s.animation() == null);
    }

    // ========================================================================
    // Tile maps
    // ========================================================================

    private static void testTilemap() {
        UMMLImage sheet = makeTwoByTwoSheet();
        UMMLTilemap map = new UMMLTilemap(sheet, 8, 8);
        check("a new map is empty", map.cols() == 0 && map.rows() == 0);
        check("an empty map is 0 pixels", map.widthInPixels() == 0 && map.heightInPixels() == 0);

        map.setTiles(new int[][] {
            { 0, 1, 2 },
            { 3, -1, 0 },
        });
        check("setTiles sets the column count", map.cols() == 3);
        check("setTiles sets the row count", map.rows() == 2);
        check("tileAt reads a cell", map.tileAt(0, 0) == 0);
        check("tileAt reads another cell", map.tileAt(1, 1) == -1);
        check("isEmpty sees empty cells", map.isEmpty(1, 1));
        check("isEmpty sees non-empty cells", !map.isEmpty(2, 0));
        check("out-of-range cells are empty", map.isEmpty(9, 9));
        check("the map size uses the tile size", map.widthInPixels() == 24 && map.heightInPixels() == 16);

        map.setTile(4, 4, 2);
        check("setTile beyond the edge grows the map", map.cols() == 5 && map.rows() == 5);
        check("the grown cell holds its value", map.tileAt(4, 4) == 2);
        check("grown gaps are empty", map.isEmpty(3, 4));

        map.setPosition(100, 50);
        check("setPosition moves the map", map.x() == 100 && map.y() == 50);
        map.move(1, 2);
        check("move slides the map", map.x() == 101 && map.y() == 52);
        check("tileWorldX computes the cell's left edge", map.tileWorldX(2) == 101 + 16);
        check("tileWorldY computes the cell's top edge", map.tileWorldY(1) == 52 + 8);
        check("toWorldCol converts a world x back to a column", map.toWorldCol(101 + 8) == 1);
        check("toWorldRow converts a world y back to a row", map.toWorldRow(52 + 20) == 2);

        map.setPosition(0, 0);
        check("sheetCols counts across the sheet", map.sheetCols() == 2);
        check("sheetRows counts down the sheet", map.sheetRows() == 2);
        check("sheetCol reads the column of a tile", map.sheetCol(3) == 1);
        check("sheetRow reads the row of a tile", map.sheetRow(3) == 1);
        check("sheetCol wraps out-of-range tiles", map.sheetCol(99) == 1);
        check("sheetRow wraps out-of-range tiles", map.sheetRow(99) == 1);

        map.setTiles(null);
        check("a null grid empties the map", map.cols() == 0 && map.rows() == 0);

        map.setTiles(new int[][] { { 1, 2 }, null });
        check("a null row is treated as empty", map.isEmpty(0, 1) && map.isEmpty(1, 1));
        check("short rows are empty on the right", map.isEmpty(2, 0));

        check("tiles() returns a copy", map.tiles().length == 2 && map.tiles()[0].length == 2);
    }

    private static void testTilemapDrawing() {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        UMMLGraphics2D g = UMMLGraphics2D.from(img);
        g.clear(Color.BLACK);

        UMMLImage sheet = makeTwoByTwoSheet();
        UMMLGraphics2D result = g.drawImage(sheet, 0, 0, 16, 16, 8, 8, 8, 8);
        check("the cut-drawing method is chainable", result == g);
        check("the source cut is drawn top-left", img.getRGB(1, 1) == Color.YELLOW.getRGB());
        check("the source cut is scaled up", img.getRGB(14, 14) == Color.YELLOW.getRGB());
        check("the area outside the cut stays clear", img.getRGB(16, 0) == Color.BLACK.getRGB());

        g.clear(Color.BLACK);
        g.drawImage(null, 0, 0, 8, 8, 0, 0, 8, 8);
        check("drawing a null source cut does not throw", true);
    }

    // ========================================================================
    // Particle systems
    // ========================================================================

    private static void testParticles() {
        UMMLParticleSystem sys = new UMMLParticleSystem(100, 200);
        check("a new system starts empty", sys.particleCount() == 0);
        check("a new system starts disabled", !sys.isEnabled());
        check("position is stored", sys.x() == 100 && sys.y() == 200);

        sys.burst(30);
        check("burst spawns the requested particles", sys.particleCount() == 30);

        sys.setGravity(100).setDirection(0, 0).setSpeed(0, 0).setLife(5, 0);
        sys.clear();
        sys.burst(5);
        sys.update(1.0);
        check("gravity accelerates from a standstill",
                sys.particles().stream().allMatch(p -> p.vy == 100));
        sys.update(0.5);
        check("gravity keeps accelerating",
                sys.particles().stream().allMatch(p -> p.vy == 150));
        check("gravity moves particles", sys.particles().stream().anyMatch(p -> p.y > 200));

        sys.clear();
        sys.setLife(0.5, 0);
        sys.burst(5);
        sys.update(0.6);
        check("particles expire after their lifetime", sys.particleCount() == 0);

        sys.setRate(50).setEnabled(true);
        sys.update(0.1);
        int spawned = sys.particleCount();
        check("an enabled system streams particles", spawned > 0);
        sys.update(10);
        check("the stream never exceeds the cap", sys.particleCount() <= sys.max());
        sys.setEnabled(false);
        sys.update(10);
        check("disabling stops the stream", sys.particleCount() == 0);

        sys.clear();
        sys.setMax(5);
        sys.burst(10);
        sys.update(0.01);
        check("the cap also limits bursts", sys.particleCount() <= 5);

        sys.burst(3);
        int before = sys.particleCount();
        sys.clear();
        check("clear removes every particle", sys.particleCount() == 0 && before >= 3);

        UMMLParticleSystem noGravity = new UMMLParticleSystem(0, 0);
        noGravity.setSpeed(100, 0).setDirection(0, 0).setGravity(0);
        noGravity.burst(1);
        noGravity.update(0.5);
        check("particles move along their direction",
                noGravity.particles().get(0).x == 50 && noGravity.particles().get(0).y == 0);

        check("ageFraction is clamped", UMMLParticleSystem.ageFraction(5, 1) == 1);
        check("ageFraction is scaled", UMMLParticleSystem.ageFraction(0.5, 1) == 0.5);
        check("ageFraction guards bad lifetimes", UMMLParticleSystem.ageFraction(1, 0) == 1);
    }

    // ========================================================================

    private static UMMLImage makeTwoByTwoSheet() {
        UMMLImage sheet = UMMLImage.solid(16, 16, Color.BLACK);
        java.awt.Graphics2D g = sheet.buffered().createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, 8, 8);
        g.setColor(Color.GREEN);
        g.fillRect(8, 0, 8, 8);
        g.setColor(Color.BLUE);
        g.fillRect(0, 8, 8, 8);
        g.setColor(Color.YELLOW);
        g.fillRect(8, 8, 8, 8);
        g.dispose();
        return sheet;
    }

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
