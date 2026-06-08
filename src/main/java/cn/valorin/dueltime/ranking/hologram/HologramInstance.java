package cn.valorin.dueltime.ranking.hologram;

import cn.valorin.dueltime.DuelTimePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class HologramInstance {
    private static final Pattern PATTERN = Pattern.compile("dueltime:", Pattern.LITERAL);
    private Object hologram;
    private final HologramPluginType hologramPluginType;

    public HologramInstance(HologramPluginType hologramPluginType, Location location, String rankingId, List<String> content, Material material) {
        this.hologramPluginType = hologramPluginType;
        if (hologramPluginType == HologramPluginType.CMI) {
            com.Zrips.CMI.Modules.Holograms.CMIHologram hologram = new com.Zrips.CMI.Modules.Holograms.CMIHologram(rankingId, location);
            hologram.setLoc(location);
            hologram.setLines(content);
            hologram.refresh();
            this.hologram = hologram;
        } else if (hologramPluginType == HologramPluginType.DECENT_HOLOGRAM) {
            //这个插件的全息图id不允许包括冒号
            eu.decentsoftware.holograms.api.holograms.Hologram hologram = eu.decentsoftware.holograms.api.DHAPI.createHologram(PATTERN.matcher(rankingId).replaceAll(""), location, null);
            if (material != null) {
                eu.decentsoftware.holograms.api.DHAPI.addHologramLine(hologram, material);
            }
            content.forEach(line -> eu.decentsoftware.holograms.api.DHAPI.addHologramLine(hologram, line));
            this.hologram = hologram;
        }
    }

    public void destroy() {
        switch (hologramPluginType) {
            case CMI:
                ((com.Zrips.CMI.Modules.Holograms.CMIHologram) hologram).remove();
                break;
            case DECENT_HOLOGRAM:
                ((eu.decentsoftware.holograms.api.holograms.Hologram) hologram).delete();
        }
    }

    public void refresh(List<String> content, Material material) {
        switch (hologramPluginType) {
            case CMI:
                ((com.Zrips.CMI.Modules.Holograms.CMIHologram) hologram).setLines(content);
                ((com.Zrips.CMI.Modules.Holograms.CMIHologram) hologram).refresh();
                break;
            case DECENT_HOLOGRAM:
                Bukkit.getScheduler().runTask(DuelTimePlugin.getInstance(), () -> {
                    eu.decentsoftware.holograms.api.holograms.Hologram hologram = (eu.decentsoftware.holograms.api.holograms.Hologram) this.hologram;
                    eu.decentsoftware.holograms.api.DHAPI.setHologramLines(hologram, new ArrayList<>());
                    if (material != null) {
                        eu.decentsoftware.holograms.api.DHAPI.addHologramLine(hologram, material);
                    }
                    content.forEach(line -> eu.decentsoftware.holograms.api.DHAPI.addHologramLine(hologram, line));
                });
        }
    }


    public void move(Location location) {
        switch (hologramPluginType) {
            case CMI:
                ((com.Zrips.CMI.Modules.Holograms.CMIHologram) hologram).setLoc(location);
                ((com.Zrips.CMI.Modules.Holograms.CMIHologram) hologram).refresh();
                break;
            case DECENT_HOLOGRAM:
                eu.decentsoftware.holograms.api.DHAPI.moveHologram((eu.decentsoftware.holograms.api.holograms.Hologram) hologram, location);
        }
    }
}
