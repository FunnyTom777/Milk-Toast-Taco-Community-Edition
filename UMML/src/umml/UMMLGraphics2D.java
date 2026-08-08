package umml;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;

/**
 * The UMML 1.5 rendering backend built on the standard Java2D graphics.
 *
 * <p>This is the "built in Java graphics" implementation of
 * {@link UMMLGraphics}. It wraps a {@link Graphics2D} and turns every UMML
 * draw call into the matching Java2D call. You normally never create one of
 * these yourself - {@link UMMLRenderer} does it for you, and the
 * {@code UMMLRendererTest} self test uses one directly against a
 * {@link java.awt.image.BufferedImage} so it can run without a window.
 *
 * <p>If you ever swap in a different rendering system, you write a new
 * {@code UMMLGraphics} implementation and nothing in MTT changes.
 */
public final class UMMLGraphics2D implements UMMLGraphics {

    private final Graphics2D g;
    private final int width;
    private final int height;
    private Color color = Color.WHITE;
    private Font font;

    /**
     * Wraps a Graphics2D. The given {@code g} must already be the right
     * size; width()/height() report the values passed here.
     */
    public UMMLGraphics2D(Graphics2D g, int width, int height) {
        this.g = g;
        this.width = width;
        this.height = height;
        this.font = g.getFont();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    /** Creates a backend that draws into the given image. Handy for self tests. */
    public static UMMLGraphics2D from(java.awt.image.BufferedImage image) {
        return new UMMLGraphics2D(image.createGraphics(), image.getWidth(), image.getHeight());
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public UMMLGraphics2D clear(Color c) {
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.Src);
        g.setColor(c);
        g.fillRect(0, 0, width, height);
        g.setComposite(old);
        return this;
    }

    @Override
    public UMMLGraphics2D setColor(Color c) {
        this.color = c;
        g.setColor(c);
        return this;
    }

    @Override
    public Color color() {
        return color;
    }

    @Override
    public UMMLGraphics2D setFont(Font f) {
        this.font = f;
        g.setFont(f);
        return this;
    }

    @Override
    public Font font() {
        return font;
    }

    @Override
    public UMMLGraphics2D fillRect(double x, double y, double w, double h) {
        g.fillRect(ix(x), iy(y), iw(w), ih(h));
        return this;
    }

    @Override
    public UMMLGraphics2D drawRect(double x, double y, double w, double h) {
        g.drawRect(ix(x), iy(y), iw(w), ih(h));
        return this;
    }

    @Override
    public UMMLGraphics2D fillCircle(double x, double y, double radius) {
        g.fillOval(ix(x - radius), iy(y - radius), iw(radius * 2), ih(radius * 2));
        return this;
    }

    @Override
    public UMMLGraphics2D drawCircle(double x, double y, double radius) {
        g.drawOval(ix(x - radius), iy(y - radius), iw(radius * 2), ih(radius * 2));
        return this;
    }

    @Override
    public UMMLGraphics2D drawLine(double x1, double y1, double x2, double y2) {
        g.drawLine(ix(x1), iy(y1), ix(x2), iy(y2));
        return this;
    }

    @Override
    public UMMLGraphics2D drawText(String text, double x, double y) {
        if (text != null) g.drawString(text, (float) x, (float) y);
        return this;
    }

    @Override
    public UMMLGraphics2D drawImage(UMMLImage image, double x, double y, double w, double h) {
        return drawImage(image, x, y, w, h, 0);
    }

    @Override
    public UMMLGraphics2D drawImage(UMMLImage image, double x, double y, double w, double h, double degrees) {
        if (image == null) return this;
        AffineTransform old = g.getTransform();
        if (degrees != 0) {
            double cx = x + w / 2.0;
            double cy = y + h / 2.0;
            g.translate(cx, cy);
            g.rotate(Math.toRadians(degrees));
            g.drawImage(image.buffered(), (int) Math.round(-w / 2.0), (int) Math.round(-h / 2.0),
                    (int) Math.round(w), (int) Math.round(h), null);
        } else {
            g.drawImage(image.buffered(), (int) Math.round(x), (int) Math.round(y),
                    (int) Math.round(w), (int) Math.round(h), null);
        }
        g.setTransform(old);
        return this;
    }

    @Override
    public UMMLGraphics2D drawImage(UMMLImage image, double x, double y, double w, double h,
                                    double sx, double sy, double sw, double sh) {
        if (image == null) return this;
        g.drawImage(image.buffered(),
                (int) Math.round(x), (int) Math.round(y), (int) Math.round(x + w), (int) Math.round(y + h),
                (int) Math.round(sx), (int) Math.round(sy),
                (int) Math.round(sx + sw), (int) Math.round(sy + sh), null);
        return this;
    }

    /** The raw Graphics2D, in case a game wants to do something UMML has no helper for. */
    public Graphics2D raw() {
        return g;
    }

    private static int ix(double v) {
        return (int) Math.round(v);
    }

    private static int iy(double v) {
        return (int) Math.round(v);
    }

    private static int iw(double v) {
        int n = (int) Math.round(Math.abs(v));
        return Math.max(0, n);
    }

    private static int ih(double v) {
        return iw(v);
    }
}
