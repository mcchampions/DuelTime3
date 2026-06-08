package cn.valorin.dueltime.network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import cn.valorin.dueltime.DuelTimePlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class VersionChecker {
    public void checkForUpdates(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(DuelTimePlugin.getInstance(), () -> {
            try {
                URL url = new URL("https://gitee.com/valorin/duel-time3/raw/master/src/main/java/cn/valorin/dueltime/network/version");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setUseCaches(false);
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                String latestVersion = in.readLine();
                List<String> updateDetails = new ArrayList<>();
                String line;
                while ((line = in.readLine()) != null) {
                    updateDetails.add(line);
                }
                in.close();

                // 比较版本号
                if (isNewerVersion(DuelTimePlugin.getInstance().getDescription().getVersion(), latestVersion)) {
                    player.sendMessage("§bDuelTime插件发现新版本§d" + latestVersion);
                    player.sendMessage("§f更新内容:");
                    for (String updateDetail : updateDetails) {
                        player.sendMessage(updateDetail);
                    }
                } else {
                    player.sendMessage("§7DuelTime插件已经是最新版本" + DuelTimePlugin.getInstance().getDescription().getVersion() + "(此消息仅管理员可见)");
                }

            } catch (Exception e) {
                player.sendMessage("§c无法为DuelTime插件检查版本更新: " + e.getMessage());
            }
        });
    }

    private boolean isNewerVersion(String currentVersion, String latestVersion) {
        String[] currentParts = currentVersion.split("\\.");
        String[] latestParts = latestVersion.split("\\.");

        int length = Math.max(currentParts.length, latestParts.length);
        for (int i = 0; i < length; i++) {
            int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;

            if (latestPart > currentPart) {
                return true;
            } else if (latestPart < currentPart) {
                return false;
            }
        }
        return false;
    }
}
