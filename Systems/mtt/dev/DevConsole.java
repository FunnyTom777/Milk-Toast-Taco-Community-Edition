package mtt.dev;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import mtt.player.Player;

public class DevConsole extends JFrame {

    private static final Color BG_COLOR = new Color(20, 20, 25);
    private static final Color TEXT_COLOR = new Color(0, 220, 80);
    private static final Color INPUT_BG = new Color(30, 30, 35);
    private static final Color INPUT_TEXT = new Color(180, 255, 180);
    private static final Color PROMPT_COLOR = new Color(0, 180, 60);
    private static final Color ERROR_COLOR = new Color(255, 80, 80);
    private static final Color INFO_COLOR = new Color(100, 180, 255);
    private static final Color HIGHLIGHT_COLOR = new Color(255, 220, 50);
    private static final Color HEADER_COLOR = new Color(0, 200, 150);

    private JTextPane outputArea;
    private JTextField inputField;

    private final Map<String, DevCommand> commands = new LinkedHashMap<>();
    private final List<String> commandHistory = new ArrayList<>();
    private int historyIndex = -1;

    private Player player = new Player("Player", 200.0, List.of());

    public DevConsole() {
        super("Milk Toast Taco Dev Console");
        setupWindow();
        registerBuiltinCommands();
        SaveCommands.register(this);
        printHeader();
    }

