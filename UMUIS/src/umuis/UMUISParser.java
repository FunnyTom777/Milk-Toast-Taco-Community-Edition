package umuis;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Turns a menu XML file into a {@link UMUISMenu}.
 *
 * <pre>
 * &lt;menu title="Main Menu" width="800" height="600" background="0x202025"&gt;
 *   &lt;element type="label" x="0" y="20" width="800" height="40"
 *            text="MILK TOAST TACO" align="center" size="28" bold="true" color="0xFFDC32"/&gt;
 *   &lt;element type="button" x="300" y="200" width="200" height="42"
 *            text="Options" target="options.xml"/&gt;
 *   &lt;element type="button" x="300" y="440" width="200" height="42"
 *            text="Quit" action="quit"/&gt;
 * &lt;/menu&gt;
 * </pre>
 *
 * <p>Supported element types are {@code label}, {@code button} and
 * {@code textfield}. Unknown element types and attributes are ignored for
 * forward compatibility. A malformed file or a missing {@code type} makes
 * {@link #parse(Path)} throw so the caller can fall back gracefully.
 */
public final class UMUISParser {

    public static final int DEFAULT_WIDTH = 800;
    public static final int DEFAULT_HEIGHT = 600;
    public static final int DEFAULT_SIZE = 14;

    private UMUISParser() {}

    /** Parses the given menu XML file. Throws when the file is missing or invalid. */
    public static UMUISMenu parse(Path file) throws Exception {
        String xml = Files.readString(file, StandardCharsets.UTF_8);
        return parseXml(file, xml);
    }

    /** Parses a menu from an XML string, used by the self tests. */
    public static UMUISMenu parseXml(Path source, String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new org.xml.sax.helpers.DefaultHandler());

        Document doc = builder.parse(new org.xml.sax.InputSource(new StringReader(xml)));
        Element root = doc.getDocumentElement();
        if (root == null || !root.getTagName().equals("menu")) {
            throw new Exception("expected a <menu> root element");
        }

        String title = root.getAttribute("title");
        int width = attrInt(root, "width", DEFAULT_WIDTH);
        int height = attrInt(root, "height", DEFAULT_HEIGHT);
        Integer background = attrColor(root, "background");

        List<UMUISElement> elements = new ArrayList<>();
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;
            Element el = (Element) node;
            if (!el.getTagName().equals("element")) continue;
            elements.add(parseElement(el));
        }

        return new UMUISMenu(source, title, width, height, background, elements);
    }

    private static UMUISElement parseElement(Element el) throws Exception {
        String type = el.getAttribute("type").toLowerCase(Locale.ROOT);
        if (type.isEmpty()) {
            throw new Exception("an <element> is missing its type attribute");
        }
        return new UMUISElement(
                type,
                attrInt(el, "x", 0),
                attrInt(el, "y", 0),
                attrInt(el, "width", 0),
                attrInt(el, "height", 0),
                el.getAttribute("text"),
                el.getAttribute("align"),
                attrInt(el, "size", DEFAULT_SIZE),
                attrBool(el, "bold", false),
                attrColor(el, "color"),
                el.getAttribute("target"),
                el.getAttribute("action"),
                el.getAttribute("id"));
    }

    private static int attrInt(Element el, String name, int def) {
        String raw = el.getAttribute(name);
        if (raw == null || raw.isBlank()) return def;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return def;
        }
    }

    private static boolean attrBool(Element el, String name, boolean def) {
        String raw = el.getAttribute(name);
        if (raw == null || raw.isBlank()) return def;
        return raw.equalsIgnoreCase("true") || raw.equals("1");
    }

    /** Parses colors like {@code 0xRRGGBB} or {@code #RRGGBB}; null when absent or invalid. */
    private static Integer attrColor(Element el, String name) {
        String raw = el.getAttribute(name);
        if (raw == null || raw.isBlank()) return null;
        String hex = raw.trim();
        if (hex.startsWith("0x") || hex.startsWith("0X")) {
            hex = hex.substring(2);
        } else if (hex.startsWith("#")) {
            hex = hex.substring(1);
        } else {
            return null;
        }
        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
