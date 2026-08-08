package umml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Self test for the {@link UMMLSaveSystem}.
 *
 * Creates a temporary save root and verifies that:
 *   - versions and slots round-trip through XML (typed values + groups)
 *   - saves from different MTT Community Edition versions live in separate folders
 *   - load/delete/rename report clean failures instead of throwing
 *   - a corrupt save file produces a {@link UMMLError}, never a crash
 *
 * Run with: java umml.UMMLSaveSystemTest
 */
public final class UMMLSaveSystemTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws IOException {
        boolean ok = runTests();
        System.out.println();
        System.out.println("SAVE SYSTEM TEST: " + passed + " passed, " + failed + " failed");
        System.exit(ok ? 0 : 1);
    }

    /** Runs every test, printing PASS/FAIL lines. Returns true if all passed. */
    public static boolean runTests() throws IOException {
        passed = 0;
        failed = 0;
        Path temp = Files.createTempDirectory("umml-savetest");
        try {
            UMMLSaveSystem saves = UMMLSaveSystem.open(temp);

            check("no version folders before ensure", !saves.versionExists("MTTV40"));
            check("ensureRoot succeeds", saves.ensureRoot().isSuccess());
            check("root exists after ensure", saves.exists());

            // --- round trip: typed values and nested groups ---
            UMMLSaveData data = new UMMLSaveData();
            data.setSavedBy("MTTV41");
            data.setString("playername", "Bobby");
            data.setInt("money", 5000);
            data.setLong("totalkm", 123456789L);
            data.setDouble("health", 87.5);
            data.setFloat("stress", 0.25f);
            data.setBoolean("hasLicense", true);
            data.group("inventory").setString("item_0", "Wrench");
            data.group("inventory").setString("item_1", "Hammer");
            data.group("vehicles").setInt("owned", 2);

            check("save succeeds", saves.save("MTTV40", "Slot1", data).isSuccess());
            check("save file exists on disk",
                    Files.isRegularFile(temp.resolve("MTTV40/Slot1.xml")));
            check("slot listed", saves.listSaves("MTTV40").equals(List.of("Slot1")));

            UMMLSaveResult loaded = saves.load("MTTV40", "Slot1");
            check("load succeeds", loaded.isSuccess());
            if (loaded.isSuccess()) {
                UMMLSaveData d = loaded.data();
                check("savedBy round trips", d.savedBy().equals("MTTV41"));
                check("savedAt auto-stamped", !d.savedAt().isEmpty());
                check("string round trips", d.getString("playername", "").equals("Bobby"));
                check("int round trips", d.getInt("money", 0) == 5000);
                check("long round trips", d.getLong("totalkm", 0) == 123456789L);
                check("double round trips", d.getDouble("health", 0) == 87.5);
                check("float round trips", d.getFloat("stress", 0) == 0.25f);
                check("boolean round trips", d.getBoolean("hasLicense", false));
                check("group values round trip",
                        d.getGroup("inventory").getString("item_0", "").equals("Wrench")
                                && d.getGroup("inventory").getString("item_1", "").equals("Hammer"));
                check("second group round trips", d.getGroup("vehicles").getInt("owned", -1) == 2);
            }

            // --- versions stay separate ---
            UMMLSaveData other = new UMMLSaveData();
            other.setString("playername", "Coop");
            check("save to second version", saves.save("MTTV41", "Slot1", other).isSuccess());
            check("two versions exist", saves.listVersions().equals(List.of("MTTV40", "MTTV41")));
            check("versions do not share files",
                    saves.listSaves("MTTV40").equals(List.of("Slot1"))
                            && saves.listSaves("MTTV41").equals(List.of("Slot1")));
            check("MTTV41 save has its own data",
                    saves.load("MTTV41", "Slot1").data().getString("playername", "").equals("Coop"));

            // --- failures never throw ---
            check("load missing slot fails cleanly", saves.load("MTTV40", "Nope").isFailure());
            check("delete missing slot fails cleanly", saves.delete("MTTV40", "Nope").isFailure());
            check("load missing version fails cleanly", saves.load("MTTV99", "Slot1").isFailure());
            check("invalid version name rejected",
                    saves.ensureVersion("../evil").isFailure() || !saves.listVersions().contains("evil"));
            check("invalid slot name rejected", saves.save("MTTV40", "../evil", new UMMLSaveData()).isFailure());

            // --- rename ---
            check("rename succeeds", saves.rename("MTTV40", "Slot1", "Slot2").isSuccess());
            check("old slot gone after rename", saves.listSaves("MTTV40").equals(List.of("Slot2")));
            check("rename to existing slot rejected",
                    saves.rename("MTTV40", "Slot2", "Slot2").isFailure());

            // --- corrupt file reported, not crashed ---
            Files.writeString(temp.resolve("MTTV40/Broken.xml"), "<save><entry", StandardCharsets.UTF_8);
            UMMLSaveResult broken = saves.load("MTTV40", "Broken");
            check("corrupt save fails cleanly", broken.isFailure());
            check("corrupt save reports XML_PARSE",
                    broken.error() != null && broken.error().type() == UMMLError.Type.XML_PARSE);

            // --- delete ---
            check("delete succeeds", saves.delete("MTTV40", "Slot2").isSuccess());
            check("slot removed after delete",
                    saves.listSaves("MTTV40").equals(List.of("Broken")));

        } finally {
            deleteTree(temp);
        }
        return failed == 0;
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

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (var stream = Files.walk(root)) {
            stream.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
        }
    }
}
