package cn.valorin.dueltime4.command.impl;

import cn.valorin.dueltime4.DuelTimePlugin;
import cn.valorin.dueltime4.arena.*;
import cn.valorin.dueltime4.command.SubCommand;
import cn.valorin.dueltime4.service.ArenaService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

import java.util.List;

public class CmdArena extends SubCommand {

    public CmdArena() {
        super("arena", new String[]{"arena", "ar"}, "dueltime4.admin", "/dt arena <create|delete|list|toggle>", false);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        ArenaService arenaService = DuelTimePlugin.getInstance().getArenaService();

        if (args.length == 0) {
            sender.sendMessage("Usage: /dt arena <create|delete|list|toggle>");
            return;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Player only.");
                    return;
                }
                if (args.length < 4) {
                    sender.sendMessage("Usage: /dt arena create <type> <id> <name>");
                    return;
                }
                String type = args[1].toLowerCase();
                String id = args[2];
                String name = args[3];
                Location loc = player.getLocation();

                Arena arena = switch (type) {
                    case "classic" -> new ClassicArena(id, name, loc.clone(), loc.clone());
                    case "team" -> new TeamArena(id, name, 2, loc.clone(), loc.clone());
                    case "ffa" -> new FFAArena(id, name, 2, 8, List.of(loc.clone()));
                    default -> null;
                };

                if (arena == null) {
                    sender.sendMessage("Unknown arena type: " + type + ". Valid: classic, team, ffa");
                    return;
                }

                arenaService.saveArena(arena, "{}");
                sender.sendMessage("Arena created: " + name + " (" + id + ") type=" + type);
            }
            case "delete" -> {
                if (args.length < 2) {
                    sender.sendMessage("Usage: /dt arena delete <id>");
                    return;
                }
                arenaService.deleteArena(args[1]);
                sender.sendMessage("Arena deleted: " + args[1]);
            }
            case "list" -> {
                List<Arena> arenas = arenaService.getAll();
                if (arenas.isEmpty()) {
                    sender.sendMessage("No arenas loaded.");
                    return;
                }
                sender.sendMessage("§6===== Arenas =====");
                for (Arena a : arenas) {
                    sender.sendMessage("§e" + a.getId() + " §7- " + a.getName() + " §7[" + a.getTypeName() + "] §7state=" + a.getState());
                }
            }
            case "toggle" -> {
                if (args.length < 2) {
                    sender.sendMessage("Usage: /dt arena toggle <id>");
                    return;
                }
                Arena a = arenaService.get(args[1]);
                if (a == null) {
                    sender.sendMessage("Arena not found: " + args[1]);
                    return;
                }
                boolean enabling = a.getState() == ArenaState.DISABLED;
                arenaService.setArenaEnabled(args[1], enabling);
                sender.sendMessage("Arena " + args[1] + " " + (enabling ? "enabled" : "disabled"));
            }
            default -> sender.sendMessage("Unknown subcommand. Use: create, delete, list, toggle");
        }
    }
}
