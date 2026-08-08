package umml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * The save game system for Milk Toast Taco Community Edition.
 *
 * <p>Saves always live in a {@code saves} folder at the root of the project,
 * one XML file per slot:
 *
 * <pre>
 * saves/
 *   1.xml  2.xml  3.xml  ...  20.xml
 * </pre>
 *
 * <p>There are no version folders - Milk Toast Taco is just "MTT" (or MTT
 * Community Edition). The MTTV39/40/41 version folders were an old idea and
 * are gone; the game has 20 numbered save slots instead.
 *
 * <p>Each save is a dynamic XML file (see {@link UMMLSaveData}). Operations
 * never throw - they return a {@link UMMLSaveResult} that either carries the
 * loaded data or a {@link UMMLError} explaining the failure.
 *
 * <pre>
 * UMMLSaveSystem saves = UMMLSaveSystem.find();
 * UMMLSaveResult result = saves.load(1);
 * if (result.isSuccess()) {
 *     UMMLSaveData data = result.data();
 *     int money = data.getInt("money", 0);
 * }
 * </pre>
 */
public class UMMLSaveSystem {

    /** The lowest usable save slot. */
    public static final int MIN_SLOT = 1;

    /** The highest usable save slot. */
    public static final int MAX_SLOT = 20;

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final Path root;

    private UMMLSaveSystem(Path root) {
        this.root = root;
    }

    /** Opens a save system rooted at the given directory. */
    public static UMMLSaveSystem open(String root) {
        return new UMMLSaveSystem(Path.of(root));
    }

    /** Opens a save system rooted at the given directory. */
    public static UMMLSaveSystem open(Path root) {
        return new UMMLSaveSystem(root);
    }

    /**
     * Opens the save system at the project root's {@code saves/} folder.
     * UMML is bundled with MTT Community Edition, so saves go to one fixed
     * place - {@code saves/} at the root of the project - instead of being
     * hunted down through parent directories.
     *
     * <p>The project root is found by walking up from the current directory
     * until a folder containing {@code Systems} (the MTT source root) is
     * found. If that never happens (e.g. a packaged build with no source
     * tree), saves fall back to {@code saves/} next to the current directory.
     * The folder is created on first save.
     */
    public static UMMLSaveSystem find() {
        return new UMMLSaveSystem(projectRoot().resolve("saves"));
    }

    /** Walks up from the current directory looking for the MTT project root. */
    private static Path projectRoot() {
        Path p = Path.of("").toAbsolutePath().normalize();
        while (p != null) {
            if (Files.isDirectory(p.resolve("Systems"))) return p;
            p = p.getParent();
        }
        return Path.of("").toAbsolutePath().normalize();
    }

    /** The root saves directory this system reads and writes. */
    public Path root() {
        return root;
    }

    /** True if the root saves directory already exists. */
    public boolean exists() {
        return Files.isDirectory(root);
    }

    /** Creates the root saves directory if it does not exist. */
    public UMMLSaveResult ensureRoot() {
        try {
            Files.createDirectories(root);
            return UMMLSaveResult.success();
        } catch (IOException e) {
            return UMMLSaveResult.failure(new UMMLError(UMMLError.Type.IO_ERROR, UMMLError.Severity.ERROR,
                    null, null, "could not create saves directory", String.valueOf(root)));
        }
    }

    // ========================================================================
    // Slot listing
    // ========================================================================

