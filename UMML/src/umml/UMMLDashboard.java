package umml;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * UMML 1.0 - the Swing graphical dashboard.
 *
 * Three tabs:
 *   Mods      - pick a mods directory, scan it, and inspect loaded/failed mods
 *   Saves     - browse saves/&lt;version&gt;/&lt;slot&gt;.xml, view and
 *               lightly edit save contents, create and delete saves
 *   Self Test - run the mod loading and save system self tests, capturing
 *               their console output
 *
 * Run with: java umml.UMMLDashboard
 */
public final class UMMLDashboard {

    private UMMLDashboard() {}

    public static void main(String[] args) {
        applyDarkTheme();
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("UMML - Unified MTT Mod Loader & Save System");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("Mods", new ModsPanel());
            tabs.addTab("Saves", new SavesPanel());
            tabs.addTab("Self Test", new SelfTestPanel());
            frame.add(tabs);
            frame.setSize(1000, 700);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    /** Finds the first existing directory among the candidates (CWD-relative). */
    private static String findDefaultDir(String[] candidates) {
        for (String candidate : candidates) {
            Path p = Path.of(candidate);
            if (Files.isDirectory(p)) return p.toAbsolutePath().normalize().toString();
        }
        return null;
    }

    private static void showError(JComponent parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private static void showInfo(JComponent parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    // ========================================================================
    // Mods tab
    // ========================================================================

    static class ModsPanel extends JPanel {
        private final JTextField dirField = new JTextField();
        private final JCheckBox strict = new JCheckBox("Strict");
        private final JCheckBox verbose = new JCheckBox("Verbose");
        private final DefaultTableModel loadedModel =
                new DefaultTableModel(new Object[]{"Folder", "Name", "Version", "Author", "Items", "Vehicles", "Depends on"}, 0) {
                    @Override public boolean isCellEditable(int row, int column) { return false; }
                };
        private final DefaultTableModel failedModel =
                new DefaultTableModel(new Object[]{"Folder", "Severity", "Type", "Message"}, 0) {
                    @Override public boolean isCellEditable(int row, int column) { return false; }
                };
        private final JTextArea log = new JTextArea();
        private final JLabel summary = new JLabel(" ");

        ModsPanel() {
            setLayout(new BorderLayout(6, 6));
            setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

            String def = findDefaultDir(new String[]{"MTT_Mods", "../MTT_Mods", "../../MTT_Mods"});
            dirField.setText(def == null ? "MTT_Mods" : def);
            dirField.setPreferredSize(new Dimension(420, 26));

            JButton browse = new JButton("Browse...");
            JButton scan = new JButton("Scan Mods");

            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            top.add(new JLabel("Mods directory:"));
            top.add(dirField);
            top.add(browse);
            top.add(scan);
            top.add(strict);
            top.add(verbose);

            browse.addActionListener(e -> chooseDirectory(dirField, this));
            scan.addActionListener(e -> scan());

            JTable loadedTable = new JTable(loadedModel);
            loadedTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            loadedTable.setFillsViewportHeight(true);
            loadedTable.setRowHeight(20);
            JTable failedTable = new JTable(failedModel);
            failedTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            failedTable.setFillsViewportHeight(true);
            failedTable.setRowHeight(20);

            setColumnWidths(loadedTable, new int[]{180, 180, 70, 120, 60, 80, 180});
            setColumnWidths(failedTable, new int[]{180, 80, 160, 480});

            JTabbedPane inner = new JTabbedPane();
            inner.addTab("Loaded", new JScrollPane(loadedTable));
            inner.addTab("Failed", new JScrollPane(failedTable));

            log.setEditable(false);
            log.setFont(new Font("Consolas", Font.PLAIN, 12));
            log.setLineWrap(true);
            log.setWrapStyleWord(true);

            JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, inner, new JScrollPane(log));
            split.setResizeWeight(0.7);
            split.setDividerLocation(380);

            add(top, BorderLayout.NORTH);
            add(split, BorderLayout.CENTER);
            add(summary, BorderLayout.SOUTH);
        }

        private void scan() {
            String dir = dirField.getText().trim();
            if (dir.isEmpty()) {
                showError(this, "Scan Mods", "Enter a mods directory first.");
                return;
            }
            UMMLOptions options = UMMLOptions.defaults().strict(strict.isSelected());
            UMMLReport report = UMML.scan(dir, options);

            loadedModel.setRowCount(0);
            for (UMMLMod mod : report.loadedMods()) {
                loadedModel.addRow(new Object[]{
                        mod.folderName(),
                        mod.name(),
                        mod.version(),
                        mod.author(),
                        mod.items().size(),
                        mod.vehicleCount(),
                        String.join(", ", mod.dependencies())
                });
            }

            failedModel.setRowCount(0);
            for (UMMLMod mod : report.failedMods()) {
                if (mod.modErrors().isEmpty()) {
                    failedModel.addRow(new Object[]{mod.folderName(), "ERROR", "-", "mod failed to load"});
                }
                for (UMMLError err : mod.modErrors()) {
                    failedModel.addRow(new Object[]{
                            mod.folderName(),
                            err.severity(),
                            err.type(),
                            err.message() + (err.detail() == null || err.detail().isEmpty() ? "" : " -> " + err.detail())
                    });
                }
            }

            StringBuilder sb = new StringBuilder();
            if (report.errorCount() > 0) {
                for (UMMLError err : report.errors()) {
                    sb.append(err).append('\n');
                }
            } else {
                sb.append("No errors or warnings.\n");
            }
            if (verbose.isSelected()) {
                sb.append("\n-- Loaded mod contents --\n");
                for (UMMLMod mod : report.loadedMods()) {
                    sb.append(mod.folderName()).append(":\n");
                    for (UMMLItem item : mod.items()) {
                        sb.append("    ").append(item).append('\n');
                    }
                    if (mod.items().isEmpty()) sb.append("    (no items)\n");
                }
            }
            log.setText(sb.toString());
            log.setCaretPosition(0);

            summary.setText("Loaded " + report.modCount() + " | Failed " + report.failedCount()
                    + " | Items " + report.itemCount() + " | Vehicles " + report.vehicleCount()
                    + " | Errors " + report.errorCount());
        }
    }

    // ========================================================================
    // Saves tab
    // ========================================================================

    static class SavesPanel extends JPanel {
        private final JTextField rootField = new JTextField();
        private final DefaultListModel<String> slotModel = new DefaultListModel<>();
        private final JList<String> slotList = new JList<>(slotModel);
        private final JLabel meta = new JLabel(" ");
        private final SaveEntryModel entryModel = new SaveEntryModel();
        private final JTable entryTable = new JTable(entryModel);

        private UMMLSaveSystem system;
        private int currentSlot = 0;

        SavesPanel() {
            setLayout(new BorderLayout(6, 6));
            setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

            String def = UMMLSaveSystem.find().root().toString();
            rootField.setText(def == null ? "saves" : def);
            rootField.setPreferredSize(new Dimension(420, 26));

            JButton browse = new JButton("Browse...");
            JButton createRoot = new JButton("Create Root");
            JButton refresh = new JButton("Refresh");
            JButton newSave = new JButton("New Save");
            JButton deleteSave = new JButton("Delete Save");
            JButton saveChanges = new JButton("Save Changes");

            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            top.add(new JLabel("Saves directory:"));
            top.add(rootField);
            top.add(browse);
            top.add(createRoot);
            top.add(refresh);

            browse.addActionListener(e -> chooseDirectory(rootField, this));
            createRoot.addActionListener(e -> {
                UMMLSaveSystem s = UMMLSaveSystem.open(rootField.getText().trim());
                UMMLSaveResult r = s.ensureRoot();
                if (r.isFailure()) showError(this, "Create Root", r.error().toString());
                else {
                    showInfo(this, "Create Root", "Created " + s.root());
                    refreshAll();
                }
            });
            refresh.addActionListener(e -> refreshAll());

            slotList.setVisibleRowCount(20);
            slotList.setBorder(BorderFactory.createTitledBorder("Save slots (1-20)"));
            slotList.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) loadSelectedSave();
            });

            entryTable.setFillsViewportHeight(true);
            entryTable.setRowHeight(20);
            entryModel.setEditable(true);

            JPanel left = new JPanel(new BorderLayout(6, 6));
            left.add(new JScrollPane(slotList), BorderLayout.CENTER);
            left.add(new JLabel("Click a slot to inspect it."), BorderLayout.SOUTH);

            JPanel right = new JPanel(new BorderLayout(6, 6));
            JPanel rightTop = new JPanel(new BorderLayout(6, 6));
            rightTop.add(meta, BorderLayout.NORTH);
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            buttons.add(newSave);
            buttons.add(deleteSave);
            buttons.add(saveChanges);
            rightTop.add(buttons, BorderLayout.SOUTH);
            right.add(rightTop, BorderLayout.NORTH);
            right.add(new JScrollPane(entryTable), BorderLayout.CENTER);

            newSave.addActionListener(e -> newSave());
            deleteSave.addActionListener(e -> deleteSave());
            saveChanges.addActionListener(e -> saveChanges());

            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
            split.setResizeWeight(0.35);
            split.setDividerLocation(240);

            add(top, BorderLayout.NORTH);
            add(split, BorderLayout.CENTER);
        }

