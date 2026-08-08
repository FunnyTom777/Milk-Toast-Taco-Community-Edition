package umml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Small self test for UMML. Creates a temporary mods directory full of
 * good and broken mods, then verifies that:
 *   - valid mods load in dependency order
 *   - corrupt/incomplete mods never crash the loader and are reported
 *   - strict mode changes dependency failure into a hard failure
 *
 * Run with: java umml.UMMLSelfTest
 */
public final class UMMLSelfTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws IOException {
        boolean ok = runTests();
        System.out.println();
        System.out.println("SELF TEST: " + passed + " passed, " + failed + " failed");
        System.exit(ok ? 0 : 1);
    }

    /**
     * Runs every mod loading test, printing PASS/FAIL lines to stdout.
     * Returns true if all passed. Safe to call from the UMML dashboard,
     * which captures stdout.
     */
    public static boolean runTests() throws IOException {
        passed = 0;
        failed = 0;
        Path temp = Files.createTempDirectory("umml-selftest");
        try {
            buildFixture(temp);

            UMMLReport report = UMML.scan(temp.toString());
            UMMLReport strictReport = UMML.scan(temp.toString(), UMMLOptions.defaults().strict(true));

            check("scan did not throw", true);
            check("loaded all expected mods", report.modCount() == 6);
            check("dependency loads first (BaseLib before UsesBase)",
                    indexOf(report.loadedMods(), "BaseLib") < indexOf(report.loadedMods(), "UsesBase"));
            check("bad XML mod reported as failed",
                    failedByFolder(report, "BadXml") != null && hasType(report, "BadXml", UMMLError.Type.XML_PARSE));
            check("missing moddata folder reported as failed",
                    failedByFolder(report, "NoData") != null && hasType(report, "NoData", UMMLError.Type.MISSING_MODDATA));
            check("missing modname reported as failed",
                    failedByFolder(report, "NoName") != null && hasType(report, "NoName", UMMLError.Type.MISSING_MODNAME));
            check("unresolved dependency flagged but mod still loads (non-strict)",
                    modByName(report.loadedMods(), "MissingDep") != null
                            && hasType(report, "MissingDep", UMMLError.Type.UNRESOLVED_DEPENDENCY));
            check("missing item path flagged but mod still loads",
                    modByName(report.loadedMods(), "MissingItem") != null
                            && hasType(report, "MissingItem", UMMLError.Type.ITEM_NOT_FOUND));
            check("strict mode fails mod with unresolved dependency",
                    modByName(strictReport.loadedMods(), "MissingDep") == null
                            && failedByFolder(strictReport, "MissingDep") != null);
            check("shop_brand item without path is not an error",
                    modByName(report.loadedMods(), "BrandOnly") != null
                            && modErrors(report, "BrandOnly").isEmpty());
            check("vehicle item without path is flagged as invalid",
                    modByName(report.loadedMods(), "PathlessVehicle") != null
                            && hasType(report, "PathlessVehicle", UMMLError.Type.INVALID_ITEM));
            check("vehicle count matches fixture", report.vehicleCount() == 3);

        } finally {
            deleteTree(temp);
        }
        return failed == 0;
    }

    private static void buildFixture(Path root) throws IOException {
        // Good mod with a dependency, plus a vehicle item whose path exists.
        write(root.resolve("BaseLib/moddata.xml"),
                "<moddata><modname>BaseLib</modname><storename>Base Library</storename><modversion>1.0</modversion></moddata>\n"
                        + "<modcontents><item type=\"vehicle\" name=\"Sock Car\" path=\"vehicles/SockCar\"/></modcontents>");
        write(root.resolve("BaseLib/vehicles/SockCar/sockcarstock.xml"), "<vehicleConfig/>");

        // Depends on BaseLib.
        write(root.resolve("UsesBase/moddata.xml"),
                "<moddata><modname>UsesBase</modname><moddependencies>BaseLib</moddependencies></moddata>\n"
                        + "<modcontents/>");

        // Corrupt XML.
        write(root.resolve("BadXml/moddata.xml"), "<moddata><modname>BadXml</modname></moddata><modcontents>");

        // No moddata.xml at all.
        write(root.resolve("NoData/readme.txt"), "not a mod");

        // Missing modname.
        write(root.resolve("NoName/moddata.xml"), "<moddata><storename>No Name</storename></moddata>");

        // Depends on something that does not exist.
        write(root.resolve("MissingDep/moddata.xml"),
                "<moddata><modname>MissingDep</modname><moddependencies>NotAMod</moddependencies></moddata>");

        // Item path does not exist.
        write(root.resolve("MissingItem/moddata.xml"),
                "<moddata><modname>MissingItem</modname></moddata>\n"
                        + "<modcontents><item type=\"vehicle\" name=\"Ghost\" path=\"vehicles/Ghost\"/></modcontents>");

        // shop_brand items have no path - this is normal, not an error.
        write(root.resolve("BrandOnly/moddata.xml"),
                "<moddata><modname>BrandOnly</modname></moddata>\n"
                        + "<modcontents><item type=\"shop_brand\" name=\"Sockworks\" store_name=\"Sockworks Garage\"/></modcontents>");

        // A vehicle item without a path is incomplete and should be flagged.
        write(root.resolve("PathlessVehicle/moddata.xml"),
                "<moddata><modname>PathlessVehicle</modname></moddata>\n"
                        + "<modcontents><item type=\"vehicle\" name=\"Mystery\"/></modcontents>");
    }

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private static void check(String label, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("PASS  " + label);
        } else {
            failed++;
            System.out.println("FAIL  " + label);
        }
    }

    private static UMMLMod modByName(List<UMMLMod> mods, String name) {
        for (UMMLMod mod : mods) {
            if (mod.name().equals(name)) return mod;
        }
        return null;
    }

    private static UMMLMod failedByFolder(UMMLReport report, String folder) {
        for (UMMLMod mod : report.failedMods()) {
            if (mod.folderName().equals(folder)) return mod;
        }
        return null;
    }

    private static boolean hasType(UMMLReport report, String folder, UMMLError.Type type) {
        for (UMMLError err : report.errors()) {
            if (folder.equals(err.modFolder()) && err.type() == type) return true;
        }
        return false;
    }

    private static List<UMMLError> modErrors(UMMLReport report, String folder) {
        for (UMMLMod mod : report.loadedMods()) {
            if (mod.folderName().equals(folder)) return mod.modErrors();
        }
        for (UMMLMod mod : report.failedMods()) {
            if (mod.folderName().equals(folder)) return mod.modErrors();
        }
        return List.of();
    }

    private static int indexOf(List<UMMLMod> mods, String name) {
        for (int i = 0; i < mods.size(); i++) {
            if (mods.get(i).name().equals(name)) return i;
        }
        return Integer.MAX_VALUE;
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (var stream = Files.walk(root)) {
            stream.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
        }
    }
}
