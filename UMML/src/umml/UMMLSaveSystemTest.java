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
 *   - typed values and nested groups round-trip through XML
 *   - the 1-20 slot system is enforced
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

            check("no slots before any save", saves.listSlots().isEmpty());
            check("ensureRoot succeeds", saves.ensureRoot().isSuccess());
            check("root exists after ensure", saves.exists());
            check("first free slot is 1", saves.nextFreeSlot() == 1);
            check("20 slots available", UMMLSaveSystem.MAX_SLOT - UMMLSaveSystem.MIN_SLOT + 1 == 20);

            // --- round trip: typed values and nested groups ---
            UMMLSaveData data = new UMMLSaveData();
            data.setSavedBy("MTT Community Edition");
            data.setString("playername", "Bobby");
            data.setInt("money", 5000);
            data.setLong("totalkm", 123456789L);
            data.setDouble("health", 87.5);
            data.setFloat("stress", 0.25f);
            data.setBoolean("hasLicense", true);
            data.group("inventory").setString("item_0", "Wrench");
            data.group("inventory").setString("item_1", "Hammer");
            data.group("vehicles").setInt("owned", 2);

            check("save to slot 1 succeeds", saves.save(1, data).isSuccess());
            check("save file exists on disk", Files.isRegularFile(temp.resolve("1.xml")));
            check("slot 1 listed", saves.listSlots().equals(List.of(1)));
            check("slot 1 exists", saves.slotExists(1));
            check("slot 2 not used", !saves.slotExists(2));
            check("next free slot is 2", saves.nextFreeSlot() == 2);

            UMMLSaveResult loaded = saves.load(1);
            check("load succeeds", loaded.isSuccess());
            if (loaded.isSuccess()) {
                UMMLSaveData d = loaded.data();
                check("savedBy round trips", d.savedBy().equals("MTT Community Edition"));
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

            // --- slots stay separate ---
            UMMLSaveData other = new UMMLSaveData();
            other.setString("playername", "Coop");
            check("save to slot 2 succeeds", saves.save(2, other).isSuccess());
            check("two slots in use", saves.listSlots().equals(List.of(1, 2)));
            check("slot 1 keeps its own data",
                    saves.load(1).data().getString("playername", "").equals("Bobby"));
            check("slot 2 has its own data",
                    saves.load(2).data().getString("playername", "").equals("Coop"));

            // --- failures never throw ---
            check("load empty slot fails cleanly", saves.load(3).isFailure());
            check("delete empty slot fails cleanly", saves.delete(3).isFailure());
            check("invalid slot 0 rejected", saves.save(0, new UMMLSaveData()).isFailure());
            check("invalid slot 21 rejected", saves.save(21, new UMMLSaveData()).isFailure());
            check("invalid slot -5 rejected", saves.load(-5).isFailure());

            // --- rename ---
            check("rename slot 1 to 3 succeeds", saves.rename(1, 3).isSuccess());
            check("old slot gone after rename", saves.listSlots().equals(List.of(2, 3)));
            check("rename onto existing slot rejected", saves.rename(2, 3).isFailure());
            check("rename to invalid slot rejected", saves.rename(3, 21).isFailure());

            // --- fill all 20 slots ---
            for (int s = 1; s <= 20; s++) {
                check("fill slot " + s, saves.save(s, new UMMLSaveData()).isSuccess());
            }
            check("all 20 slots full", saves.nextFreeSlot() == 0);
            check("20 slots listed", saves.listSlots().size() == 20);

            // --- corrupt file reported, not crashed ---
            Files.writeString(temp.resolve("3.xml"), "<save><entry", StandardCharsets.UTF_8);
            UMMLSaveResult broken = saves.load(3);
            check("corrupt save fails cleanly", broken.isFailure());
            check("corrupt save reports XML_PARSE",
                    broken.error() != null && broken.error().type() == UMMLError.Type.XML_PARSE);

            // --- delete ---
            check("delete succeeds", saves.delete(3).isSuccess());
            check("slot removed after delete", saves.listSlots().size() == 19 && !saves.listSlots().contains(3));
            check("next free slot is 3 again", saves.nextFreeSlot() == 3);

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
