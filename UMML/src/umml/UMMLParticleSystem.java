package umml;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * A particle effect - lots of small moving dots that explode, rain, drift,
 * or spray (UMML 2.5). Explosions, smoke, sparks, fire, rain, confetti,
 * sparkle trails... all the same trick: a pile of tiny particles with a
 * starting speed, a lifetime and gravity pulling on them.
 *
 * <pre>
 * UMMLParticleSystem boom = new UMMLParticleSystem();
 * boom.setPosition(400, 300);
 * boom.setSpeed(180, 120);            // 180 px/s, plus or minus up to 120
 * boom.setLife(0.8, 0.4);             // each particle lives 0.4 - 1.2 s
 * boom.setSize(6, 3);
 * boom.setGravity(300);               // pulled down 300 px/s^2
 * boom.setColor(Color.ORANGE);
 * boom.setEndColor(new Color(0xFF, 0x44, 0x00));
 * renderer.addParticles(boom);        // auto-updated and auto-drawn
 *
 * boom.burst(40);                     // explode 40 particles at once
 * </pre>
 *
 * <p>Register the system with
 * {@link UMMLRenderer#addParticles(UMMLParticleSystem)} and it updates and
 * draws itself every frame. Two ways to make particles:
 * <ul>
 *   <li>{@link #burst(int)} - fire a pile of particles right now (explosions,
 *       blood, confetti).</li>
 *   <li>{@link #setRate(double)} + {@link #setEnabled(boolean)} - a steady
 *       stream while enabled (fire, smoke, rain, a muzzle flash).</li>
 * </ul>
 *
 * <p>Directions are angles in degrees, screen style: 0 is right, 90 is
 * down, 180 left, 270 up. By default particles burst in all directions
 * (spread 360) so an explosion just works. Particles start in the start
 * colour and fade smoothly into the end colour (by default transparent).
 *
 * <p>Nothing throws here. The system quietly caps the live particle count so
 * an over-enthusiastic effect can never lag the game into a crash.
 */
public final class UMMLParticleSystem {

    /** One live particle. Public so tools (and games) can read the details. */
    public static final class Particle {
        double x;
        double y;
        double vx;
        double vy;
        double age;
        double life;
        double size;
    }

    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();

    private double x;
    private double y;

    private boolean enabled = false;
    private double rate = 20;
    private double spawnAccumulator = 0;

    private double speed = 100;
    private double speedVariance = 50;
    private double angle = 270;
    private double spread = 360;
    private double gravity = 300;
    private double life = 1.0;
    private double lifeVariance = 0.5;
    private double size = 4;
    private double sizeVariance = 2;
    private UMMLImage image = null;
    private Color color = Color.WHITE;
    private Color endColor = null;
    private int maxParticles = 500;

    /** Creates a particle system at (0,0). Configure it, then burst or enable it. */
    public UMMLParticleSystem() {
    }

    /** Creates a particle system at a position. */
    public UMMLParticleSystem(double x, double y) {
        setPosition(x, y);
    }

    // ========================================================================
    // Position
    // ========================================================================

    /** The particle system's x. New particles spawn here. */
    public double x() {
        return x;
    }

    /** The particle system's y. New particles spawn here. */
    public double y() {
        return y;
    }

    /** Moves the spawn point to (x,y). Existing particles keep flying. */
    public UMMLParticleSystem setPosition(double x, double y) {
        this.x = x;
        this.y = y;
        return this;
    }

    /** Slides the spawn point by (dx,dy). Existing particles keep flying. */
    public UMMLParticleSystem move(double dx, double dy) {
        this.x += dx;
        this.y += dy;
        return this;
    }

    // ========================================================================
    // Making particles
    // ========================================================================

    /** Fires {@code count} particles right now from the spawn point. */
    public UMMLParticleSystem burst(int count) {
        for (int i = 0; i < count; i++) {
            spawn();
        }
        return this;
    }

    /** Fires particles as a steady stream while {@link #setEnabled(boolean) enabled}. */
    public UMMLParticleSystem setRate(double particlesPerSecond) {
        this.rate = Math.max(0, particlesPerSecond);
        return this;
    }

    /** How many particles are spawned per second while enabled. */
    public double rate() {
        return rate;
    }

    /** Turns the steady stream on or off. Particles already flying stay. */
    public UMMLParticleSystem setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /** Whether the steady stream is on. */
    public boolean isEnabled() {
        return enabled;
    }

    /** Removes every live particle. */
    public UMMLParticleSystem clear() {
        particles.clear();
        spawnAccumulator = 0;
        return this;
    }

    // ========================================================================
    // How particles behave
    // ========================================================================

    /**
     * Sets how fast particles fly off, in pixels per second, plus a random
     * plus-or-minus variance.
     */
    public UMMLParticleSystem setSpeed(double speed, double variance) {
        this.speed = Math.max(0, speed);
        this.speedVariance = Math.max(0, variance);
        return this;
    }

    /** The base speed in pixels per second. */
    public double speed() {
        return speed;
    }

    /**
     * Sets the direction particles fly. Screen-style degrees: 0 right,
     * 90 down, 180 left, 270 up. {@code spread} is how far either side they
     * can drift - 360 (default) is a full circle, 0 is a dead-straight beam.
     */
    public UMMLParticleSystem setDirection(double angleDegrees, double spreadDegrees) {
        this.angle = angleDegrees;
        this.spread = Math.max(0, Math.min(360, spreadDegrees));
        return this;
    }

    /** The base direction in degrees. */
    public double angle() {
        return angle;
    }

    /** How far either side of the direction particles can drift, in degrees. */
    public double spread() {
        return spread;
    }

    /** Pulls particles down (positive) or up (negative), in px/s^2. Default 300. */
    public UMMLParticleSystem setGravity(double gravity) {
        this.gravity = gravity;
        return this;
    }

    /** The gravity in px/s^2. */
    public double gravity() {
        return gravity;
    }

    /** Sets how long particles live, in seconds, plus a random variance. */
    public UMMLParticleSystem setLife(double seconds, double variance) {
        this.life = Math.max(0.001, seconds);
        this.lifeVariance = Math.max(0, variance);
        return this;
    }

    /** The base lifetime in seconds. */
    public double life() {
        return life;
    }

    /** Sets how big particles are drawn, plus a random variance. */
    public UMMLParticleSystem setSize(double size, double variance) {
        this.size = Math.max(0.5, size);
        this.sizeVariance = Math.max(0, variance);
        return this;
    }

    /** The base particle size in pixels. */
    public double size() {
        return size;
    }

    /** Particles start in this colour and fade to the end colour. */
    public UMMLParticleSystem setColor(Color color) {
        this.color = color == null ? Color.WHITE : color;
        return this;
    }

    /** The colour particles start in. */
    public Color color() {
        return color;
    }

    /**
     * The colour particles fade to as they die (default: fully transparent).
     * If you set this to the same colour as {@link #setColor(Color)}, particles
     * stay a solid colour and just vanish at the end.
     */
    public UMMLParticleSystem setEndColor(Color endColor) {
        this.endColor = endColor;
        return this;
    }

    /** The colour particles fade to. */
    public Color endColor() {
        return endColor;
    }

    /** Draws particles as this picture instead of a coloured circle. */
    public UMMLParticleSystem setImage(UMMLImage image) {
        this.image = image;
        return this;
    }

    /** The picture particles are drawn with, or null for coloured circles. */
    public UMMLImage image() {
        return image;
    }

    /** Caps how many particles can be alive at once (default 500). */
    public UMMLParticleSystem setMax(int max) {
        this.maxParticles = Math.max(1, max);
        return this;
    }

    /** The cap on live particles. */
    public int max() {
        return maxParticles;
    }

    // ========================================================================
    // Stepping
    // ========================================================================

    /**
     * Advances every particle and spawns new ones for the steady stream.
     * Called automatically by the renderer for registered systems, or call
     * it yourself.
     */
    public UMMLParticleSystem update(double deltaSeconds) {
        deltaSeconds = Math.max(0, deltaSeconds);

        if (enabled && rate > 0 && particles.size() < maxParticles) {
            spawnAccumulator += rate * deltaSeconds;
            while (spawnAccumulator >= 1 && particles.size() < maxParticles) {
                spawn();
                spawnAccumulator -= 1;
            }
            if (particles.size() >= maxParticles) {
                spawnAccumulator = 0;
            }
        }

        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.age += deltaSeconds;
            if (p.age >= p.life) {
                it.remove();
                continue;
            }
            p.vy += gravity * deltaSeconds;
            p.x += p.vx * deltaSeconds;
            p.y += p.vy * deltaSeconds;
        }
        return this;
    }

    // ========================================================================
    // Reading
    // ========================================================================

    /** The number of particles alive right now. */
    public int particleCount() {
        return particles.size();
    }

    /** The live particles (read-only). */
    public List<Particle> particles() {
        return List.copyOf(particles);
    }

    /** How far a particle at age a (0..1 of its life) is along its fade. */
    public static double ageFraction(double ageSeconds, double lifeSeconds) {
        if (lifeSeconds <= 0) {
            return 1;
        }
        return Math.max(0, Math.min(1, ageSeconds / lifeSeconds));
    }

    // ========================================================================
    // Internals
    // ========================================================================

    private void spawn() {
        if (particles.size() >= maxParticles) {
            return;
        }
        Particle p = new Particle();
        p.x = x;
        p.y = y;
        p.life = Math.max(0.001, life + (random.nextDouble() * 2 - 1) * lifeVariance);

        double s = Math.max(0, speed + (random.nextDouble() * 2 - 1) * speedVariance);
        double dir = angle + (random.nextDouble() * 2 - 1) * spread;
        double rad = Math.toRadians(dir);
        p.vx = Math.cos(rad) * s;
        p.vy = Math.sin(rad) * s;

        p.size = Math.max(0.5, size + (random.nextDouble() * 2 - 1) * sizeVariance);
        p.age = 0;
        particles.add(p);
    }
}
