package mtt.dev;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mtt.player.Player;
import umml.UMMLSaveData;

/**
 * Converts a {@link Player} to and from {@link UMMLSaveData} so the dev
 * console can use the UMML save system.
 *
 * <pre>
 * playername (string)
 * money      (double)
 * xp         (long)
 * level      (int)
 * inventory  (group)
 *   item_0, item_1, ... (string)
 * stats      (group)
 *   &lt;stat name&gt; (int)
 * licenses   (group)
 *   license_0, license_1, ... (string)
 * </pre>
 */
public final class PlayerSave {

    private static final String NAME_KEY = "playername";
    private static final String MONEY_KEY = "money";
    private static final String XP_KEY = "xp";
    private static final String LEVEL_KEY = "level";
    private static final String INVENTORY_GROUP = "inventory";
    private static final String ITEM_PREFIX = "item_";
    private static final String STATS_GROUP = "stats";
    private static final String LICENSES_GROUP = "licenses";
    private static final String LICENSE_PREFIX = "license_";

    private PlayerSave() {}

    /** Writes the player into a fresh {@link UMMLSaveData}. */
    public static UMMLSaveData toSave(Player player) {
        UMMLSaveData data = new UMMLSaveData();
        data.setString(NAME_KEY, player.name());
        data.setDouble(MONEY_KEY, player.money());
        data.setLong(XP_KEY, player.xp());
        data.setInt(LEVEL_KEY, player.level());
        UMMLSaveData inventory = data.group(INVENTORY_GROUP);
        List<String> items = player.inventory();
        for (int i = 0; i < items.size(); i++) {
            inventory.setString(ITEM_PREFIX + i, items.get(i));
        }
        UMMLSaveData stats = data.group(STATS_GROUP);
        for (Map.Entry<String, Integer> e : player.stats().entrySet()) {
            stats.setInt(e.getKey(), e.getValue());
        }
        UMMLSaveData licenses = data.group(LICENSES_GROUP);
        int i = 0;
        for (String license : player.licenses()) {
            licenses.setString(LICENSE_PREFIX + i, license);
            i++;
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
        long xp = data.getLong(XP_KEY, 0);
        int level = data.getInt(LEVEL_KEY, 1);
        Map<String, Integer> stats = new LinkedHashMap<>();
        UMMLSaveData statsGroup = data.getGroup(STATS_GROUP);
        if (statsGroup != null) {
            for (String key : statsGroup.keys()) {
                stats.put(key, statsGroup.getInt(key, 0));
            }
        }
        Set<String> licenses = new LinkedHashSet<>();
        UMMLSaveData licenseGroup = data.getGroup(LICENSES_GROUP);
        if (licenseGroup != null) {
            for (int i = 0; licenseGroup.has(LICENSE_PREFIX + i); i++) {
                licenses.add(licenseGroup.getString(LICENSE_PREFIX + i, ""));
            }
        }
        return new Player(name, money, items, xp, level, stats, licenses);
    }
}
