package mtt.player;

import java.util.ArrayList;
import java.util.List;

public final class Player {

    private String name;
    private double money;
    private final List<String> inventory = new ArrayList<>();

    public Player(String name, double money, List<String> inventory) {
        this.name = name;
        this.money = money;
        this.inventory.addAll(inventory);
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
