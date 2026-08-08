package mtt.dev;

import java.nio.file.Files;
import java.nio.file.Path;

import umml.UMML;
import umml.UMMLError;
import umml.UMMLItem;
import umml.UMMLMod;
import umml.UMMLReport;

/**
 * Dev console commands that hook MTT up to the UMML mod loader.
 *
 * <p>Mods live in a {@code Mods/} folder at the root of the project - one
 * subfolder per mod, each with its own moddata.xml. Every command scans
 * live, so dropping a new mod into the folder is picked up on the next
 * command. Scans never crash on a broken mod; failures are reported.
 */
public final class ModCommands {

    private ModCommands() {}

    /** Registers all mod commands on the given console. */
    public static void register(DevConsole console) {
        registerMods(console);
        registerModScan(console);
        registerModList(console);
        registerModInfo(console);
        registerModFail(console);
    }

    private static void registerMods(DevConsole console) {
        console.registerCommand("mods", "Show the UMML mod loader info", "/mods", args -> {
            Path modsDir = UMML.modsDirectory();
            UMMLReport report = scan();
            console.printHeader("=== UMML Mod Loader ===");
            console.print("  Mods folder: " + modsDir);
            console.print("  Exists: " + (Files.isDirectory(modsDir) ? "yes" : "no"));
            console.print("  Loaded: " + report.modCount());
            console.print("  Failed: " + report.failedCount());
            console.print("  Items: " + report.itemCount() + " | Vehicles: " + report.vehicleCount());
        });
    }

    private static void registerModScan(DevConsole console) {
        console.registerCommand("modscan", "Scan the Mods folder and report", "/modscan", args -> {
            Path modsDir = UMML.modsDirectory();
            console.printHeader("=== Scanning " + modsDir + " ===");
            if (!Files.isDirectory(modsDir)) {
                console.printError("Mods folder not found - create a 'Mods' folder at the project root"
                        + " with one subfolder per mod.");
                return;
            }
            UMMLReport report = scan();
            console.print("Loaded " + report.modCount() + " mod(s), " + report.failedCount()
                    + " failed, " + report.errorCount() + " error(s).");
            listLoaded(console, report);
            listFailed(console, report);
        });
    }

    private static void registerModList(DevConsole console) {
        console.registerCommand("modlist", "List loaded mods in load order", "/modlist", args -> {
            listLoaded(console, scan());
        });
    }

    private static void registerModInfo(DevConsole console) {
        console.registerCommand("modinfo", "Show details for a loaded mod", "/modinfo <name>", args -> {
            if (args.length == 0) {
                console.printError("Usage: /modinfo <name>");
                return;
            }
            String name = String.join(" ", args);
            UMMLReport report = scan();
            UMMLMod mod = report.modByName(name);
            if (mod == null) {
                console.printError("No loaded mod named '" + name + "'. Use /modlist to see loaded mods.");
                return;
            }
            console.printHeader("=== Mod: " + mod.name() + " ===");
            console.print("  Folder: " + mod.folderName());
            if (!mod.storeName().isEmpty()) console.print("  Store name: " + mod.storeName());
            if (!mod.version().isEmpty()) console.print("  Version: " + mod.version());
            if (!mod.author().isEmpty()) console.print("  Author: " + mod.author());
            if (!mod.description().isEmpty()) console.print("  Description: " + mod.description());
            console.print("  Dependencies: "
                    + (mod.dependencies().isEmpty() ? "(none)" : String.join(", ", mod.dependencies())));
            console.printInfo("  --- Contents (" + mod.items().size() + " item(s), "
                    + mod.vehicleCount() + " vehicle(s)) ---");
            if (mod.items().isEmpty()) {
                console.printHighlight("    (no items)");
            } else {
                for (UMMLItem item : mod.items()) {
                    console.print("    - " + item + (item.pathMissing() ? " [MISSING PATH]" : ""));
                }
            }
            if (!mod.modErrors().isEmpty()) {
                console.printInfo("  --- Mod problems ---");
                for (UMMLError err : mod.modErrors()) {
                    console.print("    " + err);
                }
            }
        });
    }

    private static void registerModFail(DevConsole console) {
        console.registerCommand("modfail", "List mods that failed to load", "/modfail", args -> {
            listFailed(console, scan());
        });
    }

    private static UMMLReport scan() {
        return UMML.scan(UMML.modsDirectory());
    }

    private static void listLoaded(DevConsole console, UMMLReport report) {
        console.printInfo("  --- Loaded (" + report.loadedMods().size() + ") ---");
        if (report.loadedMods().isEmpty()) {
            console.printHighlight("    (no mods loaded)");
            return;
        }
        int i = 1;
        for (UMMLMod mod : report.loadedMods()) {
            String version = mod.version().isEmpty() ? "" : " v" + mod.version();
            String author = mod.author().isEmpty() ? "" : " by " + mod.author();
            String deps = mod.dependencies().isEmpty()
                    ? "" : " (needs: " + String.join(", ", mod.dependencies()) + ")";
            console.print("  " + i++ + ". " + mod.name() + version + author
                    + " - " + mod.items().size() + " item(s), " + mod.vehicleCount() + " vehicle(s)" + deps);
        }
    }

    private static void listFailed(DevConsole console, UMMLReport report) {
        if (report.failedMods().isEmpty()) {
            return;
        }
        console.printInfo("  --- Failed (" + report.failedMods().size() + ") ---");
        for (UMMLMod mod : report.failedMods()) {
            console.print("  - " + mod.folderName());
            if (mod.modErrors().isEmpty()) {
                console.print("      mod failed to load");
            } else {
                for (UMMLError err : mod.modErrors()) {
                    console.print("      " + err);
                }
            }
        }
    }
}
