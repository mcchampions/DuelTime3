package cn.valorin.dueltime4.listener;

import cn.valorin.dueltime4.DuelTimePlugin;
import cn.valorin.dueltime4.service.ArenaService;
import cn.valorin.dueltime4.service.SpectateService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ArenaSpectateListener implements Listener {

    private ArenaService arena() { return DuelTimePlugin.getInstance().getArenaService(); }
    private SpectateService spectate() { return DuelTimePlugin.getInstance().getSpectateService(); }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player v && arena().getSpectating(v) != null) e.setCancelled(true);
        if (e.getDamager() instanceof Player d && arena().getSpectating(d) != null) e.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) { spectate().stopSpectating(e.getPlayer()); }
}
