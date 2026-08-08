package umml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a mod's moddata.xml into a {@link UMMLMod}.
 *
 * Real MTT moddata.xml files often contain TWO root elements
 * (&lt;moddata&gt; and &lt;modcontents&gt;) which is not valid XML. UMML
 * wraps the raw text in a synthetic root before parsing, which is why both
 * single-root and double-root files load correctly. Corrupt files do not
 * throw out of the loader - they produce a {@link UMMLError}.
 */
public final class ModParser {

    private ModParser() {}

    private static final Pattern XML_DECL = Pattern.compile("(?s)<\\?xml[^>]*\\?>");
    private static final Pattern ENCODING_DECL =
            Pattern.compile("<\\?xml[^>]*encoding\\s*=\\s*[\"']([^\"']+)[\"']");

    /** Item types that must point at a folder/file. Others (e.g. shop_brand) may omit the path. */
    private static final java.util.Set<String> REQUIRES_PATH =
            java.util.Set.of("vehicle", "constructable", "map");

    /** Result of a successful parse: a mod with metadata, items and per-mod warnings. */
    public static class ParsedModdata {
        final UMMLMod mod;
        final List<UMMLError> warnings = new ArrayList<>();
        ParsedModdata(UMMLMod mod) {
            this.mod = mod;
        }
    }

    /**
     * Reads and parses a moddata.xml file.
     *
     * @param moddataFile the moddata.xml file
     * @param options     scan options
     * @return a parsed mod, or null if the mod failed to load (a matching
     *         fatal error is returned via the second value instead)
     * @throws IOException if the file cannot be read at all
     */
    public static Result parse(Path moddataFile, UMMLOptions options) throws IOException {
        String raw = readText(moddataFile);
        UMMLMod mod = new UMMLMod(moddataFile.getParent(), moddataFile, raw);

        Document doc;
        try {
            doc = parseLenient(raw);
        } catch (Exception e) {
            mod.addError(new UMMLError(UMMLError.Type.XML_PARSE, UMMLError.Severity.ERROR,
                    mod.folderName(), null, "moddata.xml could not be parsed as XML", e.getMessage()));
            return Result.failed(mod);
        }

        Element wrappedRoot = doc.getDocumentElement();
        NodeList dataNodes = wrappedRoot.getElementsByTagName("moddata");
        if (dataNodes.getLength() == 0) {
            mod.addError(new UMMLError(UMMLError.Type.INVALID_MODDATA, UMMLError.Severity.ERROR,
                    mod.folderName(), null, "moddata.xml has no <moddata> section", null));
            return Result.failed(mod);
        }

        if (dataNodes.getLength() > 1 && options.warnOnMultipleModdataSections()) {
            mod.addError(new UMMLError(UMMLError.Type.INVALID_MODDATA, UMMLError.Severity.WARNING,
                    mod.folderName(), null, "moddata.xml has more than one <moddata> section",
                    dataNodes.getLength() + " found; using the first"));
        }

        Element data = (Element) dataNodes.item(0);
        mod.setName(text(data, "modname"));
        if (mod.name().isEmpty()) {
            mod.addError(new UMMLError(UMMLError.Type.MISSING_MODNAME, UMMLError.Severity.ERROR,
                    mod.folderName(), null, "moddata.xml has no <modname>", null));
            return Result.failed(mod);
        }

        mod.setStoreName(text(data, "storename"));
        mod.setVersion(text(data, "modversion"));
        mod.setAuthor(text(data, "modauthor"));
        mod.setDescription(text(data, "moddescription"));

        for (String dep : splitList(text(data, "moddependencies"))) {
            mod.addDependency(dep);
        }

        NodeList contentsNodes = wrappedRoot.getElementsByTagName("modcontents");
        if (contentsNodes.getLength() > 0) {
            Element contents = (Element) contentsNodes.item(0);
            NodeList itemNodes = contents.getElementsByTagName("item");
            for (int i = 0; i < itemNodes.getLength(); i++) {
                parseItem(mod, (Element) itemNodes.item(i), options);
            }
        }

        return Result.success(mod);
    }