        private void refreshAll() {
            system = UMMLSaveSystem.open(rootField.getText().trim());
            entryModel.setRoot(null);
            meta.setText(" ");
            currentSlot = 0;
            refreshSlots();
        }

        private void refreshSlots() {
            slotModel.clear();
            if (system == null) return;
            for (int s = UMMLSaveSystem.MIN_SLOT; s <= UMMLSaveSystem.MAX_SLOT; s++) {
                String text = "Slot " + s;
                if (system.slotExists(s)) {
                    UMMLSaveResult r = system.load(s);
                    if (r.isSuccess()) {
                        UMMLSaveData d = r.data();
                        String name = d.has("playername") ? d.getString("playername", "?") : "";
                        text += "  - " + (name.isEmpty() ? "used" : name);
                    } else {
                        text += "  - (unreadable)";
                    }
                }
                slotModel.addElement(text);
            }
        }

        private void loadSelectedSave() {
            currentSlot = slotList.getSelectedIndex() + 1;
            entryModel.setRoot(null);
            meta.setText(" ");
            if (currentSlot < UMMLSaveSystem.MIN_SLOT || system == null) return;
            if (!system.slotExists(currentSlot)) {
                meta.setText("Slot " + currentSlot + " is empty.");
                return;
            }
            UMMLSaveResult result = system.load(currentSlot);
            if (result.isFailure()) {
                meta.setText("Error: " + result.error().message());
                return;
            }
            UMMLSaveData data = result.data();
            entryModel.setRoot(data);
            String path = system.root().resolve(currentSlot + ".xml").toAbsolutePath().normalize().toString();
            meta.setText("Save: Slot " + currentSlot
                    + "   |   Saved by: " + (data.savedBy().isEmpty() ? "?" : data.savedBy())
                    + "   |   Saved at: " + (data.savedAt().isEmpty() ? "?" : data.savedAt())
                    + "\nFile: " + path);
        }

