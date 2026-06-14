package cn.valorin.dueltime4.service;

import cn.valorin.dueltime4.arena.Arena;
import cn.valorin.dueltime4.arena.ArenaState;
import cn.valorin.dueltime4.player.Spectator;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class SpectateService {

    private final ArenaService arenaService;

    public SpectateService(ArenaService arenaService) { this.arenaService = arenaService; }

    public boolean canSpectate(Player player, Arena arena) {
        if (arena.getState() != ArenaState.IN_PROGRESS) return false;
        if (arena.hasGamer(player.getName())) return false;
        if (arena.hasSpectator(player.getName())) return false;
        return true;
    }

    public void startSpectating(Player player, Arena arena) {
        Spectator spec = new Spectator(player);
        arena.addSpectator(spec);
        arenaService.addSpectatorMapping(player.getName(), arena.getId());
        player.setGameMode(GameMode.SPECTATOR);
    }

    public void stopSpectating(Player player) {
        Arena arena = arenaService.getSpectating(player);
        if (arena == null) return;
        arena.removeSpectator(player.getName());
        arenaService.removeSpectatorMapping(player.getName());
        for (Spectator s : arena.getSpectators()) {
            if (s.getPlayerName().equals(player.getName())) {
                player.setGameMode(s.getOriginalGameMode());
                player.teleport(s.getOriginalLocation());
                break;
            }
        }
    }
}
