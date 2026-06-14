package cn.valorin.dueltime4.command.impl;

import cn.valorin.dueltime4.DuelTimePlugin;
import cn.valorin.dueltime4.command.SubCommand;
import cn.valorin.dueltime4.config.Messages;
import cn.valorin.dueltime4.service.ShopService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;

public class CmdShop extends SubCommand {

    public CmdShop() {
        super("shop", new String[]{"shop", "store"}, null, "/dt shop", true);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        ShopService shopService = DuelTimePlugin.getInstance().getShopService();
        Messages messages = DuelTimePlugin.getInstance().getMsg();

        List<Map<?, ?>> items = shopService.getItems();
        if (items.isEmpty()) {
            player.sendMessage("Shop is empty.");
            return;
        }

        String title = messages.get("shop.title") != null ? messages.get("shop.title") : "DuelTime Shop";
        int size = ((items.size() - 1) / 9 + 1) * 9;
        if (size < 9) size = 9;
        if (size > 54) size = 54;
        Inventory inv = Bukkit.createInventory(null, size, title);

        for (Map<?, ?> item : items) {
            String id = (String) item.get("id");
            Object matRaw = item.get("material");
            String matName = matRaw instanceof String s ? s : "STONE";
            Material material = Material.getMaterial(matName.toUpperCase());
            if (material == null) material = Material.STONE;

            Object nameRaw = item.get("name");
            String displayName = nameRaw instanceof String s ? s : (id != null ? id : "Item");
            int cost = item.get("cost") instanceof Number n ? n.intValue() : 0;
            int amount = item.get("amount") instanceof Number n ? n.intValue() : 1;

            ItemStack stack = new ItemStack(material, amount);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§e" + displayName);
                meta.setLore(List.of("§7Cost: §6" + cost + " points"));
                stack.setItemMeta(meta);
            }
            inv.addItem(stack);
        }

        player.openInventory(inv);
    }
}