    /**
     * Every used slot (1-20), sorted ascending. Never throws - an unreadable
     * root yields an empty list. Files that do not look like a numbered slot
     * (e.g. {@code readme.txt}) are ignored.
     */
    public List<Integer> listSlots() {
        List<Integer> slots = new ArrayList<>();
        if (!Files.isDirectory(root)) return slots;
        try (var stream = Files.list(root)) {
            stream.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().matches("[0-9]+\\.xml"))
                    .forEach(p -> {
                        String name = p.getFileName().toString();
                        int slot = Integer.parseInt(name.substring(0, name.length() - 4));
                        if (slot >= MIN_SLOT && slot <= MAX_SLOT) slots.add(slot);
                    });
        } catch (IOException | NumberFormatException ignored) {
            // best-effort
        }
        slots.sort(Comparator.naturalOrder());
        return slots;
    }

    /** True if a save exists in the given slot. */
    public boolean slotExists(int slot) {
        return isValidSlot(slot) && Files.isRegularFile(saveFile(slot));
    }

    /** The first unused slot (1-20), or 0 when all slots are full. */
    public int nextFreeSlot() {
        List<Integer> used = listSlots();
        for (int s = MIN_SLOT; s <= MAX_SLOT; s++) {
            if (!used.contains(s)) return s;
        }
        return 0;
    }

    // ========================================================================
    // Save / load / delete / rename
    // ========================================================================

    /**
     * Writes a save to {@code saves/&lt;slot&gt;.xml}. The savedAt stamp is
     * set automatically if it is still empty.
     */
    public UMMLSaveResult save(int slot, UMMLSaveData data) {
        if (data == null) {
            return UMMLSaveResult.failure(new UMMLError(UMMLError.Type.INVALID_ITEM, UMMLError.Severity.ERROR,
                    null, null, "cannot save null save data", null));
        }
        UMMLSaveResult base = ensureRoot();
        if (base.isFailure()) return base;
        if (!isValidSlot(slot)) {
            return UMMLSaveResult.failure(invalidSlotError(slot));
        }
        if (data.savedAt().isEmpty()) {
            data.setSavedAt(LocalDateTime.now().format(TIMESTAMP));
        }
        Path file = saveFile(slot);
        try {
            String xml = toXml(data);
            Files.createDirectories(file.getParent());
            Files.writeString(file, xml, StandardCharsets.UTF_8);
            return UMMLSaveResult.success();
        } catch (IOException | RuntimeException e) {
            return UMMLSaveResult.failure(new UMMLError(UMMLError.Type.IO_ERROR, UMMLError.Severity.ERROR,
                    null, "slot " + slot, "could not write save file", String.valueOf(file)));
        }
    }

    /** Loads a save slot, or returns a failed result (never throws). */
    public UMMLSaveResult load(int slot) {
        if (!isValidSlot(slot)) {
            return UMMLSaveResult.failure(invalidSlotError(slot));
        }
        Path file = saveFile(slot);
        if (!Files.isRegularFile(file)) {
            return UMMLSaveResult.failure(new UMMLError(UMMLError.Type.IO_ERROR, UMMLError.Severity.ERROR,
                    null, "slot " + slot, "save file does not exist", String.valueOf(file)));
        }
        try {
            String xml = Files.readString(file, StandardCharsets.UTF_8);
            return UMMLSaveResult.success(fromXml(xml));
        } catch (IOException e) {
            return UMMLSaveResult.failure(new UMMLError(UMMLError.Type.IO_ERROR, UMMLError.Severity.ERROR,
                    null, "slot " + slot, "could not read save file", e.getMessage()));
        } catch (Exception e) {
            // Bad XML in a save file must never crash the caller.
            return UMMLSaveResult.failure(new UMMLError(UMMLError.Type.XML_PARSE, UMMLError.Severity.ERROR,
                    null, "slot " + slot, "save file could not be parsed as XML", e.getMessage()));
        }
    }

    /** Deletes a save slot. Returns a failed result if it did not exist. */
    public UMMLSaveResult delete(int slot) {
        if (!isValidSlot(slot)) {
            return UMMLSaveResult.failure(invalidSlotError(slot));
        }
        Path file = saveFile(slot);
        if (!Files.isRegularFile(file)) {
            return UMMLSaveResult.failure(new UMMLError(UMMLError.Type.IO_ERROR, UMMLError.Severity.ERROR,
                    null, "slot " + slot, "save file does not exist", String.valueOf(file)));
        }
        try {
            Files.delete(file);
            return UMMLSaveResult.success();
        } catch (IOException e) {
            return UMMLSaveResult.failure(new UMMLError(UMMLError.Type.IO_ERROR, UMMLError.Severity.ERROR,
                    null, "slot " + slot, "could not delete save file", e.getMessage()));
        }
    }

    /** Moves a save to another slot. Fails if either slot is invalid or the target is taken. */
    public UMMLSaveResult rename(int oldSlot, int newSlot) {
        if (!isValidSlot(oldSlot) || !isValidSlot(newSlot)) {
            int bad = isValidSlot(oldSlot) ? newSlot : oldSlot;
            return UMMLSaveResult.failure(invalidSlotError(bad));
        }
        Path from = saveFile(oldSlot);
        Path to = saveFile(newSlot);
        if (!Files.isRegularFile(from)) {
            return UMMLSaveResult.failure(new UMMLError(UMMLError.Type.IO_ERROR, UMMLError.Severity.ERROR,
                    null, "slot " + oldSlot, "save file does not exist", String.valueOf(from)));
        }
        if (Files.exists(to)) {
            return UMMLSaveResult.failure(new UMMLError(UMMLError.Type.IO_ERROR, UMMLError.Severity.ERROR,
                    null, "slot " + newSlot, "a save already exists in that slot", String.valueOf(to)));
        }
        try {
            Files.move(from, to);
            return UMMLSaveResult.success();
        } catch (IOException e) {
            return UMMLSaveResult.failure(new UMMLError(UMMLError.Type.IO_ERROR, UMMLError.Severity.ERROR,
                    null, "slot " + oldSlot, "could not rename save file", e.getMessage()));
        }
    }

    // ========================================================================
    // Internal helpers
    // ========================================================================

    private Path saveFile(int slot) {
        return root.resolve(slot + ".xml");
    }

    private static boolean isValidSlot(int slot) {
        return slot >= MIN_SLOT && slot <= MAX_SLOT;
    }

    private static UMMLError invalidSlotError(int slot) {
        return new UMMLError(UMMLError.Type.INVALID_ITEM, UMMLError.Severity.ERROR,
                null, null,
                "invalid save slot " + slot + " - use a slot between " + MIN_SLOT + " and " + MAX_SLOT,
                null);
    }

    // ========================================================================
    // XML serialization
    // ========================================================================

    /**
     * The save XML format:
     *
     * <pre>
     * &lt;save savedby="MTT Community Edition" savedat="2026-08-03T08:15:00"&gt;
     *   &lt;entry key="money" type="int"&gt;5000&lt;/entry&gt;
     *   &lt;entry key="playername" type="string"&gt;Bobby&lt;/entry&gt;
     *   &lt;group name="inventory"&gt;
     *     &lt;entry key="item_0" type="string"&gt;Wrench&lt;/entry&gt;
     *   &lt;/group&gt;
     * &lt;/save&gt;
     * </pre>
     */
    static String toXml(UMMLSaveData data) {
        StringBuilder sb = new StringBuilder(256);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<save");
        if (!data.savedBy().isEmpty()) sb.append(" savedby=\"").append(escape(data.savedBy())).append('"');
        if (!data.savedAt().isEmpty()) sb.append(" savedat=\"").append(escape(data.savedAt())).append('"');
        sb.append(">\n");
        writeNode(sb, data, 0);
        sb.append("</save>\n");
        return sb.toString();
    }

    private static void writeNode(StringBuilder sb, UMMLSaveData node, int depth) {
        String indent = "  ".repeat(depth + 1);
        for (Map.Entry<String, Object> e : node.values().entrySet()) {
            Object value = e.getValue();
            String type = typeOf(value);
            String text = value == null ? "" : String.valueOf(value);
            sb.append(indent)
                    .append("<entry key=\"").append(escape(e.getKey()))
                    .append("\" type=\"").append(type).append("\">")
                    .append(escape(text))
                    .append("</entry>\n");
        }
        for (String name : node.groupNames()) {
            sb.append(indent)
                    .append("<group name=\"").append(escape(name)).append("\">\n");
            writeNode(sb, node.getGroup(name), depth + 1);
            sb.append(indent).append("</group>\n");
        }
    }

    private static String typeOf(Object value) {
        if (value == null) return "string";
        if (value instanceof Integer) return "int";
        if (value instanceof Long) return "long";
        if (value instanceof Double) return "double";
        if (value instanceof Float) return "float";
        if (value instanceof Boolean) return "boolean";
        return "string";
    }

    static UMMLSaveData fromXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new org.xml.sax.helpers.DefaultHandler());

        Document doc = builder.parse(new org.xml.sax.InputSource(new StringReader(xml)));
        Element root = doc.getDocumentElement();
        if (root == null || !root.getTagName().equals("save")) {
            throw new Exception("expected a <save> root element");
        }

        UMMLSaveData data = new UMMLSaveData();
        data.setSavedBy(root.getAttribute("savedby"));
        data.setSavedAt(root.getAttribute("savedat"));
        readChildren(data, root);
        return data;
    }

    private static void readChildren(UMMLSaveData target, Element parent) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;
            Element el = (Element) node;
            switch (el.getTagName()) {
                case "entry" -> target.set(el.getAttribute("key"), parseValue(el.getAttribute("type"), el.getTextContent()));
                case "group" -> readChildren(target.group(el.getAttribute("name")), el);
                default -> { /* ignore unknown tags for forward compatibility */ }
            }
        }
    }

    private static Object parseValue(String type, String text) {
        if (text == null) return "";
        switch (type == null ? "" : type) {
            case "int":
                try { return Integer.parseInt(text.trim()); } catch (NumberFormatException ignored) { return text; }
            case "long":
                try { return Long.parseLong(text.trim()); } catch (NumberFormatException ignored) { return text; }
            case "double":
                try { return Double.parseDouble(text.trim()); } catch (NumberFormatException ignored) { return text; }
            case "float":
                try { return Float.parseFloat(text.trim()); } catch (NumberFormatException ignored) { return text; }
            case "boolean":
                String t = text.trim();
                if (t.equalsIgnoreCase("true") || t.equalsIgnoreCase("false")) return Boolean.parseBoolean(t);
                return text;
            default:
                return text;
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&apos;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "UMMLSaveSystem{root=" + root + "}";
    }
}
