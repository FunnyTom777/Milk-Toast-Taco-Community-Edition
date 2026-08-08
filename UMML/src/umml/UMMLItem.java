package umml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One entry inside a mod's &lt;modcontents&gt; section, e.g. a vehicle,
 * constructable, map, or shop_brand.
 */
public class UMMLItem {

    private final String type;
    private final String name;
    private final String path;
    private final Map<String, String> attributes;
    private Path resolvedPath;
    private boolean pathMissing;

    public UMMLItem(String type, String name, String path) {
        this(type, name, path, null);
    }

    public UMMLItem(String type, String name, String path, Map<String, String> attributes) {
        this.type = type == null ? "" : type.trim();
        this.name = name == null ? "" : name.trim();
        this.path = path == null ? "" : path.trim();
        this.attributes = attributes == null || attributes.isEmpty()
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }

    /** The item type, e.g. "vehicle", "constructable", "map". */
    public String type() {
        return type;
    }

    /** The display name from the moddata.xml item tag. */
    public String name() {
        return name;
    }

    /** The raw path attribute from the moddata.xml item tag. */
    public String path() {
        return path;
    }

    /**
     * Every attribute on the item tag (type, name, path, and any extras a
     * mod author adds, such as category, hp, year or price). Never null.
     */
    public Map<String, String> attributes() {
        return attributes;
    }

    /**
     * Looks up an attribute on the item tag, or returns an empty string if
     * it was not declared.
     */
    public String attribute(String name) {
        return attribute(name, "");
    }

    /** Looks up an attribute on the item tag with a fallback value. */
    public String attribute(String name, String fallback) {
        String value = attributes.get(name);
        return value == null ? fallback : value;
    }

    /**
     * The item path resolved against the mod root folder, or null if it
     * could not be resolved (missing path attribute, or path resolving
     * disabled).
     */
    public Path resolvedPath() {
        return resolvedPath;
    }

    void setResolvedPath(Path resolvedPath) {
        this.resolvedPath = resolvedPath;
        this.pathMissing = resolvedPath == null || !Files.exists(resolvedPath);
    }

    /** True if the item has a path attribute. */
    public boolean hasPath() {
        return !path.isEmpty();
    }

    /** True if the item's path was resolved but does not exist on disk. */
    public boolean pathMissing() {
        return pathMissing;
    }

    @Override
    public String toString() {
        return "[" + type + "] " + (name.isEmpty() ? path : name) + (path.isEmpty() ? "" : " -> " + path);
    }
}
