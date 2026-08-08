package umuis;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The single window that shows every UMUIS menu. Menus are swapped in and
 * out of the same window instead of opening one JFrame per menu.
 *
 * <p>Each menu XML is parsed and rendered on the shared content panel.
 * Buttons either navigate to another menu (their {@code target}, resolved
 * relative to the current menu file), or run a built-in {@code action}:
 *
 * <ul>
 *   <li>{@code quit} - close the window and exit the app</li>
 *   <li>{@code close} - close the window</li>
 *   <li>{@code back} - go back to the previously shown menu</li>
 *   <li>{@code reload} - re-render the current menu</li>
 * </ul>
 *
 * <p>A navigation history is kept so {@code back} always returns to wherever
 * the user came from, no matter how deep they are.
 */
public final class UMUISWindow extends JFrame {

    private static final Color DEFAULT_BACKGROUND = new Color(20, 20, 25);
    private static final Color DEFAULT_FOREGROUND = new Color(224, 224, 224);

    private final JPanel content = new JPanel(null);
    private final Deque<Path> history = new ArrayDeque<>();

    public UMUISWindow(Path startMenu) {
        super("UMUIS");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        content.setBackground(DEFAULT_BACKGROUND);
        setContentPane(content);
        history.push(startMenu.toAbsolutePath().normalize());
        setLocationRelativeTo(null);
        render();
    }

    /** Re-renders the current menu (the head of the navigation history). */
    private void render() {
        Path current = history.peek();
        try {
            showMenu(UMUISParser.parse(current));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Could not load menu:\n" + current + "\n\n" + e.getMessage(),
                    "UMUIS - Menu Error", JOptionPane.ERROR_MESSAGE);
            if (history.size() > 1) {
                history.pop();
                render();
            } else {
                dispose();
            }
        }
    }

    private void showMenu(UMUISMenu menu) {
        content.removeAll();

        String title = menu.title();
        setTitle(title == null || title.isBlank() ? "UMUIS" : title);
        setSize(Math.max(menu.width(), 320), Math.max(menu.height(), 200));
        if (menu.background() != null) {
            content.setBackground(new Color(menu.background()));
        } else {
            content.setBackground(DEFAULT_BACKGROUND);
        }

        for (UMUISElement el : menu.elements()) {
            JComponent comp = createComponent(el, menu);
            if (comp == null) continue;
            comp.setBounds(el.x(), el.y(), el.width(), el.height());
            content.add(comp);
        }

        content.revalidate();
        content.repaint();
    }

    private JComponent createComponent(UMUISElement el, UMUISMenu menu) {
        switch (el.type()) {
            case UMUISElement.TYPE_LABEL -> {
                JLabel label = new JLabel(el.text(), alignment(el.align()));
                label.setFont(font(el));
                label.setForeground(foreground(el, menu));
                return label;
            }
            case UMUISElement.TYPE_BUTTON -> {
                JButton button = new JButton(el.text());
                button.setFont(font(el));
                button.setOpaque(true);
                button.setContentAreaFilled(true);
                button.setBackground(buttonBackground(menu));
                button.setForeground(buttonForeground(el, menu));
                button.setFocusPainted(false);
                button.addActionListener(actionListener(el));
                return button;
            }
            case UMUISElement.TYPE_TEXTFIELD -> {
                JTextField field = new JTextField(el.text());
                field.setFont(font(el));
                return field;
            }
            default -> {
                return null;
            }
        }
    }

    private ActionListener actionListener(UMUISElement el) {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String action = el.action();
                if (action != null) {
                    switch (action) {
                        case "quit" -> {
                            dispose();
                            System.exit(0);
                            return;
                        }
                        case "close" -> {
                            dispose();
                            return;
                        }
                        case "back" -> {
                            if (history.size() > 1) {
                                history.pop();
                                render();
                            }
                            return;
                        }
                        case "reload" -> {
                            render();
                            return;
                        }
                        default -> { /* unknown actions are ignored */ }
                    }
                }
                String target = el.target();
                if (target != null && !target.isBlank()) {
                    Path resolved = history.peek().getParent().resolve(target).normalize();
                    if (!Files.isRegularFile(resolved)) {
                        JOptionPane.showMessageDialog(UMUISWindow.this,
                                "Menu not found:\n" + resolved,
                                "UMUIS - Menu Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    history.push(resolved);
                    render();
                }
            }
        };
    }

    private Font font(UMUISElement el) {
        int style = el.bold() ? Font.BOLD : Font.PLAIN;
        return new Font("SansSerif", style, el.size());
    }

    private Color foreground(UMUISElement el, UMUISMenu menu) {
        if (el.color() != null) return new Color(el.color());
        return isDark(menu) ? DEFAULT_FOREGROUND : new Color(20, 20, 25);
    }

    private Color buttonForeground(UMUISElement el, UMUISMenu menu) {
        if (el.color() != null) return new Color(el.color());
        return isDark(menu) ? new Color(235, 235, 240) : new Color(20, 20, 25);
    }

    private Color buttonBackground(UMUISMenu menu) {
        Color bg = menu.background() == null ? DEFAULT_BACKGROUND : new Color(menu.background());
        return isDark(menu) ? blend(bg, Color.WHITE, 0.18f) : blend(bg, Color.BLACK, 0.12f);
    }

    /** True when the menu background is dark, so light text reads well. */
    private boolean isDark(UMUISMenu menu) {
        Color bg = menu.background() == null ? DEFAULT_BACKGROUND : new Color(menu.background());
        double luminance = (0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue()) / 255.0;
        return luminance < 0.5;
    }

    private static Color blend(Color a, Color b, float fraction) {
        int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * fraction);
        int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * fraction);
        int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * fraction);
        return new Color(r, g, bl);
    }

    private int alignment(String align) {
        if (align == null) return SwingConstants.LEFT;
        switch (align.toLowerCase()) {
            case "center" -> {
                return SwingConstants.CENTER;
            }
            case "right" -> {
                return SwingConstants.RIGHT;
            }
            default -> {
                return SwingConstants.LEFT;
            }
        }
    }
}
