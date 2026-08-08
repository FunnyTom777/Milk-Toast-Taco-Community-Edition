package umml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Command line front end for UMML. Lets you scan a mods directory from a
 * terminal without writing any code:
 *
 *   java umml.UMMLMain [mods_directory] [--strict] [--verbose]
 *
 * If no directory is given, UMML looks in MTT_Mods, ../MTT_Mods and
 * ../../MTT_Mods. UMML is bundled with MTT Community Edition and serves
 * that project only.
 */
public final class UMMLMain {

    private UMMLMain() {}

    public static void main(String[] args) {
        String dir = null;
        boolean strict = false;
        boolean verbose = false;

        for (String arg : args) {
            if (arg.equals("--strict")) {
                strict = true;
            } else if (arg.equals("--verbose") || arg.equals("-v")) {
                verbose = true;
            } else if (arg.equals("--help") || arg.equals("-h")) {
                printUsage();
                return;
            } else {
                dir = arg;
            }
        }

        if (dir == null) {
            dir = findModsDirectory();
            if (dir == null) {
                System.err.println("Could not find MTT_Mods. Pass the directory as an argument.");
                printUsage();
                System.exit(1);
            }
        }

        UMMLOptions options = UMMLOptions.defaults().strict(strict);
        UMMLReport report = UMML.scan(dir, options);
        printReport(report, verbose);

        System.exit(report.failedCount() == 0 ? 0 : 1);
    }

    private static void printReport(UMMLReport report, boolean verbose) {
        System.out.println();
        System.out.println("=== UMML - Unified MTT Mod Loader ===");
        System.out.println("Directory : " + report.modsDirectory());
        System.out.println("Mods loaded: " + report.modCount());
        System.out.println("Mods failed : " + report.failedCount());
        System.out.println("Vehicles   : " + report.vehicleCount());
        System.out.println("Items      : " + report.itemCount());
        System.out.println("Errors     : " + report.errorCount());
        System.out.println();

        if (!report.loadedMods().isEmpty()) {
            System.out.println("Loaded mods (load order):");
            int i = 1;
            for (UMMLMod mod : report.loadedMods()) {
                System.out.printf("  %2d. %s v%s by %s (%d items, %d vehicles)%n",
                        i++, mod.name(), mod.version(), mod.author(),
                        mod.items().size(), mod.vehicleCount());
                if (!mod.dependencies().isEmpty()) {
                    System.out.println("        depends on: " + mod.dependencies());
                }
                if (verbose && !mod.modErrors().isEmpty()) {
                    for (UMMLError err : mod.modErrors()) {
                        System.out.println("        " + err);
                    }
                }
            }
        }

        if (!report.failedMods().isEmpty()) {
            System.out.println();
            System.out.println("Failed mods:");
            for (UMMLMod mod : report.failedMods()) {
                System.out.println("  - " + mod.folderName());
                for (UMMLError err : mod.modErrors()) {
                    System.out.println("      " + err);
                }
            }
        }

        if (verbose && report.errorCount() > 0) {
            System.out.println();
            System.out.println("All errors:");
            for (UMMLError err : report.errors()) {
                System.out.println("  " + err);
            }
        }
        System.out.println();
    }

    private static String findModsDirectory() {
        String[] candidates = {"MTT_Mods", "../MTT_Mods", "../../MTT_Mods"};
        for (String candidate : candidates) {
            Path p = Path.of(candidate);
            if (Files.isDirectory(p)) return p.toAbsolutePath().normalize().toString();
        }
        return null;
    }

    private static void printUsage() {
        System.out.println("Usage: java umml.UMMLMain [mods_directory] [--strict] [--verbose]");
        System.out.println("  Scans a mods directory with UMML and prints a report.");
    }
}
