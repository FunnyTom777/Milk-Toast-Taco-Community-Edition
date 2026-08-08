package umml;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * The UMML Renderer - UMML 1.5's easy 2D game renderer.
 *
 * <p>One class to create a window, run the game loop (update + draw ~60
 * times a second), handle the keyboard and mouse, move sprites, and draw
 * shapes, text and pictures. It is built on the standard Java graphics that
 * ship with every JDK, so MTT versions can render with zero extra libraries.
 *
 * <p><b>The absolute minimum game:</b>
 *
 * <pre>
 * UMMLRenderer game = UMMLRenderer.open("My Game", 800, 600);
 * game.start();            // opens the window and starts the loop
 * </pre>
 *
 * <p><b>A sprite that moves with the arrow keys:</b>
 *
 * <pre>
 * UMMLRenderer game = UMMLRenderer.open("My Game", 800, 600);
 *
 * UMMLSprite player = new UMMLSprite(UMMLImage.load("assets/player.png"), 400, 300);
 * game.addSprite(player);                       // auto-drawn every frame
 *
 * game.onUpdate(delta -> {                      // called ~60x per second
 *     double speed = 200 * delta;               // pixels this frame
 *     if (game.input().isDown(KeyEvent.VK_LEFT))  player.move(-speed, 0);
 *     if (game.input().isDown(KeyEvent.VK_RIGHT)) player.move(speed, 0);
 *     if (game.input().isDown(KeyEvent.VK_UP))    player.move(0, -speed);
 *     if (game.input().isDown(KeyEvent.VK_DOWN))  player.move(0, speed);
 * });
 *
 * game.onDraw(renderer -> {                     // drawn after update
 *     renderer.drawText("Hello UMML!", 20, 30);
 * });
 *
 * game.start();
 * </pre>
 *
 * <p>Everything is in <b>world coordinates</b>. The camera starts at (0,0)
 * so world and window match, but if your world is bigger than the window,
 * move the camera (or use {@link #centerOn(double, double)}) to look
 * somewhere else. Nothing throws while drawing - a missing image draws as a
 * pink box and the game keeps running.
 *
 * <p><b>How this stays future-proof:</b> the renderer draws through the
 * {@link UMMLGraphics} interface (the backend). UMML 1.5 uses the built-in
 * Java2D backend. If a different rendering system is wanted later, a new
 * {@code UMMLGraphics} implementation replaces it and no MTT code changes.
 */
public final class UMMLRenderer {

    /** Code for the per-frame update step. */
    public interface UpdateHandler {
        /**
         * Called every frame.
         *
         * @param deltaSeconds seconds since the last frame (usually ~0.016).
         *        Multiply by this to get frame-rate-independent movement.
         */
        void update(double deltaSeconds);
    }

    /** Code for the per-frame drawing step. */
    public interface DrawHandler {
        /** Called every frame with the renderer, ready for drawing. */
        void draw(UMMLRenderer renderer);
    }

    private final JFrame frame;
    private final JPanel canvas;
    private final BufferedImage buffer;
    private final int width;
    private final int height;
    private final UMMLInput input = new UMMLInput();
    private final List<UMMLSprite> sprites = new ArrayList<>();
    private final List<UMMLParticleSystem> particleSystems = new ArrayList<>();

    private UpdateHandler updateHandler = null;
    private DrawHandler drawHandler = null;

    private Color clearColor = Color.BLACK;
    private double cameraX;
    private double cameraY;
    private int targetFps = 60;
    private boolean running = false;
    private Thread loop;

    private UMMLRenderer(String title, int width, int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.buffer = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);

        this.canvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(buffer, 0, 0, null);
            }
        };
        canvas.setPreferredSize(new Dimension(this.width, this.height));
        canvas.setFocusable(true);
        wireInput();

        this.frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(canvas);
        frame.pack();
        frame.setLocationRelativeTo(null);
    }

    /**
     * Creates a window and a renderer for it. Nothing is drawn until you
     * call {@link #start()}.
     *
     * @param title the window title
     * @param width the window width in pixels
     * @param height the window height in pixels
     */
    public static UMMLRenderer open(String title, int width, int height) {
        return new UMMLRenderer(title, width, height);
    }

    // ========================================================================
    // Lifecycle
    // ========================================================================

    /**
     * Shows the window and starts the game loop. The loop calls your
     * {@link #onUpdate(UpdateHandler)} then {@link #onDraw(DrawHandler)}
     * handler about {@link #setTargetFps(int)} times per second, and
     * auto-updates and auto-draws every sprite added with
     * {@link #addSprite(UMMLSprite)}.
     */
    public void start() {
        if (running) return;
        running = true;
        frame.setVisible(true);
        canvas.requestFocusInWindow();
        loop = new Thread(this::gameLoop, "UMMLRenderer");
        loop.setDaemon(true);
        loop.start();
    }

    /** Stops the loop and closes the window. */
    public void close() {
        running = false;
        if (loop != null) {
            try {
                loop.join(1000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        frame.dispose();
    }

    /** Whether the loop is currently running. */
    public boolean isRunning() {
        return running;
    }

    /** The width of the window, in pixels. */
    public int width() {
        return width;
    }

    /** The height of the window, in pixels. */
    public int height() {
        return height;
    }

    /** Changes the window title. */
    public UMMLRenderer setTitle(String title) {
        frame.setTitle(title);
        return this;
    }

    // ========================================================================
    // Game loop hooks
    // ========================================================================

    /**
     * Sets the per-frame update code. Run every frame before drawing.
     * Use {@code delta} to move things by a distance-per-second:
     * {@code sprite.move(100 * delta, 0)} moves it 100 pixels per second.
     */
    public UMMLRenderer onUpdate(UpdateHandler handler) {
        this.updateHandler = handler;
        return this;
    }

    /**
     * Sets the per-frame drawing code. Runs every frame after update, before
     * the auto-drawn sprites. Draw through the renderer's methods
     * ({@link #fillRect}, {@link #drawImage}, ...) which respect the camera.
     */
    public UMMLRenderer onDraw(DrawHandler handler) {
        this.drawHandler = handler;
        return this;
    }

    // ========================================================================
    // Sprites
    // ========================================================================

    /**
     * Registers a sprite so the renderer updates it (moves it by its
     * velocity) and draws it every frame, in the order sprites were added.
     * Draw it yourself instead? Just skip this and call
     * {@link #drawSprite(UMMLSprite)} in your draw handler.
     */
    public UMMLRenderer addSprite(UMMLSprite sprite) {
        if (sprite != null && !sprites.contains(sprite)) {
            sprites.add(sprite);
        }
        return this;
    }

    /** Unregisters a sprite. */
    public UMMLRenderer removeSprite(UMMLSprite sprite) {
        sprites.remove(sprite);
        return this;
    }

    /** Removes every registered sprite. */
    public UMMLRenderer clearSprites() {
        sprites.clear();
        return this;
    }

    /** The registered sprites, in draw order. */
    public List<UMMLSprite> sprites() {
        return Collections.unmodifiableList(sprites);
    }

    // ========================================================================
    // Particle systems (UMML 2.5)
    // ========================================================================

    /**
     * Registers a particle system so the renderer updates and draws it every
     * frame (after the sprites, so effects sit on top).
     *
     * @see UMMLParticleSystem
     */
    public UMMLRenderer addParticles(UMMLParticleSystem system) {
        if (system != null && !particleSystems.contains(system)) {
            particleSystems.add(system);
        }
        return this;
    }

    /** Unregisters a particle system (its live particles stop updating). */
    public UMMLRenderer removeParticles(UMMLParticleSystem system) {
        particleSystems.remove(system);
        return this;
    }

    /** Removes every registered particle system. */
    public UMMLRenderer clearParticles() {
        particleSystems.clear();
        return this;
    }

    /** The registered particle systems, in draw order. */
    public List<UMMLParticleSystem> particleSystems() {
        return Collections.unmodifiableList(particleSystems);
    }

    // ========================================================================
    // Input
    // ========================================================================

    /** The keyboard + mouse state for the current frame. */
    public UMMLInput input() {
        return input;
    }

    /** Shortcut for {@code input().isDown(keyCode)}. */
    public boolean isKeyDown(int keyCode) {
        return input.isDown(keyCode);
    }

    // ========================================================================
    // Camera
    // ========================================================================

    /**
     * Moves the camera so world position (x,y) is the top-left of the
     * window. The camera starts at (0,0), which makes world coords equal to
     * window coords.
     */
    public UMMLRenderer setCamera(double x, double y) {
        this.cameraX = x;
        this.cameraY = y;
        return this;
    }

    /** Moves the camera so the given world position is the centre of the window. */
    public UMMLRenderer centerOn(double x, double y) {
        return setCamera(x - width / 2.0, y - height / 2.0);
    }

    /** Nudges the camera by (dx,dy) world pixels. */
    public UMMLRenderer moveCamera(double dx, double dy) {
        this.cameraX += dx;
        this.cameraY += dy;
        return this;
    }

    /** The camera's current x (top-left world coord shown). */
    public double cameraX() {
        return cameraX;
    }

    /** The camera's current y (top-left world coord shown). */
    public double cameraY() {
        return cameraY;
    }

    // ========================================================================
    // Appearance
    // ========================================================================

    /** The color the window is cleared to at the start of each frame. */
    public UMMLRenderer setClearColor(Color color) {
        this.clearColor = color;
        return this;
    }

    /** How many times a second the loop tries to update+draw. Default 60. */
    public UMMLRenderer setTargetFps(int fps) {
        this.targetFps = Math.max(1, fps);
        return this;
    }

    // ========================================================================
    // Drawing (world coordinates, respects the camera)
    // ========================================================================

    /** Draws a filled rectangle. */
    public UMMLRenderer fillRect(double x, double y, double w, double h) {
        graphics().fillRect(x - cameraX, y - cameraY, w, h);
        return this;
    }

    /** Draws a filled rectangle in a specific colour (one call, no setColor). */
    public UMMLRenderer fillRect(double x, double y, double w, double h, Color color) {
        graphics().setColor(color).fillRect(x - cameraX, y - cameraY, w, h);
        return this;
    }

    /** Draws a rectangle outline. */
    public UMMLRenderer drawRect(double x, double y, double w, double h) {
        graphics().drawRect(x - cameraX, y - cameraY, w, h);
        return this;
    }

    /** Draws a filled circle centred at (x,y). */
    public UMMLRenderer fillCircle(double x, double y, double radius) {
        graphics().fillCircle(x - cameraX, y - cameraY, radius);
        return this;
    }

    /** Draws a filled circle centred at (x,y) in a specific colour. */
    public UMMLRenderer fillCircle(double x, double y, double radius, Color color) {
        graphics().setColor(color).fillCircle(x - cameraX, y - cameraY, radius);
        return this;
    }

    /** Draws a circle outline centred at (x,y). */
    public UMMLRenderer drawCircle(double x, double y, double radius) {
        graphics().drawCircle(x - cameraX, y - cameraY, radius);
        return this;
    }

    /** Draws a line. */
    public UMMLRenderer drawLine(double x1, double y1, double x2, double y2) {
        graphics().drawLine(x1 - cameraX, y1 - cameraY, x2 - cameraX, y2 - cameraY);
        return this;
    }

    /** Draws text at (x,y) (bottom-left of the text). Uses the given colour and size. */
    public UMMLRenderer drawText(String text, double x, double y, Color color, float size) {
        graphics().setColor(color).setFont(new Font(Font.DIALOG, Font.PLAIN, Math.max(1, Math.round(size))))
                .drawText(text, x - cameraX, y - cameraY);
        return this;
    }

    /** Draws text at (x,y) in the current default colour and size. */
    public UMMLRenderer drawText(String text, double x, double y) {
        graphics().drawText(text, x - cameraX, y - cameraY);
        return this;
    }

    /** Draws an image scaled to w x h, top-left at (x,y). */
    public UMMLRenderer drawImage(UMMLImage image, double x, double y, double w, double h) {
        graphics().drawImage(image, x - cameraX, y - cameraY, w, h);
        return this;
    }

    /** Draws a sprite exactly as it is (position, size, rotation, colour/picture). */
    public UMMLRenderer drawSprite(UMMLSprite sprite) {
        if (sprite == null || !sprite.isVisible()) return this;
        double sx = sprite.x() - cameraX;
        double sy = sprite.y() - cameraY;
        if (sprite.rotation() != 0) {
            graphics().drawImage(sprite.image(), sx, sy, sprite.width(), sprite.height(), sprite.rotation());
        } else if (sprite.image() != null) {
            graphics().drawImage(sprite.image(), sx, sy, sprite.width(), sprite.height());
        } else if (sprite.color() != null) {
            graphics().setColor(sprite.color()).fillRect(sx, sy, sprite.width(), sprite.height());
        }
        return this;
    }

    /**
     * Draws a tile map (UMML 2.5). Only the tiles inside the window are
     * drawn, so big levels are cheap. Each tile is cut from the map's tile
     * sheet and drawn at its cell, and the whole map is offset by
     * {@link UMMLTilemap#setPosition(double, double)}.
     *
     * @see UMMLTilemap
     */
    public UMMLRenderer drawTilemap(UMMLTilemap map) {
        if (map == null || map.sheet() == null) {
            return this;
        }
        int cols = map.cols();
        int rows = map.rows();
        if (cols == 0 || rows == 0) {
            return this;
        }
        UMMLImage sheet = map.sheet();
        int tileW = map.tileWidth();
        int tileH = map.tileHeight();
        int sheetCols = map.sheetCols();
        int sheetRows = map.sheetRows();
        if (sheetCols == 0 || sheetRows == 0) {
            return this;
        }
        int sheetTotal = sheetCols * sheetRows;

        int firstCol = (int) Math.floor((cameraX - map.x()) / tileW);
        int firstRow = (int) Math.floor((cameraY - map.y()) / tileH);
        int lastCol = (int) Math.ceil((cameraX + width - map.x()) / tileW) - 1;
        int lastRow = (int) Math.ceil((cameraY + height - map.y()) / tileH) - 1;
        lastCol = Math.min(cols - 1, lastCol);
        lastRow = Math.min(rows - 1, lastRow);
        if (firstCol < 0) firstCol = 0;
        if (firstRow < 0) firstRow = 0;

        for (int r = firstRow; r <= lastRow; r++) {
            double wy = map.tileWorldY(r) - cameraY;
            for (int c = firstCol; c <= lastCol; c++) {
                int index = map.tileAt(c, r);
                if (index < 0) {
                    continue;
                }
                int sc = ((index % sheetTotal) % sheetCols + sheetCols) % sheetCols;
                int sr = ((index % sheetTotal) / sheetCols % sheetRows + sheetRows) % sheetRows;
                graphics().drawImage(sheet,
                        map.tileWorldX(c) - cameraX, wy, tileW, tileH,
                        (double) sc * tileW, (double) sr * tileH, (sc + 1) * tileW, (sr + 1) * tileH);
            }
        }
        return this;
    }

    /** The low-level backend for the current frame (mostly for UMML internals). */
    private UMMLGraphics graphics() {
        return backend;
    }

    // ========================================================================
    // The game loop
    // ========================================================================

    private UMMLGraphics backend;

    private void gameLoop() {
        long last = System.nanoTime();
        long frameNanos = 1_000_000_000L / targetFps;
        while (running) {
            long now = System.nanoTime();
            double delta = (now - last) / 1_000_000_000.0;
            last = now;
            if (delta > 0.1) delta = 0.1;

            input.endFrame();

            if (updateHandler != null) updateHandler.update(delta);
            for (UMMLSprite s : sprites) {
                s.update(delta);
            }
            for (UMMLParticleSystem ps : particleSystems) {
                ps.update(delta);
            }

            renderFrame();

            long elapsed = System.nanoTime() - now;
            long sleepNanos = frameNanos - elapsed;
            if (sleepNanos > 0) {
                try {
                    Thread.sleep(sleepNanos / 1_000_000L, (int) (sleepNanos % 1_000_000L));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void renderFrame() {
        Graphics2D g2 = buffer.createGraphics();
        try {
            backend = new UMMLGraphics2D(g2, width, height);
            backend.clear(clearColor);
            if (drawHandler != null) drawHandler.draw(this);
            for (UMMLSprite s : sprites) {
                drawSprite(s);
            }
            for (UMMLParticleSystem ps : particleSystems) {
                drawParticles(ps);
            }
        } finally {
            g2.dispose();
            backend = null;
        }
        canvas.repaint();
    }

    private void drawParticles(UMMLParticleSystem system) {
        UMMLImage img = system.image();
        Color start = system.color();
        Color end = system.endColor();
        for (UMMLParticleSystem.Particle p : system.particles()) {
            double f = UMMLParticleSystem.ageFraction(p.age, p.life);
            if (img != null) {
                graphics().drawImage(img,
                        p.x - cameraX - p.size / 2, p.y - cameraY - p.size / 2, p.size, p.size);
            } else {
                Color c;
                if (end != null) {
                    c = lerpColor(start, end, f);
                } else {
                    c = new Color(start.getRed(), start.getGreen(), start.getBlue(),
                            (int) Math.max(0, Math.min(255, Math.round((1 - f) * 255))));
                }
                graphics().setColor(c).fillCircle(p.x - cameraX, p.y - cameraY, p.size / 2);
            }
        }
    }

    private static Color lerpColor(Color a, Color b, double f) {
        double t = Math.max(0, Math.min(1, f));
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                (int) Math.round(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }

    // ========================================================================
    // Window input wiring
    // ========================================================================

    private void wireInput() {
        canvas.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                input.keyDown(e.getKeyCode());
            }

            @Override
            public void keyReleased(KeyEvent e) {
                input.keyUp(e.getKeyCode());
            }
        });

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                input.mouseMove(e.getX(), e.getY());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                input.mouseMove(e.getX(), e.getY());
            }

            @Override
            public void mousePressed(MouseEvent e) {
                input.mouseMove(e.getX(), e.getY());
                input.mouseDown(e.getButton());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                input.mouseMove(e.getX(), e.getY());
                input.mouseUp(e.getButton());
            }
        };
        canvas.addMouseListener(mouse);
        canvas.addMouseMotionListener(mouse);
    }
}
