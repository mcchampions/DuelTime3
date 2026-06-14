package cn.valorin.dueltime4.command.impl;

import cn.valorin.dueltime4.DuelTimePlugin;
import cn.valorin.dueltime4.command.SubCommand;
import cn.valorin.dueltime4.service.ArenaService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

public class CmdLobby extends SubCommand {

    public CmdLobby() {
        super("lobby", new String[]{"lobby", "setlobby"}, "dueltime4.admin", "/dt lobby", false);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Player only.");
            return;
        }
        Location loc = player.getLocation();
        ArenaService arenaService = DuelTimePlugin.getInstance().getArenaService();
        arenaService.setLobby(loc);
        player.sendMessage("Lobby location set to your current position.");
    }
}
