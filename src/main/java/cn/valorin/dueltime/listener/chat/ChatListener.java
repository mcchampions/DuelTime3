package cn.valorin.dueltime.listener.chat;

import cn.valorin.dueltime.DuelTimePlugin;
import cn.valorin.dueltime.level.LevelManager;
import cn.valorin.dueltime.yaml.configuration.CfgManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatListener implements Listener {
    private static final Pattern PATTERN = Pattern.compile("%v", Pattern.LITERAL);

    @EventHandler
    public void showTierTitleWhileChatting(AsyncPlayerChatEvent e) {
        CfgManager cfgManager = DuelTimePlugin.getInstance().getCfgManager();
        if (!cfgManager.isTierTitleShowedInChatBoxEnabled()) {
            return;
        }
        Player player = e.getPlayer();
        LevelManager levelManager = DuelTimePlugin.getInstance().getLevelManager();
        String title = levelManager.getTier(player.getName()).getTitle();
        e.setFormat(PATTERN.matcher(cfgManager.getTierTitleShowedInChatBoxFormat()).replaceAll(Matcher.quoteReplacement(title)) + e.getFormat());
    }
}