        private void newSave() {
            if (system == null) refreshAll();
            int slot = system.nextFreeSlot();
            if (slot == 0) {
                showInfo(this, "New Save", "All " + UMMLSaveSystem.MAX_SLOT + " save slots are full. Delete one first.");
                return;
            }
            UMMLSaveData data = new UMMLSaveData();
            data.setSavedBy("UMML Dashboard");
            data.setString("playername", "New Player");
            data.setInt("money", 5000);
            UMMLSaveResult result = system.save(slot, data);
            if (result.isFailure()) {
                showError(this, "New Save", result.error().toString());
                return;
            }
            refreshAll();
            slotList.setSelectedIndex(slot - 1);
        }

        private void deleteSave() {
            if (currentSlot < UMMLSaveSystem.MIN_SLOT || system == null) {
                showInfo(this, "Delete Save", "Select a save slot first.");
                return;
            }
            if (!system.slotExists(currentSlot)) {
                showInfo(this, "Delete Save", "Slot " + currentSlot + " is already empty.");
                return;
            }
            int answer = JOptionPane.showConfirmDialog(this,
                    "Delete save slot " + currentSlot + "?", "Delete Save",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer != JOptionPane.YES_OPTION) return;
            UMMLSaveResult result = system.delete(currentSlot);
            if (result.isFailure()) showError(this, "Delete Save", result.error().toString());
            refreshSlots();
            loadSelectedSave();
        }

