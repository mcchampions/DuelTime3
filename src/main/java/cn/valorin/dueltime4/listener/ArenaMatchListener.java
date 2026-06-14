package cn.valorin.dueltime4.listener;

import cn.valorin.dueltime4.DuelTimePlugin;
import cn.valorin.dueltime4.arena.Arena;
import cn.valorin.dueltime4.arena.ArenaState;
import cn.valorin.dueltime4.player.Gamer;
import cn.valorin.dueltime4.service.ArenaService;
import cn.valorin.dueltime4.service.MatchService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class ArenaMatchListener implements Listener {

    private MatchService match() { return DuelTimePlugin.getInstance().getMatchService(); }
    private ArenaService arena() { return DuelTimePlugin.getInstance().getArenaService(); }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player damager) || !(e.getEntity() instanceof Player victim)) return;
        Arena a = arena().getByPlayer(damager);
        if (a == null || a.getState() != ArenaState.IN_PROGRESS) return;
        if (a.hasGamer(victim.getName())) match().recordDamage(damager, victim, e.getFinalDamage());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getPlayer();
        Arena a = arena().getByPlayer(p);
        if (a == null || a.getState() != ArenaState.IN_PROGRESS) return;
        Gamer g = a.getGamer(p.getName());
        if (g != null) g.setDead(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        Arena a = arena().getByPlayer(p);
        if (a != null) { Gamer g = a.getGamer(p.getName()); if (g != null) g.setDead(true); }
        arena().removeFromWaiting(p);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        Arena a = arena().getByPlayer(p);
        if (a != null) { var lobby = arena().getLobby(); if (lobby != null) e.setRespawnLocation(lobby); }
    }
}
