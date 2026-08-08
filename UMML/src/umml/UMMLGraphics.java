package umml;

import java.awt.Color;
import java.awt.Font;

/**
 * The drawing surface UMML renders into.
 *
 * <p>This is the <b>backend abstraction</b> that makes the UMML Renderer
 * future-proof. MTT games never touch this interface directly - they call
 * methods on {@link UMMLRenderer} instead. The renderer talks to this
 * interface, and UMML 1.5 ships one implementation, {@link UMMLGraphics2D},
 * which draws using the built-in Java2D graphics.
 *
 * <p>If you later want a different rendering system (for example OpenGL or
 * Vulkan), you write a new class that implements {@code UMMLGraphics} and
 * plug it into the renderer. MTT code keeps calling the exact same
 * {@link UMMLRenderer} methods and nothing has to be rewritten.
 *
 * <p>All coordinates are in <b>screen pixels</b> (the top-left of the
 * window is 0,0, x goes right, y goes down). Every method returns
 * {@code this} so calls can be chained:
 *
 * <pre>
 * graphics.clear(Color.BLACK);
 * graphics.setColor(Color.RED)
 *        .fillRect(50, 50, 100, 30);
 * </pre>
 */
public interface UMMLGraphics {

    /** The width of the drawing surface, in pixels. */
    int width();

    /** The height of the drawing surface, in pixels. */
    int height();

    /** Fills the whole surface with the given color. */
    UMMLGraphics clear(Color color);

    /** Sets the color used for the next drawing calls. */
    UMMLGraphics setColor(Color color);

    /** The color currently set. */
    Color color();

    /** Sets the font used for {@link #drawText}. */
    UMMLGraphics setFont(Font font);

    /** The font currently set. */
    Font font();

    /** Draws a filled rectangle at (x,y) with the given width and height. */
    UMMLGraphics fillRect(double x, double y, double w, double h);

    /** Draws the outline of a rectangle at (x,y) with the given size. */
    UMMLGraphics drawRect(double x, double y, double w, double h);

    /** Draws a filled circle centred at (x,y) with the given radius. */
    UMMLGraphics fillCircle(double x, double y, double radius);

    /** Draws the outline of a circle centred at (x,y) with the given radius. */
    UMMLGraphics drawCircle(double x, double y, double radius);

    /** Draws a line from (x1,y1) to (x2,y2). */
    UMMLGraphics drawLine(double x1, double y1, double x2, double y2);

    /**
     * Draws text. (x,y) is the bottom-left corner of the text, like the
     * standard Java {@code drawString}. Use {@link #setFont} to change size
     * and face first.
     */
    UMMLGraphics drawText(String text, double x, double y);

    /**
     * Draws an image scaled to the given width and height, top-left at
     * (x,y), unrotated.
     */
    UMMLGraphics drawImage(UMMLImage image, double x, double y, double w, double h);

    /**
     * Draws an image scaled to the given size, rotated by {@code degrees}
     * clockwise around its own centre.
     */
    UMMLGraphics drawImage(UMMLImage image, double x, double y, double w, double h, double degrees);

    /**
     * Draws a rectangular cut of an image - the part of the picture from
     * (sx,sy) sized sw x sh - stretched to (x,y) sized w x h. Used for
     * drawing tile maps and sprite-sheet frames. Source coords are in the
     * image's own pixels.
     */
    UMMLGraphics drawImage(UMMLImage image, double x, double y, double w, double h,
                           double sx, double sy, double sw, double sh);
}
