package cn.valorin.dueltime4.service;

import cn.valorin.dueltime4.config.Config;
import cn.valorin.dueltime4.player.PlayerProfile;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ShopService {

    private final PlayerService playerService;
    private final Config config;
    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    public ShopService(PlayerService playerService, Config config) {
        this.playerService = playerService;
        this.config = config;
    }

    public List<Map<?, ?>> getItems() { return config.getMapList("shop.items"); }

    public Map<?, ?> findItem(String id) {
        return getItems().stream().filter(item -> id.equals(item.get("id"))).findFirst().orElse(null);
    }

    @SuppressWarnings("unchecked")
    public boolean buy(Player player, String itemId) {
        Map<?, ?> item = findItem(itemId);
        if (item == null) return false;

        Object costObj = item.get("cost");
        if (!(costObj instanceof Number)) return false;
        int cost = ((Number) costObj).intValue();

        PlayerProfile profile = playerService.getOrCreate(player.getName());
        if (profile.getPoint() < cost) return false;

        profile.setPoint(profile.getPoint() - cost);
        playerService.save(profile);

        // Build ItemStack from plain fields
        Material mat = Material.valueOf((String) item.get("material"));
        int amount = 1;
        if (item.get("amount") instanceof Number n) amount = n.intValue();
        ItemStack stack = new ItemStack(mat, amount);
        ItemMeta meta = stack.getItemMeta();
        if (item.get("name") instanceof String s) meta.displayName(SERIALIZER.deserialize(s));
        if (item.get("lore") instanceof List<?> list && !list.isEmpty()) {
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            for (Object o : list) {
                if (o instanceof String s) lore.add(SERIALIZER.deserialize(s));
            }
            meta.lore(lore);
        }
        stack.setItemMeta(meta);

        // Give item, drop overflow
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }

        // Run commands
        Object commandsObj = item.get("commands");
        if (commandsObj instanceof List<?> list) {
            for (Object obj : list) {
                if (obj instanceof String cmd && !cmd.isEmpty()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
                }
            }
        }
        return true;
    }
}
