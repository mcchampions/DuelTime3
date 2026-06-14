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

    @SuppressWarnings("unchecked")
    public boolean buy(Player player, String itemId) {
        Map<?, ?> item = findItem(itemId);
        if (item == null) return false;

        int cost = ((Number) item.get("cost")).intValue();
        PlayerProfile profile = playerService.getOrCreate(player.getName());
        if (profile.getPoint() < cost) return false;

        profile.setPoint(profile.getPoint() - cost);
        playerService.save(profile);

        List<String> commands = (List<String>) item.get("commands");
        if (commands != null) {
            for (String cmd : commands) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
            }
        }
        return true;
    }
}
