package umml;

import java.nio.file.Path;

/**
 * UMML - Unified MTT Mod Loader.
 *
 * The single entry point for MTT versions that want to hook into the
 * shared mod loader. Scans a mods directory (e.g. MTT_Mods), loads every
 * valid mod, resolves dependencies, and never throws - problems are
 * reported as {@link UMMLError} entries inside a {@link UMMLReport}.
 *
 * <pre>
 * UMMLReport report = UMML.scan("MTT_Mods");
 * for (UMMLMod mod : report.loadedMods()) { ... }
 * for (UMMLError err : report.errors()) { ... }
 * </pre>
 */
public final class UMML {

    private UMML() {}

    /** Scans a mods directory with default options. */
    public static UMMLReport scan(String modsDirectory) {
        return scan(Path.of(modsDirectory), UMMLOptions.defaults());
    }

    /** Scans a mods directory with default options. */
    public static UMMLReport scan(Path modsDirectory) {
        return scan(modsDirectory, UMMLOptions.defaults());
    }

    /** Scans a mods directory with custom options. */
    public static UMMLReport scan(String modsDirectory, UMMLOptions options) {
        return scan(Path.of(modsDirectory), options);
    }

    /** Scans a mods directory with custom options. */
    public static UMMLReport scan(Path modsDirectory, UMMLOptions options) {
        return ModScanner.scan(modsDirectory, options);
    }
}
