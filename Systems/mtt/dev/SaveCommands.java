package mtt.dev;

import java.util.List;

import mtt.player.Player;
import umml.UMMLSaveData;
import umml.UMMLSaveResult;
import umml.UMMLSaveSystem;

/**
 * Dev console commands that hook MTT up to the UMML save system.
 *
 * <p>Milk Toast Taco has 20 numbered save slots and no version folders.
 * Every command maps to {@code saves/<slot>.xml}.
 */
public final class SaveCommands {

    /** Written into new saves so it is clear who created them. */
    private static final String SAVED_BY = "MTT Community Edition";

    private SaveCommands() {}

    /** Registers all save commands on the given console. */
    public static void register(DevConsole console) {
        registerSaveSystem(console);
        registerSaves(console);
        registerSave(console);
        registerLoad(console);
        registerSaveDelete(console);
        registerSaveRename(console);
        registerSaveInfo(console);
    }

    private static void registerSaveSystem(DevConsole console) {
        console.registerCommand("savesystem", "Show the UMML save system info", "/savesystem", args -> {
            UMMLSaveSystem system = UMMLSaveSystem.find();
            List<Integer> used = system.listSlots();
            console.printHeader("=== UMML Save System ===");
            console.print("  Root: " + system.root());
            console.print("  Slots: " + used.size() + "/" + UMMLSaveSystem.MAX_SLOT + " used");
            if (used.isEmpty()) {
                console.printHighlight("  No saves yet - use /save <slot 1-20>");
            } else {
                console.printInfo("  Used: " + used);
                int free = system.nextFreeSlot();
                if (free > 0) console.printInfo("  Next free slot: " + free);
            }
        });
    }

    private static void registerSaves(DevConsole console) {
        console.registerCommand("saves", "List all 20 save slots", "/saves", args -> {
            UMMLSaveSystem system = UMMLSaveSystem.find();
            console.printHeader("=== Saves ===");
            for (int s = UMMLSaveSystem.MIN_SLOT; s <= UMMLSaveSystem.MAX_SLOT; s++) {
                if (system.slotExists(s)) {
                    UMMLSaveData d = system.load(s).data();
                    String name = d.has("playername") ? d.getString("playername", "?") : "used";
                    console.print("  Slot " + s + ": " + name);
                } else {
                    console.printHighlight("  Slot " + s + ": (empty)");
                }
            }
        });
    }

    private static void registerSave(DevConsole console) {
        console.registerCommand("save", "Save the current player to a slot", "/save <slot 1-20>", args -> {
            int slot = requireSlot(console, args, "/save <slot 1-20>");
            if (slot < 0) return;
            UMMLSaveSystem system = UMMLSaveSystem.find();
            UMMLSaveData data = PlayerSave.toSave(console.getPlayer());
            data.setSavedBy(SAVED_BY);
            UMMLSaveResult result = system.save(slot, data);
            if (result.isFailure()) {
                console.printError(result.error().toString());
                return;
            }
            console.print("Saved " + console.getPlayer().name() + " to slot " + slot
                    + " (" + system.root().resolve(slot + ".xml") + ")");
        });
    }

    private static void registerLoad(DevConsole console) {
        console.registerCommand("load", "Load a save slot into the player", "/load <slot 1-20>", args -> {
            int slot = requireSlot(console, args, "/load <slot 1-20>");
            if (slot < 0) return;
            UMMLSaveSystem system = UMMLSaveSystem.find();
            UMMLSaveResult result = system.load(slot);
            if (result.isFailure()) {
                console.printError(result.error().toString());
                return;
            }
            Player loaded = PlayerSave.fromSave(result.data());
            console.setPlayer(loaded);
            console.printHeader("Loaded slot " + slot);
            console.print("  Name: " + loaded.name());
            console.print("  Money: $" + String.format("%.2f", loaded.money()));
            console.printInfo("  Inventory (" + loaded.inventory().size() + "): "
                    + (loaded.inventory().isEmpty() ? "(empty)" : String.join(", ", loaded.inventory())));
        });
    }

    private static void registerSaveDelete(DevConsole console) {
        console.registerCommand("savedelete", "Delete a save slot", "/savedelete <slot 1-20>", args -> {
            int slot = requireSlot(console, args, "/savedelete <slot 1-20>");
            if (slot < 0) return;
            UMMLSaveSystem system = UMMLSaveSystem.find();
            UMMLSaveResult result = system.delete(slot);
            if (result.isFailure()) {
                console.printError(result.error().toString());
                return;
            }
            console.print("Deleted slot " + slot);
        });
    }

    private static void registerSaveRename(DevConsole console) {
        console.registerCommand("saverename", "Move a save to another slot", "/saverename <old 1-20> <new 1-20>", args -> {
            if (args.length < 2) {
                console.printError("Usage: /saverename <old slot 1-20> <new slot 1-20>");
                return;
            }
            int oldSlot = parseInt(args[0]);
            int newSlot = parseInt(args[1]);
            if (oldSlot < 0 || newSlot < 0) {
                console.printError("Slots must be numbers between 1 and 20");
                return;
            }
            UMMLSaveSystem system = UMMLSaveSystem.find();
            UMMLSaveResult result = system.rename(oldSlot, newSlot);
            if (result.isFailure()) {
                console.printError(result.error().toString());
                return;
            }
            console.print("Moved slot " + oldSlot + " to slot " + newSlot);
        });
    }

    private static void registerSaveInfo(DevConsole console) {
        console.registerCommand("saveinfo", "Show the contents of a save slot", "/saveinfo <slot 1-20>", args -> {
            int slot = requireSlot(console, args, "/saveinfo <slot 1-20>");
            if (slot < 0) return;
            UMMLSaveSystem system = UMMLSaveSystem.find();
            UMMLSaveResult result = system.load(slot);
            if (result.isFailure()) {
                console.printError(result.error().toString());
                return;
            }
            UMMLSaveData data = result.data();
            console.printHeader("=== Save slot " + slot + " ===");
            console.printInfo("  Saved by: " + (data.savedBy().isEmpty() ? "?" : data.savedBy()));
            console.printInfo("  Saved at: " + (data.savedAt().isEmpty() ? "?" : data.savedAt()));
            dumpData(console, data, "  ");
        });
    }

    private static void dumpData(DevConsole console, UMMLSaveData node, String indent) {
        for (String key : node.keys()) {
            console.print(indent + key + " = " + node.get(key));
        }
        for (String group : node.groupNames()) {
            console.printHeader(indent + "[" + group + "]");
            dumpData(console, node.getGroup(group), indent + "  ");
        }
    }

    private static int requireSlot(DevConsole console, String[] args, String usage) {
        if (args.length == 0) {
            console.printError("Usage: " + usage);
            return -1;
        }
        int slot = parseInt(args[0]);
        if (slot < 0) {
            console.printError("'" + args[0] + "' is not a valid save slot - use a number between 1 and 20");
            return -1;
        }
        return slot;
    }

    private static int parseInt(String s) {
        try {
            int slot = Integer.parseInt(s.trim());
            if (slot >= UMMLSaveSystem.MIN_SLOT && slot <= UMMLSaveSystem.MAX_SLOT) return slot;
            return -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
