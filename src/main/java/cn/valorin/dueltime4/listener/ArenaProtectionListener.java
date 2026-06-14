package cn.valorin.dueltime4.listener;

import cn.valorin.dueltime4.DuelTimePlugin;
import cn.valorin.dueltime4.arena.Arena;
import cn.valorin.dueltime4.arena.ArenaState;
import cn.valorin.dueltime4.service.ArenaService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.block.Block;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;

public class ArenaProtectionListener implements Listener {

    private static final String BYPASS = "dueltime4.admin";

    private ArenaService svc() { return DuelTimePlugin.getInstance().getArenaService(); }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (p.hasPermission(BYPASS)) return;
        Location to = e.getTo();
        if (to == null || e.getFrom().getBlockX() == to.getBlockX()
            && e.getFrom().getBlockY() == to.getBlockY()
            && e.getFrom().getBlockZ() == to.getBlockZ()) return;

        Arena playerArena = svc().getByPlayer(p);
        Arena targetArena = findArenaAt(to);
        if (targetArena == null) return;

        // Player in one arena cannot enter a different arena
        if (playerArena != null && !playerArena.getId().equals(targetArena.getId())) {
            e.setCancelled(true);
            return;
        }

        // Non-participant cannot enter any arena
        if (playerArena == null) {
            if (targetArena.getState() == ArenaState.WAITING || targetArena.getState() == ArenaState.DISABLED) {
                e.setCancelled(true); return;
            }
            if (!targetArena.hasGamer(p.getName()) && !targetArena.hasSpectator(p.getName())) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (p.hasPermission(BYPASS)) return;
        Arena playerArena = svc().getByPlayer(p);
        Arena blockArena = findArenaAt(e.getBlock().getLocation());
        if (playerArena == null && blockArena != null) e.setCancelled(true);
        else if (playerArena != null && (blockArena == null || !blockArena.getId().equals(playerArena.getId()))) e.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (p.hasPermission(BYPASS)) return;
        Arena playerArena = svc().getByPlayer(p);
        Arena blockArena = findArenaAt(e.getBlock().getLocation());
        if (playerArena == null && blockArena != null) e.setCancelled(true);
        else if (playerArena != null && (blockArena == null || !blockArena.getId().equals(playerArena.getId()))) e.setCancelled(true);
    }

    @EventHandler
    public void onPvP(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player damager) || !(e.getEntity() instanceof Player victim)) return;
        Arena victimArena = svc().getByPlayer(victim);
        if (victimArena == null) return;
        Arena damagerArena = svc().getByPlayer(damager);
        if (damagerArena == null || !damagerArena.getId().equals(victimArena.getId())) e.setCancelled(true);
    }

    @EventHandler
    public void onLiquidFlow(BlockFromToEvent e) {
        if (findArenaAt(e.getToBlock().getLocation()) != null) e.setCancelled(true);
    }

    @EventHandler
    public void onPiston(BlockPistonExtendEvent e) {
        Arena pistonArena = findArenaAt(e.getBlock().getLocation());
        for (Block b : e.getBlocks()) {
            Arena target = findArenaAt(b.getRelative(e.getDirection()).getLocation());
            if (pistonArena == null && target != null) { e.setCancelled(true); return; }
            if (pistonArena != null && (target == null || !target.getId().equals(pistonArena.getId()))) { e.setCancelled(true); return; }
        }
    }

    private Arena findArenaAt(Location loc) {
        for (Arena a : svc().getAll()) {
            if (a.contains(loc)) return a;
        }
        return null;
    }
}
