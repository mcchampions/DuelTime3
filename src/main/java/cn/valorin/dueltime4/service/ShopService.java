package cn.valorin.dueltime4.service;

import cn.valorin.dueltime4.config.Config;
import cn.valorin.dueltime4.player.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class ShopService {

    private final PlayerService playerService;
    private final Config config;

    public ShopService(PlayerService playerService, Config config) {
        this.playerService = playerService;
        this.config = config;
    }

    public List<Map<?, ?>> getItems() { return config.getMapList("shop.items"); }

    public Map<?, ?> findItem(String id) {
        return getItems().stream().filter(item -> id.equals(item.get("id"))).findFirst().orElse(null);
    }

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

        Object commandsObj = item.get("commands");
        if (commandsObj instanceof List<?> list) {
            for (Object obj : list) {
                if (obj instanceof String cmd) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
                }
            }
        }
        return true;
    }
}
