package cn.valorin.dueltime4.gui;

import cn.valorin.dueltime4.DuelTimePlugin;
import cn.valorin.dueltime4.service.ShopService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.*;

public class ShopGui extends PagedGui {

    private final ShopService shopService;
    private final List<Map<?, ?>> items;
    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    public ShopGui(Player player) {
        super(player, "Points Shop");
        this.shopService = DuelTimePlugin.getInstance().getShopService();
        this.items = shopService.getItems();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected List<ItemStack> getPageItems(int page) {
        List<ItemStack> result = new ArrayList<>();
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, items.size());
        for (int i = start; i < end; i++) {
            Map<?, ?> shopItem = items.get(i);
            Material mat = Material.valueOf((String) shopItem.get("material"));
            int amount = 1;
            Object amtObj = shopItem.get("amount");
            if (amtObj instanceof Number n) amount = n.intValue();
            ItemStack stack = new ItemStack(mat, amount);
            ItemMeta meta = stack.getItemMeta();
            Object nameObj = shopItem.get("name");
            if (nameObj instanceof String s) meta.displayName(SERIALIZER.deserialize(s));
            Object loreObj = shopItem.get("lore");
            if (loreObj instanceof List<?> list && !list.isEmpty()) {
                List<Component> lore = new ArrayList<>();
                for (Object o : list) {
                    if (o instanceof String s) lore.add(SERIALIZER.deserialize(s));
                }
                lore.add(Component.text("§7Cost: §e" + shopItem.get("cost") + " points"));
                lore.add(Component.text("§aClick to buy!"));
                meta.lore(lore);
            } else {
                meta.lore(List.of(
                    Component.text("§7Cost: §e" + shopItem.get("cost") + " points"),
                    Component.text("§aClick to buy!")
                ));
            }
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
        if (slot < 0 || slot >= PAGE_SIZE) return;
        int index = page * PAGE_SIZE + slot;
        if (index < items.size()) {
            String itemId = (String) items.get(index).get("id");
            boolean success = shopService.buy(player, itemId);
            if (success) player.sendMessage("§aPurchased!");
            else player.sendMessage("§cNot enough points or inventory full!");
            player.closeInventory();
        }
    }
}
