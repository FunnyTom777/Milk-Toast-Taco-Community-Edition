package umml;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A mod that was discovered in the mods directory, whether it loaded
 * successfully or not. A mod that failed to load still appears in the
 * report's failed list so the calling MTT instance can tell the player
 * exactly what went wrong.
 */
public class UMMLMod {

    private final Path root;
    private final Path moddataFile;
    private final String moddataRaw;

    private String name = "";
    private String storeName = "";
    private String version = "";
    private String author = "";
    private String description = "";
    private final List<String> dependencies = new ArrayList<>();
    private final List<UMMLItem> items = new ArrayList<>();
    private final List<UMMLError> modErrors = new ArrayList<>();

    UMMLMod(Path root, Path moddataFile, String moddataRaw) {
        this.root = root;
        this.moddataFile = moddataFile;
        this.moddataRaw = moddataRaw;
    }

    /** The mod's folder. Never null. */
    public Path root() {
        return root;
    }

    /** The folder name, e.g. "MTT_BoatsDLC". */
    public String folderName() {
        return root.getFileName().toString();
    }

    /** The moddata.xml file, or null if the mod never had one. */
    public Path moddataFile() {
        return moddataFile;
    }

    /** The raw text of moddata.xml (useful for editors), or null if unavailable. */
    public String moddataRaw() {
        return moddataRaw;
    }

    /** The mod id from &lt;modname&gt;. Empty if the mod failed before the name could be read. */
    public String name() {
        return name;
    }

    public String storeName() {
        return storeName;
    }

    public String version() {
        return version;
    }

    public String author() {
        return author;
    }

    public String description() {
        return description;
    }

    /** Names this mod needs in order to work. */
    public List<String> dependencies() {
        return Collections.unmodifiableList(dependencies);
    }

    /** The items declared in &lt;modcontents&gt;. */
    public List<UMMLItem> items() {
        return Collections.unmodifiableList(items);
    }

    /** Problems attached to this specific mod. */
    public List<UMMLError> modErrors() {
        return Collections.unmodifiableList(modErrors);
    }

    /** True if every item resolved and no errors were recorded. */
    public boolean isFullyLoaded() {
        return modErrors.isEmpty();
    }

    /** Convenience: number of vehicle items in this mod. */
    public int vehicleCount() {
        int n = 0;
        for (UMMLItem item : items) {
            if (item.type().equalsIgnoreCase("vehicle")) n++;
        }
        return n;
    }

    void setName(String name) { this.name = name == null ? "" : name.trim(); }
    void setStoreName(String storeName) { this.storeName = storeName == null ? "" : storeName.trim(); }
    void setVersion(String version) { this.version = version == null ? "" : version.trim(); }
    void setAuthor(String author) { this.author = author == null ? "" : author.trim(); }
    void setDescription(String description) { this.description = description == null ? "" : description.trim(); }
    void addDependency(String dependency) {
        if (dependency != null && !dependency.trim().isEmpty()) dependencies.add(dependency.trim());
    }
    void addItem(UMMLItem item) { items.add(item); }
    void addError(UMMLError error) { modErrors.add(error); }

    @Override
    public String toString() {
        return storeName.isEmpty() ? folderName() : storeName;
    }
}
