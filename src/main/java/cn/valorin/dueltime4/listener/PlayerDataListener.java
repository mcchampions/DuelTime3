package cn.valorin.dueltime4.listener;

import cn.valorin.dueltime4.DuelTimePlugin;
import cn.valorin.dueltime4.player.PlayerProfile;
import cn.valorin.dueltime4.service.PlayerService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerDataListener implements Listener {

    private PlayerService svc() { return DuelTimePlugin.getInstance().getPlayerService(); }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) { svc().save(svc().getOrCreate(e.getPlayer().getName())); }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) { svc().save(svc().getOrCreate(e.getPlayer().getName())); }
}
