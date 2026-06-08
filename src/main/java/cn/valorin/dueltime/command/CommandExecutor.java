package cn.valorin.dueltime.command;


import cn.valorin.dueltime.DuelTimePlugin;
import cn.valorin.dueltime.util.UtilSimilarityComparer;
import cn.valorin.dueltime.yaml.message.Msg;
import cn.valorin.dueltime.yaml.message.MsgBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class CommandExecutor implements TabExecutor {
    private final List<String> subCommandMainAliaList;

    public CommandExecutor(Set<SubCommand> commands) {
        subCommandMainAliaList = commands.stream().map(command -> command.getAliases()[0]).collect(Collectors.toList());
    }

    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (args.length == 0) {
            return CMDMain.onCommand(sender, label);
        } else {
            SubCommand subCommand = DuelTimePlugin.getInstance().getCommandHandler()
                    .getSubCommand(args[0]);
            if (subCommand == null) {
                MsgBuilder.send(Msg.ERROR_SUB_COMMAND_NOT_EXISTS, sender,
                        args[0]);
                String mostSimilarSubCommand = UtilSimilarityComparer.getMostSimilar(args[0], subCommandMainAliaList);
                if (mostSimilarSubCommand != null) {
                    String commandSuggested = "§2/" + label + " §a§n" + mostSimilarSubCommand + "§r";
                    MsgBuilder.sendClickable(Msg.COMMAND_SUGGEST, sender, false, commandSuggested);
                }
                return true;
            }
            return subCommand.onCommand(sender, command, label, args);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {/*
        if (args.length == 1) {
            return Arrays.stream(subCommands)
                    .filter(s -> s.contains(args[0]))
                    .collect(Collectors.toList());
        } else {
            if (args[0].equalsIgnoreCase("publicarea") ||
                    args[0].equalsIgnoreCase("pa")) {
                if (args.length == 5 && args[1].equalsIgnoreCase("tag") && args[2].equalsIgnoreCase("add")) {
                    return Arrays.stream(CachePublicArea.Tag.values())
                            .map(Enum::name)
                            .filter(tagName -> tagName.toLowerCase().contains(args[4].toLowerCase()))
                            .collect(Collectors.toList());
                }
                if (args.length == 3 && args[1].equalsIgnoreCase("tp")) {
                    return Main.getInstance().getCachePublicAreaManager().getMap().values()
                            .stream()
                            .filter(publicArea -> !publicArea.hasTag(CachePublicArea.Tag.DISALLOW_TELEPORT))
                            .map(CachePublicArea::getId)
                            .filter(id -> id.toLowerCase().contains(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
            if ((args[0].equalsIgnoreCase("flat") ||
                    args[0].equalsIgnoreCase("f")) && sender instanceof Player) {
                if (args.length == 3 && args[1].equalsIgnoreCase("back")) {
                    List<CacheFlat> cacheFlatList = new ArrayList<>();
                    for (Map.Entry<String, CacheFlat> kv : Main.getInstance().getCacheFlatManager().getMap().entrySet()) {
                        CacheFlat flat = kv.getValue();
                        if (flat.getOwnerName() == null) {
                            continue;
                        }
                        if (!flat.getOwnerName().equals(sender.getName()) && !flat.getMemberList().contains(sender.getName())) {
                            continue;
                        }
                        cacheFlatList.add(flat);
                    }
                    return cacheFlatList.stream()
                            .map(CacheFlat::getName)
                            .filter(id -> id.toLowerCase().contains(args[2].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
        }*/
        return null;
    }
}
