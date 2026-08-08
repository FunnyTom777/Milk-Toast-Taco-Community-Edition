package umml;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

/**
 * A picture (texture/sprite image) that UMML can draw.
 *
 * <p>UMML images are loaded once and reused - loading is the only slow part,
 * drawing a sprite image every frame is fast. All the normal picture formats
 * work: PNG (best for game art, supports transparency), JPG, GIF, BMP.
 *
 * <pre>
 * // Load from a file (or from inside a jar / classpath folder):
 * UMMLImage player = UMMLImage.load("assets/player.png");
 *
 * // Draw it via the renderer:
 * renderer.drawImage(player, 100, 100, 64, 64);
 *
 * // Or put it on a sprite so it moves on its own:
 * UMMLSprite s = new UMMLSprite(player, 100, 100);
 * renderer.addSprite(s);
 * </pre>
 *
 * <p>Loading a picture that can't be found <b>never crashes</b>. You get a
 * pink placeholder box instead, so a missing asset is obvious on screen
 * (and easy to spot while developing) instead of breaking the game.
 *
 * <p>You can also build images from scratch, which is handy for making
 * simple games without any art files:
 *
 * <pre>
 * UMMLImage platform = UMMLImage.solid(200, 30, new Color(0x8B, 0x5A, 0x2B));
 * </pre>
 */
public final class UMMLImage {

    /** The image UMML returns when a picture could not be loaded. */
    private static final BufferedImage PLACEHOLDER = makePlaceholder();

    private final BufferedImage image;
    private final String source;

    private UMMLImage(BufferedImage image, String source) {
        this.image = image;
        this.source = source;
    }

    /**
     * Loads a picture from a file path, or from the classpath if the path
     * is not a file. Relative paths are checked against the current working
     * directory first.
     *
     * <p>If the picture cannot be found (or is not a valid image) this
     * returns a pink placeholder instead of throwing or returning null.
     *
     * @param pathOrResource e.g. {@code "assets/player.png"} or
     *        {@code "com/example/game/player.png"}
     */
    public static UMMLImage load(String pathOrResource) {
        if (pathOrResource == null || pathOrResource.isBlank()) {
            return new UMMLImage(PLACEHOLDER, "empty path");
        }
        BufferedImage img = null;
        String source = pathOrResource;
        try {
            Path p = Path.of(pathOrResource);
            if (Files.isRegularFile(p)) {
                img = ImageIO.read(p.toFile());
            }
        } catch (IOException | RuntimeException ignored) {
            // fall through to classpath attempt
        }
        if (img == null) {
            try (InputStream in = UMMLImage.class.getResourceAsStream(pathOrResource)) {
                if (in != null) img = ImageIO.read(in);
            } catch (IOException | RuntimeException ignored) {
                img = null;
            }
        }
        if (img == null) {
            return new UMMLImage(PLACEHOLDER, source + " (not found - placeholder shown)");
        }
        return new UMMLImage(img, source);
    }

    /** Wraps an already-loaded BufferedImage. */
    public static UMMLImage from(BufferedImage image) {
        return new UMMLImage(image, "fromBufferedImage");
    }

    /** Builds a solid-colour image of the given size. Great for quick test art. */
    public static UMMLImage solid(int width, int height, Color color) {
        BufferedImage img = new BufferedImage(Math.max(1, width), Math.max(1, height),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.dispose();
        return new UMMLImage(img, "solid " + color);
    }

    /** Builds a fully transparent image of the given size. */
    public static UMMLImage empty(int width, int height) {
        return solid(width, height, new Color(0, 0, 0, 0));
    }

    /** The width of this image in pixels. */
    public int width() {
        return image.getWidth();
    }

    /** The height of this image in pixels. */
    public int height() {
        return image.getHeight();
    }

    /** Where this image came from (path, resource name, or a description). */
    public String source() {
        return source;
    }

    /** The backing BufferedImage (used by the rendering backend and tools like UMML Studio). */
    public BufferedImage buffered() {
        return image;
    }

    /** True when this image is the placeholder returned for a failed load. */
    public boolean isPlaceholder() {
        return image == PLACEHOLDER;
    }

    @Override
    public String toString() {
        return "UMMLImage{" + source + ", " + width() + "x" + height() + "}";
    }

    private static BufferedImage makePlaceholder() {
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(255, 0, 255));
        g.fillRect(0, 0, 32, 32);
        g.setColor(Color.BLACK);
        g.drawLine(0, 0, 31, 31);
        g.drawLine(31, 0, 0, 31);
        g.dispose();
        return img;
    }
}
