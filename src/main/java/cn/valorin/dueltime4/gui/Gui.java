package cn.valorin.dueltime4.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

public abstract class Gui implements InventoryHolder {

    public void onClick(int rawSlot, InventoryClickEvent event) {
        // Override in subclasses
    }
}
