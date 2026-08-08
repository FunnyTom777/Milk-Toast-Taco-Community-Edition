package mtt.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Player {

    private String name;
    private double money;
    private final List<String> inventory = new ArrayList<>();
    private long xp;
    private int level = 1;
    private final Map<String, Integer> stats = new LinkedHashMap<>();
    private final Set<String> licenses = new LinkedHashSet<>();

    public Player(String name, double money, List<String> inventory) {
        this.name = name;
        this.money = money;
        this.inventory.addAll(inventory);
        stats.put("Reputation", 0);
        stats.put("Energy", 100);
        stats.put("Driving", 0);
        stats.put("Repair", 0);
        stats.put("Charm", 0);
    }

    public Player(String name, double money, List<String> inventory, long xp, int level,
                  Map<String, Integer> stats, Set<String> licenses) {
        this(name, money, inventory);
        this.xp = Math.max(0, xp);
        this.level = Math.max(1, level);
        this.stats.putAll(stats);
        this.licenses.addAll(licenses);
    }

    public String name() {
        return name;
    }

    public double money() {
        return money;
    }

    public List<String> inventory() {
        return inventory;
    }

    public long xp() {
        return xp;
    }

    public int level() {
        return level;
    }

    public long xpForNextLevel() {
        return 100L * level;
    }

    public int addXp(long amount) {
        if (amount <= 0) {
            return 0;
        }
        xp += amount;
        int levelsGained = 0;
        while (xp >= xpForNextLevel()) {
            xp -= xpForNextLevel();
            level++;
            levelsGained++;
        }
        return levelsGained;
    }

    public int stat(String name) {
        return stats.getOrDefault(name, 0);
    }

    public Map<String, Integer> stats() {
        return Collections.unmodifiableMap(stats);
    }

    public void setStat(String name, int value) {
        stats.put(name, value);
    }

    public int addStat(String name, int amount) {
        int updated = Math.max(0, stat(name) + amount);
        stats.put(name, updated);
        return updated;
    }

    public Set<String> licenses() {
        return Collections.unmodifiableSet(licenses);
    }

    public boolean hasLicense(String name) {
        return licenses.contains(name);
    }

    public boolean addLicense(String name) {
        return licenses.add(name);
    }

    public boolean removeLicense(String name) {
        return licenses.remove(name);
    }

    public void rename(String name) {
        this.name = name;
    }

    public void setMoney(double amount) {
        this.money = amount;
    }

    public void addMoney(double amount) {
        this.money += amount;
    }

    public boolean removeMoney(double amount) {
        if (amount < 0 || this.money < amount) {
            return false;
        }
        this.money -= amount;
        return true;
    }

    public void addItem(String item) {
        this.inventory.add(item);
    }
}
