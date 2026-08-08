package mtt.dev;

import java.util.ArrayList;
import java.util.List;

import mtt.player.Player;
import umml.UMMLSaveData;

/**
 * Converts a {@link Player} to and from {@link UMMLSaveData} so the dev
 * console can use the UMML save system.
 *
 * <pre>
 * playername (string)
 * money      (double)
 * inventory  (group)
 *   item_0, item_1, ... (string)
 * </pre>
 */
public final class PlayerSave {

    private static final String NAME_KEY = "playername";
    private static final String MONEY_KEY = "money";
    private static final String INVENTORY_GROUP = "inventory";
    private static final String ITEM_PREFIX = "item_";

    private PlayerSave() {}

    /** Writes the player into a fresh {@link UMMLSaveData}. */
    public static UMMLSaveData toSave(Player player) {
        UMMLSaveData data = new UMMLSaveData();
        data.setString(NAME_KEY, player.name());
        data.setDouble(MONEY_KEY, player.money());
        UMMLSaveData inventory = data.group(INVENTORY_GROUP);
        List<String> items = player.inventory();
        for (int i = 0; i < items.size(); i++) {
            inventory.setString(ITEM_PREFIX + i, items.get(i));
        }
        return data;
    }

    /** Restores a player from save data, falling back to defaults when absent. */
    public static Player fromSave(UMMLSaveData data) {
        String name = data.getString(NAME_KEY, "Player");
        double money = data.getDouble(MONEY_KEY, 0.0);
        List<String> items = new ArrayList<>();
        UMMLSaveData inventory = data.getGroup(INVENTORY_GROUP);
        if (inventory != null) {
            for (int i = 0; inventory.has(ITEM_PREFIX + i); i++) {
                items.add(inventory.getString(ITEM_PREFIX + i, ""));
            }
        }
        return new Player(name, money, items);
    }
}
