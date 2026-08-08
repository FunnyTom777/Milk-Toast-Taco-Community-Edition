package umml;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The contents of one MTT save file.
 *
 * <p>Deliberately dynamic: UMML does not need to know what the game stores
 * in its save. MTT Community Edition can write any number of typed
 * key/value entries plus nested {@link #group(String) groups}, and the XML
 * format is stable across MTT Community Edition versions, so a save written
 * by one build can be read by the next (and vice versa).
 *
 * <p>Values must be one of {@link String}, {@link Integer}, {@link Long},
 * {@link Double}, {@link Float}, {@link Boolean} or {@code null}. A null
 * value is written as an empty string and read back as an empty string.
 *
 * <pre>
 * UMMLSaveData data = new UMMLSaveData();
 * data.setString("playername", "Bobby");
 * data.setInt("money", 5000);
 * data.group("inventory").setString("item_0", "Wrench");
 * </pre>
 */
public class UMMLSaveData {

    private final Map<String, Object> values = new LinkedHashMap<>();
    private final Map<String, UMMLSaveData> groups = new LinkedHashMap<>();

    private String savedBy = "";
    private String savedAt = "";

    // ========================================================================
    // Header (written into the XML as attributes of the <save> root)
    // ========================================================================

    /** Which MTT version (or tool) wrote this save, e.g. "MTTV41". */
    public String savedBy() {
        return savedBy;
    }

    public void setSavedBy(String savedBy) {
        this.savedBy = savedBy == null ? "" : savedBy.trim();
    }

    /** When the save was written, as an ISO date-time string. */
    public String savedAt() {
        return savedAt;
    }

    public void setSavedAt(String savedAt) {
        this.savedAt = savedAt == null ? "" : savedAt.trim();
    }

    // ========================================================================
    // Values
    // ========================================================================

    /**
     * Stores a value. Supported types are String, Integer, Long, Double,
     * Float and Boolean. Anything else throws an IllegalArgumentException.
     *
     * @return this save data, so calls can be chained
     */
    public UMMLSaveData set(String key, Object value) {
        checkKey(key);
        if (value == null
                || value instanceof String
                || value instanceof Integer
                || value instanceof Long
                || value instanceof Double
                || value instanceof Float
                || value instanceof Boolean) {
            values.put(key, value);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported save value type for key '" + key + "': " + value.getClass().getName());
        }
        return this;
    }

    public UMMLSaveData setString(String key, String value) {
        return set(key, value);
    }

    public UMMLSaveData setInt(String key, int value) {
        return set(key, value);
    }

    public UMMLSaveData setLong(String key, long value) {
        return set(key, value);
    }

    public UMMLSaveData setDouble(String key, double value) {
        return set(key, value);
    }

    public UMMLSaveData setFloat(String key, float value) {
        return set(key, value);
    }

    public UMMLSaveData setBoolean(String key, boolean value) {
        return set(key, value);
    }

    /** Returns the raw stored value, or null if the key is absent. */
    public Object get(String key) {
        return values.get(key);
    }

    public boolean has(String key) {
        return values.containsKey(key);
    }

    public String getString(String key, String fallback) {
        Object v = values.get(key);
        if (v == null) return fallback;
        return String.valueOf(v);
    }

    public int getInt(String key, int fallback) {
        Object v = values.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        if (v instanceof String) {
            try {
                return Integer.parseInt((String) v);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    public long getLong(String key, long fallback) {
        Object v = values.get(key);
        if (v instanceof Number) return ((Number) v).longValue();
        if (v instanceof String) {
            try {
                return Long.parseLong((String) v);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    public double getDouble(String key, double fallback) {
        Object v = values.get(key);
        if (v instanceof Number) return ((Number) v).doubleValue();
        if (v instanceof String) {
            try {
                return Double.parseDouble((String) v);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    public float getFloat(String key, float fallback) {
        Object v = values.get(key);
        if (v instanceof Number) return ((Number) v).floatValue();
        if (v instanceof String) {
            try {
                return Float.parseFloat((String) v);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    public boolean getBoolean(String key, boolean fallback) {
        Object v = values.get(key);
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof String) {
            String s = ((String) v).trim();
            if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("1")) return true;
            if (s.equalsIgnoreCase("false") || s.equalsIgnoreCase("0")) return false;
        }
        return fallback;
    }

    public void remove(String key) {
        values.remove(key);
    }

    /** All stored keys, in insertion order. */
    public Set<String> keys() {
        return Collections.unmodifiableSet(values.keySet());
    }

    /** Unmodifiable view of the stored values. */
    public Map<String, Object> values() {
        return Collections.unmodifiableMap(values);
    }

    public int entryCount() {
        return values.size();
    }

    public boolean isEmpty() {
        return values.isEmpty() && groups.isEmpty();
    }

    public void clear() {
        values.clear();
        groups.clear();
    }

    // ========================================================================
    // Groups (nested sections inside the save)
    // ========================================================================

    /**
     * Returns the named nested group, creating it if it does not exist yet.
     * Groups let a game keep related data together (inventory, vehicles,
     * properties, ...) without UMML knowing what they mean.
     */
    public UMMLSaveData group(String name) {
        checkKey(name);
        return groups.computeIfAbsent(name, k -> new UMMLSaveData());
    }

    /** Returns the named group, or null if it does not exist. */
    public UMMLSaveData getGroup(String name) {
        return groups.get(name);
    }

    public boolean hasGroup(String name) {
        return groups.containsKey(name);
    }

    /** Names of all nested groups, in insertion order. */
    public Set<String> groupNames() {
        return Collections.unmodifiableSet(groups.keySet());
    }

    private static void checkKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Save entry key must not be empty");
        }
    }

    @Override
    public String toString() {
        return "UMMLSaveData{" + entryCount() + " entries, " + groups.size() + " groups"
                + (savedBy.isEmpty() ? "" : ", savedBy=" + savedBy) + "}";
    }
}
