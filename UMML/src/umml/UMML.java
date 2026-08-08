package umml;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * UMML - Unified MTT Mod Loader.
 *
 * The mod loader bundled with Milk Toast Taco Community Edition. It is
 * only designed to serve MTT Community Edition - not every MTT version -
 * and lives inside this repository's {@code UMML/} folder. Scans the
 * project's {@code Mods/} folder, loads every valid mod, resolves
 * dependencies, and never throws - problems are reported as
 * {@link UMMLError} entries inside a {@link UMMLReport}.
 *
 * <pre>
 * UMMLReport report = UMML.scan(UMML.modsDirectory());
 * for (UMMLMod mod : report.loadedMods()) { ... }
 * for (UMMLError err : report.errors()) { ... }
 * </pre>
 */
public final class UMML {

    /** The name of the mods folder at the root of the project. */
    public static final String MODS_DIR_NAME = "Mods";

    private UMML() {}

    /**
     * The MTT Community Edition project's mods folder - {@code Mods/} at
     * the project root. It is found the same way as the save system: walk
     * up from the current directory until a folder containing the
     * {@code Systems} source root is found. If that never happens (e.g. a
     * packaged build with no source tree), it falls back to {@code Mods/}
     * next to the current directory. The folder itself is not created by
     * the loader - mod authors drop mod folders into it.
     */
    public static Path modsDirectory() {
        Path p = Path.of("").toAbsolutePath().normalize();
        while (p != null) {
            if (Files.isDirectory(p.resolve("Systems"))) return p.resolve(MODS_DIR_NAME);
            p = p.getParent();
        }
        return Path.of(MODS_DIR_NAME).toAbsolutePath().normalize();
    }

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