        private void saveChanges() {
            if (currentSlot < UMMLSaveSystem.MIN_SLOT || system == null || entryModel.getRootData() == null) {
                showInfo(this, "Save Changes", "Select a save slot first.");
                return;
            }
            UMMLSaveResult result = system.save(currentSlot, entryModel.getRootData());
            if (result.isFailure()) {
                showError(this, "Save Changes", result.error().toString());
                return;
            }
            showInfo(this, "Save Changes", "Saved slot " + currentSlot);
            refreshSlots();
            loadSelectedSave();
        }
    }

    /** Table model that shows a UMMLSaveData tree flattened into rows. */
    static class SaveEntryModel extends AbstractTableModel {
        private UMMLSaveData root;
        private boolean editable;
        private final List<Row> rows = new ArrayList<>();

        private static class Row {
            final String path;
            final String key;
            final String type;
            String value;
            final UMMLSaveData node;

            Row(String path, String key, String type, String value, UMMLSaveData node) {
                this.path = path;
                this.key = key;
                this.type = type;
                this.value = value;
                this.node = node;
            }
        }

        void setEditable(boolean editable) {
            this.editable = editable;
        }

        void setRoot(UMMLSaveData data) {
            this.root = data;
            rows.clear();
            if (data != null) flatten(data, "", rows);
            fireTableDataChanged();
        }

        UMMLSaveData getRootData() {
            return root;
        }

        private void flatten(UMMLSaveData node, String prefix, List<Row> out) {
            for (String key : node.keys()) {
                Object v = node.get(key);
                out.add(new Row(prefix, key, typeName(v), v == null ? "" : String.valueOf(v), node));
            }
            for (String group : node.groupNames()) {
                String next = prefix.isEmpty() ? group : prefix + "." + group;
                flatten(node.getGroup(group), next, out);
            }
        }

        private static String typeName(Object value) {
            if (value instanceof Integer) return "int";
            if (value instanceof Long) return "long";
            if (value instanceof Double) return "double";
            if (value instanceof Float) return "float";
            if (value instanceof Boolean) return "boolean";
            return "string";
        }

        @Override public int getRowCount() { return rows.size(); }

        @Override public int getColumnCount() { return 4; }

        @Override public String getColumnName(int column) {
            return switch (column) {
                case 0 -> "Group";
                case 1 -> "Key";
                case 2 -> "Type";
                default -> "Value";
            };
        }

        @Override public Object getValueAt(int rowIndex, int columnIndex) {
            Row r = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> r.path;
                case 1 -> r.key;
                case 2 -> r.type;
                default -> r.value;
            };
        }

        @Override public boolean isCellEditable(int rowIndex, int columnIndex) {
            return editable && columnIndex == 3;
        }

        @Override public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex != 3) return;
            Row r = rows.get(rowIndex);
            String text = aValue == null ? "" : String.valueOf(aValue);
            r.value = text;
            r.node.set(r.key, parseByType(r.type, text));
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }

    /** Parses an edited string back into its declared type; falls back to string. */
    static Object parseByType(String type, String text) {
        try {
            return switch (type) {
                case "int" -> Integer.parseInt(text.trim());
                case "long" -> Long.parseLong(text.trim());
                case "double" -> Double.parseDouble(text.trim());
                case "float" -> Float.parseFloat(text.trim());
                case "boolean" -> Boolean.parseBoolean(text.trim());
                default -> text;
            };
        } catch (NumberFormatException e) {
            return text;
        }
    }

    // ========================================================================
    // Self Test tab
    // ========================================================================

    static class SelfTestPanel extends JPanel {
        private final JTextArea out = new JTextArea();
        private final JButton modsBtn = new JButton("Test Mod Loading");
        private final JButton savesBtn = new JButton("Test Save System");
        private final JButton bothBtn = new JButton("Run All");

        SelfTestPanel() {
            setLayout(new BorderLayout(6, 6));
            setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            top.add(new JLabel("Run the UMML self tests. Output is captured below."));
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            buttons.add(modsBtn);
            buttons.add(savesBtn);
            buttons.add(bothBtn);
            top.add(buttons);

            out.setEditable(false);
            out.setFont(new Font("Consolas", Font.PLAIN, 12));
            out.append("UMML self test console.\n"
                    + "Pick a test to run. The mod loading test builds a temporary mods\n"
                    + "fixture; the save system test builds a temporary saves tree.\n\n");

            add(top, BorderLayout.NORTH);
            add(new JScrollPane(out), BorderLayout.CENTER);

            modsBtn.addActionListener(e -> runTest("mods"));
            savesBtn.addActionListener(e -> runTest("saves"));
            bothBtn.addActionListener(e -> runTest("both"));
        }

        private void runTest(String which) {
            setButtonsEnabled(false);
            new SwingWorker<Boolean, Void>() {
                private final StringBuilder captured = new StringBuilder();

                @Override protected Boolean doInBackground() {
                    PrintStream oldOut = System.out;
                    PrintStream oldErr = System.err;
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    PrintStream cap = new PrintStream(buffer, true);
                    System.setOut(cap);
                    System.setErr(cap);
                    boolean ok = true;
                    try {
                        switch (which) {
                            case "mods" -> {
                                captured.append(">>> Mod loading self test\n\n");
                                ok = UMMLSelfTest.runTests();
                            }
                            case "saves" -> {
                                captured.append(">>> Save system self test\n\n");
                                ok = UMMLSaveSystemTest.runTests();
                            }
                            default -> {
                                captured.append(">>> Mod loading self test\n\n");
                                ok = UMMLSelfTest.runTests();
                                captured.append(buffer.toString());
                                captured.append("\n\n>>> Save system self test\n\n");
                                buffer.reset();
                                ok = UMMLSaveSystemTest.runTests() && ok;
                            }
                        }
                    } catch (IOException e) {
                        cap.println("Test could not run: " + e);
                        ok = false;
                    } finally {
                        captured.append(buffer.toString());
                        System.setOut(oldOut);
                        System.setErr(oldErr);
                    }
                    captured.append("\n>>> ").append(ok ? "ALL TESTS PASSED" : "TESTS FAILED").append("\n\n");
                    return ok;
                }

                @Override protected void done() {
                    out.append(captured.toString());
                    out.setCaretPosition(out.getDocument().getLength());
                    setButtonsEnabled(true);
                }
            }.execute();
        }

        private void setButtonsEnabled(boolean enabled) {
            modsBtn.setEnabled(enabled);
            savesBtn.setEnabled(enabled);
            bothBtn.setEnabled(enabled);
        }
    }

    // ========================================================================
    // Shared helpers
    // ========================================================================

    private static void chooseDirectory(JTextField field, JComponent parent) {
        JFileChooser chooser = new JFileChooser(field.getText().trim());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            field.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private static void setColumnWidths(JTable table, int[] widths) {
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    private static void applyDarkTheme() {
        Color bg = new Color(30, 30, 30);
        Color panel = new Color(37, 37, 38);
        Color input = new Color(45, 45, 48);
        Color fg = new Color(220, 220, 220);
        Color accent = new Color(78, 201, 176);
        Color select = new Color(9, 71, 113);
        Color thumb = new Color(62, 62, 66);

        UIManager.put("Panel.background", panel);
        UIManager.put("OptionPane.background", panel);
        UIManager.put("OptionPane.messageForeground", fg);
        UIManager.put("Label.foreground", fg);
        UIManager.put("Button.background", input);
        UIManager.put("Button.foreground", fg);
        UIManager.put("Button.select", new Color(62, 62, 66));
        UIManager.put("Button.focus", input);
        UIManager.put("CheckBox.background", panel);
        UIManager.put("CheckBox.foreground", fg);
        UIManager.put("RadioButton.background", panel);
        UIManager.put("RadioButton.foreground", fg);
        UIManager.put("TextField.background", input);
        UIManager.put("TextField.foreground", fg);
        UIManager.put("TextField.caretForeground", fg);
        UIManager.put("TextField.selectionBackground", select);
        UIManager.put("TextArea.background", bg);
        UIManager.put("TextArea.foreground", fg);
        UIManager.put("TextArea.caretForeground", fg);
        UIManager.put("TextArea.selectionBackground", select);
        UIManager.put("List.background", input);
        UIManager.put("List.foreground", fg);
        UIManager.put("List.selectionBackground", select);
        UIManager.put("List.selectionForeground", fg);
        UIManager.put("Table.background", input);
        UIManager.put("Table.foreground", fg);
        UIManager.put("Table.selectionBackground", select);
        UIManager.put("Table.selectionForeground", fg);
        UIManager.put("Table.gridColor", new Color(55, 55, 58));
        UIManager.put("TableHeader.background", input);
        UIManager.put("TableHeader.foreground", accent);
        UIManager.put("TabbedPane.background", panel);
        UIManager.put("TabbedPane.foreground", fg);
        UIManager.put("TabbedPane.selected", select);
        UIManager.put("TabbedPane.contentAreaColor", panel);
        UIManager.put("TabbedPane.unselectedBackground", input);
        UIManager.put("TabbedPane.focus", panel);
        UIManager.put("ScrollPane.background", bg);
        UIManager.put("Viewport.background", bg);
        UIManager.put("SplitPane.background", panel);
        UIManager.put("SplitPane.dividerFocusColor", accent);
        UIManager.put("ComboBox.background", input);
        UIManager.put("ComboBox.foreground", fg);
        UIManager.put("ComboBox.selectionBackground", select);
        UIManager.put("ComboBox.selectionForeground", fg);
        UIManager.put("ScrollBar.background", input);
        UIManager.put("ScrollBar.thumb", thumb);
        UIManager.put("ToolTip.background", input);
        UIManager.put("ToolTip.foreground", fg);
        UIManager.put("TitledBorder.titleColor", fg);
        UIManager.put("InternalFrame.background", panel);
    }
}
