package umml;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * A frame-by-frame animation for the UMML Renderer (UMML 2.5).
 *
 * <p>An animation is a list of frames shown one after another. You can build
 * one from a <b>sprite sheet</b> (one picture with the frames laid out in a
 * grid - the classic way to do game animation) or from a plain list of
 * pictures.
 *
 * <pre>
 * // Sprite sheet: frames read left-to-right, then top-to-bottom.
 * UMMLImage sheet = UMMLImage.load("assets/hero_walk.png"); // 4 frames x 2 rows
 * UMMLAnimation walk = new UMMLAnimation(sheet, 32, 32);
 * walk.setFrameDuration(0.1);   // each frame shows for 0.1 seconds
 *
 * // A whole row of frames (frame 4 .. 7, say the 2nd row):
 * UMMLAnimation run = new UMMLAnimation(sheet, 32, 32, 4, 4);
 *
 * // Or just hand it a bunch of pictures:
 * UMMLAnimation flicker = new UMMLAnimation(
 *     UMMLImage.solid(8, 8, Color.WHITE),
 *     UMMLImage.solid(8, 8, Color.GRAY));
 * </pre>
 *
 * <p>Make a sprite animate by attaching the animation to it. The sprite then
 * shows each frame in turn automatically while it updates:
 *
 * <pre>
 * UMMLSprite hero = new UMMLSprite(sheet, 100, 100);
 * hero.setAnimation(walk);       // now the sprite is animated
 * renderer.addSprite(hero);      // updates + draws it every frame
 * </pre>
 *
 * <p>Animations that are not attached to a sprite can still be driven by hand:
 * call {@link #update(double)} to advance them and {@link #currentImage()} to
 * see the frame.
 *
 * <p>Nothing throws here - an empty animation simply shows nothing, and a bad
 * frame index never crashes.
 */
public final class UMMLAnimation {

    private final List<UMMLImage> frames = new ArrayList<>();
    private double frameDuration = 0.1;
    private boolean loop = true;
    private boolean playing = true;
    private double timer = 0;
    private int current = 0;
    private Runnable onFinished = null;

    /** Creates an empty animation (no frames). Add frames with {@link #addFrame(UMMLImage)}. */
    public UMMLAnimation() {
    }

    /**
     * Builds an animation from the frames found in a sprite sheet.
     *
     * @param sheet the picture containing the frames in a grid
     * @param frameWidth width of one frame, in pixels
     * @param frameHeight height of one frame, in pixels
     */
    public UMMLAnimation(UMMLImage sheet, int frameWidth, int frameHeight) {
        addSheet(sheet, frameWidth, frameHeight);
    }

    /**
     * Builds an animation from a run of frames inside a sprite sheet.
     *
     * @param sheet the picture containing the frames in a grid
     * @param frameWidth width of one frame, in pixels
     * @param frameHeight height of one frame, in pixels
     * @param startFrame the first frame to include (0 = top-left of the sheet)
     * @param frameCount how many frames to include
     */
    public UMMLAnimation(UMMLImage sheet, int frameWidth, int frameHeight,
                         int startFrame, int frameCount) {
        addSheet(sheet, frameWidth, frameHeight, startFrame, frameCount);
    }

    /** Builds an animation from a list of pictures, in order. */
    public UMMLAnimation(UMMLImage... frames) {
        if (frames != null) {
            for (UMMLImage f : frames) {
                addFrame(f);
            }
        }
    }

    // ========================================================================
    // Building the animation
    // ========================================================================

    /** Adds one frame to the end of the animation. */
    public UMMLAnimation addFrame(UMMLImage image) {
        if (image != null) {
            frames.add(image);
        }
        return this;
    }

    /**
     * Adds every frame found in a sprite sheet, in reading order
     * (left-to-right, top-to-bottom).
     */
    public UMMLAnimation addSheet(UMMLImage sheet, int frameWidth, int frameHeight) {
        return addSheet(sheet, frameWidth, frameHeight, 0, Integer.MAX_VALUE);
    }

    /**
     * Adds a run of frames from a sprite sheet.
     *
     * @param startFrame the first frame to add (0 = top-left of the sheet)
     * @param frameCount how many frames to add
     */
    public UMMLAnimation addSheet(UMMLImage sheet, int frameWidth, int frameHeight,
                                  int startFrame, int frameCount) {
        if (sheet == null || frameWidth <= 0 || frameHeight <= 0) {
            return this;
        }
        int cols = Math.max(1, sheet.width() / frameWidth);
        int rows = Math.max(1, sheet.height() / frameHeight);
        int total = cols * rows;
        int from = Math.max(0, startFrame);
        int count = Math.min(Math.max(0, frameCount), total - from);
        for (int i = 0; i < count; i++) {
            int index = from + i;
            int col = index % cols;
            int row = index / cols;
            BufferedImage frame = sheet.buffered().getSubimage(
                    col * frameWidth, row * frameHeight, frameWidth, frameHeight);
            frames.add(UMMLImage.from(frame));
        }
        return this;
    }

    // ========================================================================
    // Playback control
    // ========================================================================

    /** Sets how long each frame is shown, in seconds. Default 0.1. */
    public UMMLAnimation setFrameDuration(double seconds) {
        this.frameDuration = Math.max(0.0001, seconds);
        return this;
    }

    /** The time each frame is shown, in seconds. */
    public double frameDuration() {
        return frameDuration;
    }

    /** Whether the animation repeats forever. Default true. */
    public UMMLAnimation setLoop(boolean loop) {
        this.loop = loop;
        return this;
    }

    /** Whether the animation repeats forever. */
    public boolean loops() {
        return loop;
    }

    /** Starts playing from the current frame (the default state). */
    public UMMLAnimation play() {
        this.playing = true;
        return this;
    }

    /** Pauses playback. The current frame stays where it is. */
    public UMMLAnimation pause() {
        this.playing = false;
        return this;
    }

    /** Pauses playback and rewinds to the first frame. */
    public UMMLAnimation stop() {
        this.playing = false;
        this.current = 0;
        this.timer = 0;
        return this;
    }

    /** Jumps back to the first frame (and keeps playing). */
    public UMMLAnimation reset() {
        this.current = 0;
        this.timer = 0;
        return this;
    }

    /** Whether the animation is advancing. */
    public boolean isPlaying() {
        return playing;
    }

    /** Sets a one-off callback run when a non-looping animation finishes. */
    public UMMLAnimation setOnFinished(Runnable onFinished) {
        this.onFinished = onFinished;
        return this;
    }

    // ========================================================================
    // Stepping
    // ========================================================================

    /**
     * Advances the animation by {@code deltaSeconds}. Called automatically
     * when the animation is attached to a sprite the renderer updates, or
     * call it yourself. Has no effect while paused or when there are no
     * frames.
     */
    public UMMLAnimation update(double deltaSeconds) {
        if (!playing || frames.isEmpty()) {
            return this;
        }
        timer += Math.max(0, deltaSeconds);
        while (timer >= frameDuration) {
            timer -= frameDuration;
            current++;
            if (current >= frames.size()) {
                if (loop) {
                    current = 0;
                } else {
                    current = frames.size() - 1;
                    playing = false;
                    timer = 0;
                    if (onFinished != null) {
                        Runnable done = onFinished;
                        onFinished = null;
                        done.run();
                    }
                    break;
                }
            }
        }
        return this;
    }

    // ========================================================================
    // Reading
    // ========================================================================

    /** The number of frames in the animation. */
    public int frameCount() {
        return frames.size();
    }

    /** The current frame index (0 is the first frame). */
    public int frame() {
        return frames.isEmpty() ? -1 : current;
    }

    /** Jumps straight to a frame index (clamped, no crash on bad input). */
    public UMMLAnimation setFrame(int index) {
        if (!frames.isEmpty()) {
            this.current = Math.max(0, Math.min(index, frames.size() - 1));
            this.timer = 0;
        }
        return this;
    }

    /** The picture for the current frame, or null if the animation is empty. */
    public UMMLImage currentImage() {
        return frames.isEmpty() ? null : frames.get(current);
    }

    /** The picture for a specific frame, or null if the index is out of range. */
    public UMMLImage frame(int index) {
        return index < 0 || index >= frames.size() ? null : frames.get(index);
    }

    /** The width of the current frame, or 0 if the animation is empty. */
    public int width() {
        UMMLImage img = currentImage();
        return img == null ? 0 : img.width();
    }

    /** The height of the current frame, or 0 if the animation is empty. */
    public int height() {
        UMMLImage img = currentImage();
        return img == null ? 0 : img.height();
    }

    @Override
    public String toString() {
        return "UMMLAnimation{" + frames.size() + " frames, frame " + frame() + "}";
    }
}
