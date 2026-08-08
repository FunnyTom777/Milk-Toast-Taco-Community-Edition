package umml;

import java.awt.Color;

/**
 * A movable, drawable 2D thing - the easiest way to put graphics on screen.
 *
 * <p>A sprite holds a picture (or a solid colour), a position, a size and a
 * velocity. The UMML Renderer can move sprites for you automatically: call
 * {@link #setVelocity(double, double)} and the sprite moves itself every
 * frame when the renderer updates it.
 *
 * <pre>
 * UMMLImage playerImg = UMMLImage.load("assets/player.png");
 * UMMLSprite player = new UMMLSprite(playerImg, 200, 300);
 * player.setVelocity(120, 0);          // drifts right 120 px per second
 * renderer.addSprite(player);          // auto-updated and auto-drawn
 *
 * // Move it yourself on a key press:
 * if (renderer.input().isDown(KeyEvent.VK_LEFT)) {
 *     player.move(-200 * delta, 0);    // 200 px per second to the left
 * }
 * </pre>
 *
 * <p>Positions and velocities are in pixels and pixels-per-second. The
 * top-left corner of the sprite is at (x,y). Rotation is in degrees,
 * clockwise, around the sprite's centre.
 */
public class UMMLSprite {

    private UMMLImage image;
    private Color color = null;
    private UMMLAnimation animation = null;

    private double x;
    private double y;
    private double vx;
    private double vy;
    private double width;
    private double height;
    private double rotation = 0;
    private boolean visible = true;

    /** Creates a sprite from an image, 0,0, sized to the image. */
    public UMMLSprite(UMMLImage image) {
        setImage(image);
    }

    /** Creates a sprite from an image at a position, sized to the image. */
    public UMMLSprite(UMMLImage image, double x, double y) {
        setImage(image);
        setPosition(x, y);
    }

    /** Creates a solid-colour sprite, 32x32, at 0,0. */
    public UMMLSprite(Color color) {
        this(color, 0, 0, 32, 32);
    }

    /** Creates a solid-colour sprite of the given size at a position. */
    public UMMLSprite(Color color, double x, double y, double width, double height) {
        this.color = color;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // ========================================================================
    // Position and movement
    // ========================================================================

    /** Moves the sprite to a new (x,y) - its top-left corner. */
    public UMMLSprite setPosition(double x, double y) {
        this.x = x;
        this.y = y;
        return this;
    }

    /** Moves the sprite relative to where it is now. */
    public UMMLSprite move(double dx, double dy) {
        this.x += dx;
        this.y += dy;
        return this;
    }

    /**
     * Sets the speed in pixels per second. The sprite then moves itself
     * automatically while the renderer updates it.
     */
    public UMMLSprite setVelocity(double vx, double vy) {
        this.vx = vx;
        this.vy = vy;
        return this;
    }

    /** The current velocity in pixels per second. */
    public double velocityX() {
        return vx;
    }

    /** The current velocity in pixels per second. */
    public double velocityY() {
        return vy;
    }

    /**
     * Advances the sprite by {@code deltaSeconds} worth of its velocity.
     * Called automatically by the renderer for registered sprites, but you
     * can call it yourself too. Also advances a {@link #setAnimation(UMMLAnimation)
     * attached animation} so the sprite changes frames over time.
     */
    public UMMLSprite update(double deltaSeconds) {
        x += vx * deltaSeconds;
        y += vy * deltaSeconds;
        if (animation != null) {
            animation.update(deltaSeconds);
            UMMLImage frame = animation.currentImage();
            if (frame != null && frame != image) {
                image = frame;
            }
        }
        return this;
    }

    // ========================================================================
    // Position getters
    // ========================================================================

    /** The x position of the sprite's top-left corner. */
    public double x() {
        return x;
    }

    /** The y position of the sprite's top-left corner. */
    public double y() {
        return y;
    }

    /** The x position of the sprite's centre. */
    public double centerX() {
        return x + width / 2.0;
    }

    /** The y position of the sprite's centre. */
    public double centerY() {
        return y + height / 2.0;
    }

    // ========================================================================
    // Size
    // ========================================================================

    /** Sets the drawn size in pixels. */
    public UMMLSprite setSize(double width, double height) {
        this.width = width;
        this.height = height;
        return this;
    }

    /** The drawn width in pixels. */
    public double width() {
        return width;
    }

    /** The drawn height in pixels. */
    public double height() {
        return height;
    }

    // ========================================================================
    // Appearance
    // ========================================================================

    /** Swaps the sprite's picture. The size snaps to the new image's size. */
    public UMMLSprite setImage(UMMLImage image) {
        this.image = image;
        this.color = null;
        if (image != null) {
            this.width = image.width();
            this.height = image.height();
        }
        return this;
    }

    /** The sprite's picture, or null if it draws as a solid colour. */
    public UMMLImage image() {
        return image;
    }

    /** Switches the sprite to a solid colour. The picture is discarded. */
    public UMMLSprite setColor(Color color) {
        this.color = color;
        this.image = null;
        this.animation = null;
        return this;
    }

    /** The sprite's solid colour, or null if it draws a picture. */
    public Color color() {
        return color;
    }

    // ========================================================================
    // Animation (UMML 2.5)
    // ========================================================================

    /**
     * Makes the sprite play an animation. The sprite shows the animation's
     * current frame and, while the sprite is updated, advances it
     * automatically - so attaching a walk cycle to a player sprite is one
     * line. The sprite snaps to the first frame's size.
     *
     * <pre>
     * UMMLAnimation walk = new UMMLAnimation(sheet, 32, 32);
     * player.setAnimation(walk);
     * </pre>
     *
     * @see UMMLAnimation
     */
    public UMMLSprite setAnimation(UMMLAnimation animation) {
        this.animation = animation;
        this.color = null;
        if (animation != null) {
            UMMLImage frame = animation.currentImage();
            if (frame != null) {
                setImage(frame);
            }
        }
        return this;
    }

    /** The sprite's animation, or null if it draws a single picture. */
    public UMMLAnimation animation() {
        return animation;
    }

    /** Removes the animation, leaving the current frame as a static picture. */
    public UMMLSprite clearAnimation() {
        this.animation = null;
        return this;
    }

    /** Sets the rotation in degrees, clockwise, around the sprite's centre. */
    public UMMLSprite setRotation(double degrees) {
        this.rotation = degrees;
        return this;
    }

    /** Adds to the current rotation (degrees, clockwise). */
    public UMMLSprite rotate(double degrees) {
        this.rotation += degrees;
        return this;
    }

    /** The current rotation in degrees. */
    public double rotation() {
        return rotation;
    }

    /** Hides the sprite (it is no longer drawn, but still updates/moves). */
    public UMMLSprite setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    /** Whether the sprite is currently drawn. */
    public boolean isVisible() {
        return visible;
    }

    // ========================================================================
    // Collision helpers
    // ========================================================================

    /**
     * True if the sprite's box overlaps the other sprite's box. Handy for
     * super simple "did I touch it" checks.
     */
    public boolean intersects(UMMLSprite other) {
        return this.x < other.x + other.width
                && other.x < this.x + this.width
                && this.y < other.y + other.height
                && other.y < this.y + this.height;
    }

    /** True if the point (px,py) is inside the sprite's box. */
    public boolean contains(double px, double py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }

    @Override
    public String toString() {
        return "UMMLSprite{x=" + x + ", y=" + y + ", " + (int) width + "x" + (int) height + "}";
    }
}
