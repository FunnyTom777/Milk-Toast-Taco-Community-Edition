package umml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The outcome of a {@link UMML#scan(Path, UMMLOptions)} call.
 *
 * Contains the mods that loaded (in dependency-safe load order), the mods
 * that failed to load, and every error/warning that was collected. A scan
 * never throws - anything that goes wrong is placed in this report.
 */
public class UMMLReport {

    private final List<UMMLMod> loadedMods;
    private final List<UMMLMod> failedMods;
    private final List<UMMLError> errors;
    private final String modsDirectory;

    UMMLReport(String modsDirectory, List<UMMLMod> loadedMods,
               List<UMMLMod> failedMods, List<UMMLError> errors) {
        this.modsDirectory = modsDirectory;
        this.loadedMods = Collections.unmodifiableList(new ArrayList<>(loadedMods));
        this.failedMods = Collections.unmodifiableList(new ArrayList<>(failedMods));
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
    }

    /** The directory that was scanned. */
    public String modsDirectory() {
        return modsDirectory;
    }

    /** Mods that loaded successfully, in load order (dependencies first). */
    public List<UMMLMod> loadedMods() {
        return loadedMods;
    }

    /** Mods that could not be loaded at all, with the reason attached to each. */
    public List<UMMLMod> failedMods() {
        return failedMods;
    }

    /**
     * Every problem found during the scan, in the order it was found.
     * Includes directory level errors plus each mod's individual errors.
     */
    public List<UMMLError> errors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public int modCount() {
        return loadedMods.size();
    }

    public int failedCount() {
        return failedMods.size();
    }

    public int errorCount() {
        return errors.size();
    }

    /** Total number of items (vehicles, constructables, maps, ...) across loaded mods. */
    public int itemCount() {
        int n = 0;
        for (UMMLMod mod : loadedMods) n += mod.items().size();
        return n;
    }

    /** Total number of vehicle items across loaded mods. */
    public int vehicleCount() {
        int n = 0;
        for (UMMLMod mod : loadedMods) n += mod.vehicleCount();
        return n;
    }

    /** Finds a loaded mod by its &lt;modname&gt; (case-insensitive), or null. */
    public UMMLMod modByName(String name) {
        if (name == null) return null;
        for (UMMLMod mod : loadedMods) {
            if (mod.name().equalsIgnoreCase(name)) return mod;
        }
        return null;
    }

    @Override
    public String toString() {
        return "UMMLReport{dir=" + modsDirectory + ", loaded=" + loadedMods.size()
                + ", failed=" + failedMods.size() + ", errors=" + errors.size() + "}";
    }
}
