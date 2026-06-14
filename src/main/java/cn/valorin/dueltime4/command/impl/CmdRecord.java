package cn.valorin.dueltime4.command.impl;

import cn.valorin.dueltime4.DuelTimePlugin;
import cn.valorin.dueltime4.command.SubCommand;
import cn.valorin.dueltime4.repository.RecordRepository;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

public class CmdRecord extends SubCommand {

    public CmdRecord() {
        super("record", new String[]{"record", "rec", "history"}, null, "/dt record [playerName]", true);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        String targetName;
        if (args.length > 0) {
            targetName = args[0];
        } else {
            targetName = sender.getName();
        }

        RecordRepository recordRepository = DuelTimePlugin.getInstance().getRecordRepository();
        List<Map<String, Object>> records = recordRepository.findByPlayer(targetName, 10);

        if (records.isEmpty()) {
            sender.sendMessage("No records found for: " + targetName);
            return;
        }

        sender.sendMessage("§6===== Match Records: " + targetName + " =====");
        for (Map<String, Object> record : records) {
            String result = (String) record.get("result");
            String arenaType = (String) record.get("arena_type");
            String opponent = (String) record.get("opponent_name");
            Integer duration = (Integer) record.get("duration");
            String time = (String) record.get("time");

            String resultColor = switch (result) {
                case "WIN" -> "§a";
                case "LOSE" -> "§c";
                case "DRAW" -> "§e";
                default -> "§7";
            };

            sender.sendMessage(resultColor + result + " §7| " + arenaType
                + " | vs " + (opponent != null ? opponent : "N/A")
                + " | " + duration + "s | " + time);
        }
    }
}
