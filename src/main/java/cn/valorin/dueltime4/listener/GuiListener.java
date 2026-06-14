package cn.valorin.dueltime4.listener;

import cn.valorin.dueltime4.gui.Gui;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GuiListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() instanceof Gui gui) {
            e.setCancelled(true);
            gui.onClick(e.getRawSlot(), e);
        }
    }
}
