package umml;

/**
 * A single problem found while scanning mods.
 *
 * Severity WARNING means the mod still loaded (possibly partially).
 * Severity ERROR means either the mod did not load at all, or a meaningful
 * part of it could not be loaded. The exact meaning is described by the
 * {@link Type}.
 */
public class UMMLError {

    public enum Type {
        /** A subfolder has no moddata.xml. */
        MISSING_MODDATA,
        /** moddata.xml exists but could not be parsed as XML. */
        XML_PARSE,
        /** moddata.xml parsed but does not contain a usable &lt;moddata&gt; section. */
        INVALID_MODDATA,
        /** The &lt;modname&gt; tag is missing or empty. */
        MISSING_MODNAME,
        /** A mod content item is missing its required attributes. */
        INVALID_ITEM,
        /** A mod content item points to a path that does not exist. */
        ITEM_NOT_FOUND,
        /** A dependency could not be found among the discovered mods. */
        UNRESOLVED_DEPENDENCY,
        /** Two or more mods depend on each other. */
        CYCLIC_DEPENDENCY,
        /** A filesystem level error (directory unreadable, etc.). */
        IO_ERROR,
        /** Anything else. */
        OTHER
    }

    public enum Severity { WARNING, ERROR }

    private final Type type;
    private final Severity severity;
    private final String modFolder;
    private final String modName;
    private final String message;
    private final String detail;

    public UMMLError(Type type, Severity severity, String modFolder, String modName,
                     String message, String detail) {
        this.type = type;
        this.severity = severity;
        this.modFolder = modFolder;
        this.modName = modName;
        this.message = message;
        this.detail = detail;
    }

    public Type type() {
        return type;
    }

    public Severity severity() {
        return severity;
    }

    /** The name of the mod's folder, if known. May be null for directory-level errors. */
    public String modFolder() {
        return modFolder;
    }

    /** The mod's declared name, if it could be read. May be null. */
    public String modName() {
        return modName;
    }

    /** A human readable description of the problem. */
    public String message() {
        return message;
    }

    /** Extra detail such as the underlying exception message. May be null. */
    public String detail() {
        return detail;
    }

    public boolean isWarning() {
        return severity == Severity.WARNING;
    }

    public boolean isError() {
        return severity == Severity.ERROR;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(type).append("] ").append(severity);
        if (modFolder != null) sb.append(" (mod: ").append(modFolder).append(')');
        sb.append(": ").append(message);
        if (detail != null && !detail.isEmpty()) sb.append(" -> ").append(detail);
        return sb.toString();
    }
}