    private static void parseItem(UMMLMod mod, Element item, UMMLOptions options) {
        String type = attr(item, "type");
        String name = attr(item, "name");
        String path = attr(item, "path");

        java.util.Map<String, String> attributes = new java.util.LinkedHashMap<>();
        if (item.hasAttributes()) {
            for (int i = 0; i < item.getAttributes().getLength(); i++) {
                org.w3c.dom.Node n = item.getAttributes().item(i);
                attributes.put(n.getNodeName(), n.getNodeValue());
            }
        }

        UMMLItem ummlItem = new UMMLItem(type, name, path, attributes);
        if (!ummlItem.hasPath()) {
            // Item types like "shop_brand" legitimately have no path. Only
            // types that must point at a folder are errors without one.
            if (REQUIRES_PATH.contains(type.toLowerCase())) {
                mod.addError(new UMMLError(UMMLError.Type.INVALID_ITEM, UMMLError.Severity.ERROR,
                        mod.folderName(), mod.name(),
                        "content item has no path attribute: " + display(type, name), null));
            }
        } else if (options.resolveItemPaths()) {
            ummlItem.setResolvedPath(resolveAgainstModRoot(mod.root(), path));
            if (ummlItem.pathMissing()) {
                mod.addError(new UMMLError(UMMLError.Type.ITEM_NOT_FOUND, UMMLError.Severity.ERROR,
                        mod.folderName(), mod.name(),
                        "content item path does not exist: " + path,
                        String.valueOf(ummlItem.resolvedPath())));
            }
        }
        mod.addItem(ummlItem);
    }

    /**
     * Resolves an item path the way the classic loaders did. Mods write
     * paths like "../vehicles/X" but mean "vehicles/X" relative to the mod
     * folder itself (the "../" is decorative). Absolute paths are used as-is.
     */
    static Path resolveAgainstModRoot(Path modRoot, String itemPath) {
        String p = itemPath.replace('\\', '/');
        while (p.startsWith("./") || p.startsWith("../")) p = p.substring(p.indexOf('/') + 1);
        if (p.startsWith("/")) {
            return Path.of(p).normalize();
        }
        return modRoot.resolve(p).normalize();
    }

    // ========================================================================
    // XML helpers
    // ========================================================================

    private static Document parseLenient(String raw)
            throws ParserConfigurationException, SAXException, IOException {
        String content = stripXmlDeclaration(raw).trim();
        if (content.isEmpty()) {
            throw new SAXException("file is empty");
        }
        String wrapped = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<modroot>" + content + "</modroot>";

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        // Keep corrupt files quiet - no [Fatal Error] spam on stderr. The
        // SAXException is still thrown and converted into an UMMLError.
        builder.setErrorHandler(new org.xml.sax.helpers.DefaultHandler());
        return builder.parse(new InputSource(new StringReader(wrapped)));
    }

    private static String stripXmlDeclaration(String raw) {
        String s = raw;
        if (!s.isEmpty() && s.charAt(0) == '\uFEFF') s = s.substring(1);
        return XML_DECL.matcher(s).replaceAll("");
    }

    /** Reads the file honouring the encoding declared in the XML declaration (UTF-8 default). */
    private static String readText(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        Charset cs = StandardCharsets.UTF_8;
        String head = new String(bytes, StandardCharsets.UTF_8);
        Matcher m = ENCODING_DECL.matcher(head);
        if (m.find()) {
            try {
                cs = Charset.forName(m.group(1));
            } catch (Exception ignored) {
                // fall back to UTF-8
            }
        }
        String text = new String(bytes, cs);
        if (!text.isEmpty() && text.charAt(0) == '\uFEFF') text = text.substring(1);
        return text;
    }

    private static String text(Element parent, String tag) {
        NodeList list = parent.getElementsByTagName(tag);
        if (list.getLength() == 0) return "";
        return list.item(0).getTextContent().trim();
    }

    private static String attr(Element element, String name) {
        return element.hasAttribute(name) ? element.getAttribute(name) : "";
    }

    private static List<String> splitList(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) return out;
        for (String part : raw.split("[,;]")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) out.add(trimmed);
        }
        return out;
    }

    private static String display(String type, String name) {
        return name == null || name.isEmpty() ? (type == null || type.isEmpty() ? "<unnamed>" : type) : name;
    }

    // ========================================================================
    // Result
    // ========================================================================

    /** Small result wrapper so the scanner can distinguish success from failure. */
    public static final class Result {
        private final UMMLMod mod;
        private final boolean success;

        private Result(UMMLMod mod, boolean success) {
            this.mod = mod;
            this.success = success;
        }

        static Result success(UMMLMod mod) {
            return new Result(mod, true);
        }

        static Result failed(UMMLMod mod) {
            return new Result(mod, false);
        }

        public UMMLMod mod() {
            return mod;
        }

        public boolean success() {
            return success;
        }
    }
}
