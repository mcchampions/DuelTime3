package cn.valorin.dueltime4.hook;

import cn.valorin.dueltime4.player.PlayerProfile;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.*;

public class HologramManager {

    private Hologram hologram;

    public boolean isEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("DecentHolograms");
    }

    public void createOrUpdate(Location loc, List<PlayerProfile> topList, int maxSize) {
        List<String> lines = new ArrayList<>();
        lines.add("§6§lLeaderboard");
        for (int i = 0; i < Math.min(topList.size(), maxSize); i++) {
            PlayerProfile p = topList.get(i);
            lines.add("§e#" + (i + 1) + " §f" + p.getPlayerName()
                + " §7- §b" + String.format("%.0f", p.getExp()) + " EXP");
        }
        if (hologram == null) {
            hologram = DHAPI.createHologram("dueltime_ranking", loc, lines);
        } else {
            DHAPI.setHologramLines(hologram, lines);
        }
    }

    public void remove() {
        if (hologram != null) { hologram.delete(); hologram = null; }
    }

    public void disable() { remove(); }
}
