package cn.valorin.dueltime4.gui;

import cn.valorin.dueltime4.DuelTimePlugin;
import cn.valorin.dueltime4.service.ShopService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

import java.util.*;

public class ShopGui extends PagedGui {

    private final ShopService shopService;
    private final List<Map<?, ?>> items;

    public ShopGui(Player player) {
        super(player, "Points Shop");
        this.shopService = DuelTimePlugin.getInstance().getShopService();
        this.items = shopService.getItems();
    }

    @Override
    protected List<ItemStack> getPageItems(int page) {
        List<ItemStack> result = new ArrayList<>();
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, items.size());
        for (int i = start; i < end; i++) {
            Map<?, ?> item = items.get(i);
            Material mat = Material.valueOf((String) item.get("material"));
            ItemStack stack = new ItemStack(mat);
            ItemMeta meta = stack.getItemMeta();
            meta.displayName(Component.text((String) item.get("name")));
            List<String> lore = new ArrayList<>((List<String>) item.get("lore"));
            lore.add("§7Cost: §e" + item.get("cost") + " points");
            lore.add("§aClick to buy!");
            meta.lore(lore.stream().map(Component::text).toList());
            stack.setItemMeta(meta);
            result.add(stack);
        }
        return result;
    }

    @Override
    protected int totalPages() {
        return (int) Math.ceil((double) items.size() / PAGE_SIZE);
    }

    @Override
    public void onClick(int slot, InventoryClickEvent event) {
        super.onClick(slot, event);
        if (slot == 45 || slot == 53) return; // Navigation slots
        int index = page * PAGE_SIZE + slot;
        if (index >= 0 && index < items.size()) {
            String itemId = (String) items.get(index).get("id");
            boolean success = shopService.buy(player, itemId);
            if (success) player.sendMessage("§aPurchased!");
            else player.sendMessage("§cNot enough points!");
            player.closeInventory();
        }
    }
}
