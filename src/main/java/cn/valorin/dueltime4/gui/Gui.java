package cn.valorin.dueltime4.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.text.Component;

public abstract class Gui implements InventoryHolder {

    protected Player player;
    protected Inventory inventory;

    public Gui(Player player, String title, int rows) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, rows * 9, Component.text(title));
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void onClick(int rawSlot, InventoryClickEvent event) {
        // Override in subclasses
    }
}