    private void setupWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setMinimumSize(new Dimension(600, 400));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        outputArea = new JTextPane();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        outputArea.setBackground(BG_COLOR);
        outputArea.setForeground(TEXT_COLOR);
        outputArea.setCaretColor(TEXT_COLOR);
        outputArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUI(new DarkScrollBarUI());
        add(scrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(BG_COLOR);
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(50, 50, 55)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));

        JLabel prompt = new JLabel("> ");
        prompt.setFont(new Font("Consolas", Font.BOLD, 14));
        prompt.setForeground(PROMPT_COLOR);
        prompt.setPreferredSize(new Dimension(24, 24));
        inputPanel.add(prompt, BorderLayout.WEST);

        inputField = new JTextField();
        inputField.setFont(new Font("Consolas", Font.PLAIN, 14));
        inputField.setBackground(INPUT_BG);
        inputField.setForeground(INPUT_TEXT);
        inputField.setCaretColor(INPUT_TEXT);
        inputField.setBorder(BorderFactory.createEmptyBorder());
        inputField.setOpaque(true);
        inputField.addKeyListener(new InputHandler());
        inputPanel.add(inputField, BorderLayout.CENTER);

        add(inputPanel, BorderLayout.SOUTH);

        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                inputField.requestFocusInWindow();
            }
        });

        inputField.requestFocusInWindow();
    }

    private void registerBuiltinCommands() {
        registerCommand("help", "List all commands or get help for a specific command", "/help [command]", args -> {
            if (args.length > 0) {
                DevCommand cmd = commands.get(args[0].toLowerCase());
                if (cmd != null) {
                    printColored(INFO_COLOR, cmd.name() + " - " + cmd.description());
                    printColored(HIGHLIGHT_COLOR, "Usage: " + cmd.usage());
                } else {
                    printError("Unknown command: " + args[0]);
                }
            } else {
                printColored(HEADER_COLOR, "=== Dev Commands ===");
                for (DevCommand cmd : commands.values()) {
                    printColored(TEXT_COLOR, "  /" + padRight(cmd.name(), 14) + cmd.description());
                }
                printColored(INFO_COLOR, "Type /help <command> for usage info");
            }
        });

        registerCommand("clear", "Clear the console output", "/clear", args -> {
            outputArea.setText("");
            printHeader();
        });

        registerCommand("exit", "Close the dev console", "/exit", args -> {
            printColored(HIGHLIGHT_COLOR, "Shutting down...");
            dispose();
            System.exit(0);
        });

        registerCommand("echo", "Print a message to the console", "/echo <message>", args -> {
            printColored(TEXT_COLOR, String.join(" ", args));
        });

        registerCommand("sysinfo", "Show system information", "/sysinfo", args -> {
            printColored(HEADER_COLOR, "=== System Info ===");
            printColored(TEXT_COLOR, "  Java: " + System.getProperty("java.version"));
            printColored(TEXT_COLOR, "  OS: " + System.getProperty("os.name") + " " + System.getProperty("os.arch"));
            printColored(TEXT_COLOR, "  Cores: " + Runtime.getRuntime().availableProcessors());
            printColored(TEXT_COLOR, "  Max Memory: " + (Runtime.getRuntime().maxMemory() / 1024 / 1024) + " MB");
            printColored(TEXT_COLOR, "  Free Memory: " + (Runtime.getRuntime().freeMemory() / 1024 / 1024) + " MB");
            printColored(TEXT_COLOR, "  Working Dir: " + System.getProperty("user.dir"));
        });

        registerCommand("player", "Show player stats, money, and inventory", "/player", args -> {
            printColored(HEADER_COLOR, "=== " + player.name() + " ===");
            printColored(HIGHLIGHT_COLOR, "  Money: $" + String.format("%.2f", player.money()));
            printColored(INFO_COLOR, "  --- Inventory (" + player.inventory().size() + " items) ---");
            if (player.inventory().isEmpty()) {
                printColored(HIGHLIGHT_COLOR, "    (empty)");
            } else {
                for (String item : player.inventory()) {
                    printColored(TEXT_COLOR, "    - " + item);
                }
            }
        });

        registerCommand("setmoney", "Set the player's money to an exact amount", "/setmoney <amount>", args -> {
            if (args.length == 0) {
                printError("Usage: /setmoney <amount>");
                return;
            }
            player.setMoney(parseDoubleOrDefault(args[0], 0));
            printColored(TEXT_COLOR, "Money set to $" + String.format("%.2f", player.money()));
        });

        registerCommand("addmoney", "Add money to the player", "/addmoney <amount>", args -> {
            if (args.length == 0) {
                printError("Usage: /addmoney <amount>");
                return;
            }
            double amount = parseDoubleOrDefault(args[0], 0);
            player.addMoney(amount);
            printColored(TEXT_COLOR, "+$" + String.format("%.2f", amount) + " | Balance: $" + String.format("%.2f", player.money()));
        });

        registerCommand("takemoney", "Remove money from the player", "/takemoney <amount>", args -> {
            if (args.length == 0) {
                printError("Usage: /takemoney <amount>");
                return;
            }
            double amount = parseDoubleOrDefault(args[0], 0);
            if (player.removeMoney(amount)) {
                printColored(ERROR_COLOR, "-$" + String.format("%.2f", amount) + " | Balance: $" + String.format("%.2f", player.money()));
            } else {
                printError("Not enough money! Has $" + String.format("%.2f", player.money()));
            }
        });

        registerCommand("giveitem", "Add an item to the player's inventory", "/giveitem <name>", args -> {
            if (args.length == 0) {
                printError("Usage: /giveitem <name>");
                return;
            }
            String name = String.join(" ", args);
            player.addItem(name);
            printColored(TEXT_COLOR, "Added '" + name + "' to inventory");
        });

        registerCommand("inventory", "List the player's inventory", "/inventory", args -> {
            printColored(INFO_COLOR, "=== Inventory (" + player.inventory().size() + " items) ===");
            if (player.inventory().isEmpty()) {
                printColored(HIGHLIGHT_COLOR, "  (empty)");
            } else {
                for (String item : player.inventory()) {
                    printColored(TEXT_COLOR, "  - " + item);
                }
            }
        });

        registerCommand("rename", "Rename the current player", "/rename <name>", args -> {
            if (args.length == 0) {
                printError("Usage: /rename <name>");
                return;
            }
            player.rename(String.join(" ", args));
            printColored(TEXT_COLOR, "Player renamed to: " + player.name());
        });

        registerCommand("newgame", "Create a new player with a name (resets the current player)", "/newgame <name>", args -> {
            String name = args.length > 0 ? String.join(" ", args) : "Player";
            player = new Player(name, 200.0, List.of());
            printColored(TEXT_COLOR, "New game started: " + player.name());
        });
    }

    public void registerCommand(String name, String description, String usage, Consumer<String[]> handler) {
        commands.put(name.toLowerCase(), new DevCommand() {
            public String name() {
                return name;
            }

            public String description() {
                return description;
            }

            public String usage() {
                return usage;
            }

            public void execute(String[] args, DevConsole console) {
                handler.accept(args);
            }
        });
    }

    public void executeCommand(String input) {
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        String[] parts = trimmed.split("\\s+");
        String cmdName = parts[0].toLowerCase();
        if (cmdName.startsWith("/")) {
            cmdName = cmdName.substring(1);
        }
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);

        DevCommand cmd = commands.get(cmdName);
        if (cmd != null) {
            try {
                cmd.execute(args, this);
            } catch (Exception e) {
                printError("Command failed: " + e.getMessage());
            }
        } else {
            printError("Unknown command: /" + cmdName + " (type /help for available commands)");
        }
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    private void printHeader() {
        printColored(HEADER_COLOR, "");
        printColored(HEADER_COLOR, "Milk Toast Taco Development Console");
        printColored(HIGHLIGHT_COLOR, "         Community Edition Dev Console");
        printColored(INFO_COLOR, "         Type /help for available commands");
        printColored(HEADER_COLOR, "");
    }

    public void print(String text) {
        printColored(TEXT_COLOR, text);
    }

    public void printColored(Color color, String text) {
        StyledDocument doc = outputArea.getStyledDocument();
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setForeground(attrs, color);
        StyleConstants.setFontFamily(attrs, "Consolas");
        StyleConstants.setFontSize(attrs, 14);
        try {
            doc.insertString(doc.getLength(), text + "\n", attrs);
        } catch (Exception e) {
            // fallback
        }
        outputArea.setCaretPosition(doc.getLength());
    }

    public void printError(String text) {
        printColored(ERROR_COLOR, "[ERROR] " + text);
    }

    public void printInfo(String text) {
        printColored(INFO_COLOR, text);
    }

    public void printHeader(String text) {
        printColored(HEADER_COLOR, text);
    }

    public void printHighlight(String text) {
        printColored(HIGHLIGHT_COLOR, text);
    }

    private class InputHandler extends KeyAdapter {
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                String input = inputField.getText().trim();
                if (!input.isEmpty()) {
                    commandHistory.add(input);
                    historyIndex = commandHistory.size();
                    printColored(PROMPT_COLOR, "> " + input);
                    executeCommand(input);
                }
                inputField.setText("");
            } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                if (historyIndex > 0) {
                    historyIndex--;
                    inputField.setText(commandHistory.get(historyIndex));
                }
                e.consume();
            } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                if (historyIndex < commandHistory.size() - 1) {
                    historyIndex++;
                    inputField.setText(commandHistory.get(historyIndex));
                } else {
                    historyIndex = commandHistory.size();
                    inputField.setText("");
                }
                e.consume();
            } else if (e.getKeyCode() == KeyEvent.VK_TAB) {
                String partial = inputField.getText().trim().toLowerCase();
                if (partial.startsWith("/")) {
                    partial = partial.substring(1);
                    List<String> matches = new ArrayList<>();
                    for (String cmd : commands.keySet()) {
                        if (cmd.startsWith(partial)) {
                            matches.add(cmd);
                        }
                    }
                    if (matches.size() == 1) {
                        inputField.setText("/" + matches.get(0) + " ");
                    } else if (matches.size() > 1) {
                        printColored(INFO_COLOR, "Matches: " + String.join(", ", matches));
                    }
                }
                e.consume();
            }
        }
    }

    private static class DarkScrollBarUI extends BasicScrollBarUI {
        protected void configureScrollBarColors() {
            trackColor = BG_COLOR;
        }

        public void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            g.setColor(new Color(60, 60, 65));
            g.fillRect(thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height);
        }

        public void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            g.setColor(BG_COLOR);
            g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
        }
    }

    private double parseDoubleOrDefault(String s, double def) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static String padRight(String s, int n) {
        if (s.length() >= n) {
            return s + " ";
        }
        return s + " ".repeat(n - s.length());
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            // keep default look and feel
        }

        SwingUtilities.invokeLater(() -> {
            DevConsole console = new DevConsole();
            console.setVisible(true);
        });
    }
}
