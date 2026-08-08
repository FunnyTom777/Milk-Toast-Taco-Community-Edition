package umml;

/** Options that change how UMML behaves during a scan. */
public class UMMLOptions {

    private boolean strict = false;
    private boolean reportFoldersWithoutModdata = true;
    private boolean resolveItemPaths = true;
    private boolean warnOnMultipleModdataSections = true;

    private UMMLOptions() {}

    /** Creates an options object with the default behaviour. */
    public static UMMLOptions defaults() {
        return new UMMLOptions();
    }

    /**
     * Whether problems should be treated as fatal. When true, mods with
     * unresolved dependencies fail to load. When false (default) such mods
     * still load but are flagged with an error.
     */
    public boolean strict() {
        return strict;
    }

    public UMMLOptions strict(boolean value) {
        this.strict = value;
        return this;
    }

    /**
     * Whether a subfolder that has no moddata.xml is reported as a failed
     * mod. Default true.
     */
    public boolean reportFoldersWithoutModdata() {
        return reportFoldersWithoutModdata;
    }

    public UMMLOptions reportFoldersWithoutModdata(boolean value) {
        this.reportFoldersWithoutModdata = value;
        return this;
    }

    /**
     * Whether each mod item's path is resolved against the mod root folder.
     * Default true.
     */
    public boolean resolveItemPaths() {
        return resolveItemPaths;
    }

    public UMMLOptions resolveItemPaths(boolean value) {
        this.resolveItemPaths = value;
        return this;
    }

    /**
     * Whether more than one &lt;moddata&gt; section in a moddata.xml file
     * produces a warning. Default true.
     */
    public boolean warnOnMultipleModdataSections() {
        return warnOnMultipleModdataSections;
    }

    public UMMLOptions warnOnMultipleModdataSections(boolean value) {
        this.warnOnMultipleModdataSections = value;
        return this;
    }
}
