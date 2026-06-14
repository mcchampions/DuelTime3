package cn.valorin.dueltime4.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

import java.util.List;

public abstract class PagedGui extends Gui {

    protected int page = 0;
    protected static final int PAGE_SIZE = 45;

    public PagedGui(Player player, String title) {
        super(player, title, 6);
    }

    protected abstract List<ItemStack> getPageItems(int page);
    protected abstract int totalPages();

    public void render() {
        inventory.clear();
        List<ItemStack> items = getPageItems(page);
        for (int i = 0; i < Math.min(items.size(), PAGE_SIZE); i++) {
            inventory.setItem(i, items.get(i));
        }
        if (page > 0) inventory.setItem(45, navItem(Material.ARROW, "§ePrevious"));
        if (page < totalPages() - 1) inventory.setItem(53, navItem(Material.ARROW, "§eNext"));
    }

    private ItemStack navItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void onClick(int slot, InventoryClickEvent event) {
        if (slot == 45 && page > 0) { page--; render(); return; }
        if (slot == 53 && page < totalPages() - 1) { page++; render(); return; }
    }

    @Override
    public void open() { render(); super.open(); }
}
