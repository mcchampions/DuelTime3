package cn.valorin.dueltime.arena;

import cn.valorin.dueltime.DuelTimePlugin;
import cn.valorin.dueltime.arena.base.BaseArena;
import cn.valorin.dueltime.arena.base.BaseGamerData;
import cn.valorin.dueltime.arena.base.BaseSpectatorData;
import cn.valorin.dueltime.arena.gamer.ClassicGamerData;
import cn.valorin.dueltime.arena.spectator.ClassicSpectatorData;
import cn.valorin.dueltime.arena.type.ArenaType;
import cn.valorin.dueltime.cache.RecordCache;
import cn.valorin.dueltime.cache.LocationCache;
import cn.valorin.dueltime.cache.PlayerDataCache;
import cn.valorin.dueltime.data.pojo.ClassicArenaData;
import cn.valorin.dueltime.data.pojo.ClassicArenaRecordData;
import cn.valorin.dueltime.data.pojo.PlayerData;
import cn.valorin.dueltime.level.Tier;
import cn.valorin.dueltime.listener.arena.BaseArenaListener;
import cn.valorin.dueltime.listener.arena.ClassicArenaListener;
import cn.valorin.dueltime.util.UtilFormat;
import cn.valorin.dueltime.util.UtilGeometry;
import cn.valorin.dueltime.util.UtilMath;
import cn.valorin.dueltime.util.UtilSync;
import cn.valorin.dueltime.viaversion.ViaVersion;
import cn.valorin.dueltime.yaml.configuration.CfgManager;
import cn.valorin.dueltime.yaml.message.Msg;
import cn.valorin.dueltime.yaml.message.MsgBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.valorin.dueltime.arena.type.ArenaType.FunctionInternalType.*;

public class ClassicArena extends BaseArena {

    private int time = 0;
    private BukkitTask timer;
    private Stage stage;
    //附加功能-倒计时：玩家-初始位置映射，用于倒计时期间禁止移动情形下快速拉回各自原点
    private final HashMap<String, Location> playerStartLocationMap = new HashMap<>();
    //附加功能-限时：BossBar进度条
    private BossBar timeLimitBossBar;
    //附加功能-观战：显示双方选手血量的BossBar
    private final HashMap<String, BossBar> healthBossBars = new HashMap<>();
    private Result result;
    private Player winner;


    public ClassicArena(ClassicArenaData arenaData) {
        super(arenaData);
        setState(State.WAITING);
    }

    public Player getOpponent(Player player) {
        String playerName = player.getName();
        if (!hasPlayer(player)) {
            return null;
        }
        for (BaseGamerData gamerData : getGamerDataList()) {
            if (!gamerData.getPlayer().getName().equals(playerName)) {
                return gamerData.getPlayer();
            }
        }
        return null;
    }

    //在不确定双方的Player对象是否存在的情况下（如因下线输掉比赛时为null），就依靠玩家名获取对手名
    public String getOpponent(String playerName) {
        for (BaseGamerData gamerData : getGamerDataList()) {
            if (!gamerData.getPlayerName().equals(playerName)) {
                return gamerData.getPlayerName();
            }
        }
        return null;
    }

    @Override
    public void start(Object data, Player... gamers) {
        if (gamers.length != 2) return;
        if (gamers[0].getName().equals(gamers[1].getName())) return;
        //声明状态，并把选手们都传送进来
        setState(State.IN_PROGRESS_CLOSED);
        Player gamer1 = gamers[0];
        Player gamer2 = gamers[1];
        ClassicGamerData gamerData1 = new ClassicGamerData(gamer1, gamer1.getLocation());
        ClassicGamerData gamerData2 = new ClassicGamerData(gamer2, gamer2.getLocation());
        addGamerData(gamerData1);
        addGamerData(gamerData2);
        ClassicArenaData arenaData = (ClassicArenaData) getArenaData();
        gamer1.teleport(arenaData.getPlayerLocation1());
        playerStartLocationMap.put(gamer1.getName(), arenaData.getPlayerLocation1());
        gamerData1.updateRecentLocation(arenaData.getPlayerLocation1());//初始化最近位置，下同
        gamer2.teleport(arenaData.getPlayerLocation2());
        playerStartLocationMap.put(gamer2.getName(), arenaData.getPlayerLocation2());
        gamerData2.updateRecentLocation(arenaData.getPlayerLocation2());
        //加满血量，并根据是否有观战的附加功能来初始化双方的血条BossBar
        for (BaseGamerData gamerData : getGamerDataList()) {
            Player player = gamerData.getPlayer();
            player.setHealth(player.getMaxHealth());
            if (getArenaData().hasFunction(ArenaType.FunctionInternalType.CLASSIC_SPECTATE) &&
                    (boolean) (getArenaData().getFunctionData(CLASSIC_SPECTATE)[3]) &&
                    DuelTimePlugin.serverVersionInt >= 9) {
                BossBar bossBar = Bukkit.createBossBar(MsgBuilder.get(Msg.ARENA_TYPE_CLASSIC_FUNCTION_SPECTATE_GAMER_HEALTH_BOSSBAR, player,
                        player.getName(), "" + UtilFormat.round(player.getHealth(), 1), "" + UtilFormat.round(player.getMaxHealth(), 1)), BarColor.BLUE, BarStyle.SOLID);
                bossBar.setProgress(1.0);
                healthBossBars.put(player.getName(), bossBar);
            }
        }
        //初始化定时器
        stage = (getArenaData().hasFunction(CLASSIC_COUNTDOWN)) ? Stage.COUNTDOWN : Stage.GAME;
        time = -1;
        timer = Bukkit.getScheduler().runTaskTimer(DuelTimePlugin.getInstance(),
                () -> {
                    time++;
                    //检测是否为倒计时阶段
                    if (getArenaData().hasFunction(CLASSIC_COUNTDOWN)) {
                        if (stage == Stage.COUNTDOWN) {
                            int nowCountdown = (int) (getArenaData().getFunctionData(CLASSIC_COUNTDOWN)[0]) - time;
                            if (nowCountdown > 0) {
                                //提示倒计时并播放音效
                                for (BaseGamerData gamerData : getGamerDataList()) {
                                    Player gamer = gamerData.getPlayer();
                                    MsgBuilder.sendTitle(Msg.ARENA_TYPE_CLASSIC_FUNCTION_COUNTDOWN_GAME_TITLE,
                                            Msg.ARENA_TYPE_CLASSIC_FUNCTION_COUNTDOWN_GAME_SUBTITLE,
                                            0, 25, 0, gamer, ViaVersion.TitleType.LINE,
                                            "" + nowCountdown);
                                    gamer.playSound(gamer.getLocation(), ViaVersion.getSound("BLOCK_NOTE_BASS", "NOTE_BASS"), 1, 0);
                                }
                                return;
                            } else {
                                time = 0;//计时归零，正式开始
                                stage = Stage.GAME;
                            }
                        }
                    }
                    if (time == 0) {
                        //比赛正式开始，提示Title，播放音效
                        for (BaseGamerData gamerData : getGamerDataList()) {
                            Player gamer = gamerData.getPlayer();
                            MsgBuilder.sendTitle(Msg.ARENA_TYPE_CLASSIC_START_TITLE, Msg.ARENA_TYPE_CLASSIC_START_SUBTITLE, 0, 25, 0, gamer, ViaVersion.TitleType.TITLE);
                            gamer.playSound(gamer.getLocation(), ViaVersion
                                    .getSound("ENTITY_PLAYER_LEVELUP", "LEVELUP"), 1.0f, 1.0f);
                        }
                        //如果有倒计时功能，且设置了期间不禁止移动，则要把选手拉回原传送点
                        if (getArenaData().hasFunction(CLASSIC_COUNTDOWN) && !(boolean) getArenaData().getFunctionData(CLASSIC_COUNTDOWN)[1]) {
                            UtilSync.tp(gamer1, arenaData.getPlayerLocation1());
                            UtilSync.tp(gamer2, arenaData.getPlayerLocation2());
                        }
                    }
                    //检测开赛后是否达到时间限制
                    if (getArenaData().hasFunction(CLASSIC_TIME_LIMIT)) {
                        //如果还没有BossBar则创建一个
                        if (timeLimitBossBar == null && DuelTimePlugin.serverVersionInt >= 9) {
                            timeLimitBossBar = Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SOLID);
                        }
                        int timeLimit = (int) getArenaData().getFunctionData(CLASSIC_TIME_LIMIT)[0];
                        int timeLeft = timeLimit - time;
                        //更新BossBar信息（标题、数值）
                        if (timeLimitBossBar != null) {
                            for (BaseGamerData gamerData : getGamerDataList()) {
                                Player player = gamerData.getPlayer();
                                timeLimitBossBar.setProgress(timeLeft / (double) timeLimit);
                                timeLimitBossBar.setTitle(MsgBuilder.get(Msg.ARENA_TYPE_CLASSIC_FUNCTION_TIME_LIMIT_BOSSBAR_TITLE, player,
                                        "" + timeLeft));
                                if (!timeLimitBossBar.getPlayers().contains(player)) {
                                    timeLimitBossBar.addPlayer(player);
                                }
                            }
                        }
                        //若剩余时间为0，则结束比赛
                        if (timeLeft == 0) {
                            confirmResult(Result.DRAW, null);
                            for (BaseGamerData gamerData : getGamerDataList()) {
                                ((ClassicGamerData) gamerData).confirmResult(ClassicArenaRecordData.Result.DRAW);
                            }
                            DuelTimePlugin.getInstance().getArenaManager().end(getId());
                        }
                    }
                },
                0, 20);
    }

    @Override
    public void end() {
        //关闭计时器
        if (timer != null && !timer.isCancelled()) {
            timer.cancel();
        }
        //关闭比赛限时附加功能的BossBar
        if (getArenaData().hasFunction(CLASSIC_TIME_LIMIT) && timeLimitBossBar != null) {
            timeLimitBossBar.removeAll();
        }
        //清空玩家-起始位置映射
        playerStartLocationMap.clear();
        //先判断比赛整体结果，如果不是被强制停止，则根据双方的结果发送提示语并记录比赛
        CfgManager cfgManager = DuelTimePlugin.getInstance().getCfgManager();
        if (result != Result.STOPPED) {
            PlayerDataCache playerDataCache = DuelTimePlugin.getInstance().getCacheManager().getPlayerDataCache();
            RecordCache recordCache = DuelTimePlugin.getInstance().getCacheManager().getArenaRecordCache();
            for (BaseGamerData gamerData : new ArrayList<>(getGamerDataList())) {
                ClassicGamerData classicGamerData = (ClassicGamerData) gamerData;
                ClassicArenaRecordData.Result result = classicGamerData.getResult();
                Player player = classicGamerData.getPlayer();
                String playerName = classicGamerData.getPlayerName();
                Msg resultMsg;
                double expChange;
                PlayerData playerData = playerDataCache.get(playerName);
                Location lobby = DuelTimePlugin.getInstance().getCacheManager().getLocationCache().get(LocationCache.InternalType.LOBBY.getId());
                Location back = getSafeBackLocation(lobby != null ? lobby : ((ClassicGamerData) gamerData).getOriginalLocation());
                if (result == ClassicArenaRecordData.Result.DRAW) {
                    resultMsg = Msg.ARENA_TYPE_CLASSIC_END_RESULT_DRAW;
                    playerData.accumulateArenaClassicDraws();
                    expChange = 0;
                    player.teleport(back);
                } else {
                    if (result == ClassicArenaRecordData.Result.WIN) {
                        resultMsg = Msg.ARENA_TYPE_CLASSIC_END_RESULT_WIN;
                        playerData.accumulateArenaClassicWins();
                        playerData.setPoint(playerData.getPoint() + cfgManager.getArenaClassicRewardWinPoint());
                        expChange = cfgManager.getArenaClassicRewardWinExp();
                        //赢家处放烟花
                        Firework firework = (Firework) player.getWorld().spawnEntity(player.getLocation(),
                                EntityType.FIREWORK_ROCKET);
                        FireworkMeta fm = firework.getFireworkMeta();
                        fm.addEffect(FireworkEffect.builder()
                                .with(FireworkEffect.Type.BALL_LARGE)
                                .withFade(Color.PURPLE).withColor(Color.ORANGE)
                                .withColor(Color.YELLOW).withTrail().build());
                        fm.addEffect(FireworkEffect.builder()
                                .with(FireworkEffect.Type.BALL).withFade(Color.AQUA)
                                .withColor(Color.ORANGE).withColor(Color.YELLOW)
                                .withTrail().build());
                        fm.setPower(2);
                        firework.setFireworkMeta(fm);
                        //提示title，并根据配置开关实现延时传送
                        MsgBuilder.sendTitle(Msg.ARENA_TYPE_CLASSIC_WIN_TITLE, Msg.ARENA_TYPE_CLASSIC_WIN_SUBTITLE, 10, 30, 0, player);
                        if (cfgManager.isArenaClassicDelayedBackEnabled()) {
                            AtomicInteger countdown = new AtomicInteger(cfgManager.getArenaClassicDelayedBackTime());
                            String originalWorldName = player.getWorld().getName();
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    countdown.getAndDecrement();
                                    if (countdown.get() == 0) {
                                        if (ViaVersion.getOnlinePlayers().contains(player) && player.getWorld().getName().equals(originalWorldName)) {
                                            player.teleport(back);
                                        }
                                        this.cancel();
                                    } else if (cfgManager.getArenaClassicDelayedBackTime() - countdown.get() >= 2) {
                                        MsgBuilder.sendTitle(Msg.ARENA_TYPE_CLASSIC_DELAYED_BACK_GAMER_TITLE, Msg.ARENA_TYPE_CLASSIC_DELAYED_BACK_GAMER_SUBTITLE, 0, 25, 0, player, ViaVersion.TitleType.LINE,
                                                "" + countdown);
                                    }
                                }
                            }.runTaskTimer(DuelTimePlugin.getInstance(), 20, 20);
                            BaseArenaListener.tempMovePermit.put(player.getName(), System.currentTimeMillis() + (cfgManager.getArenaClassicDelayedBackTime() + 1) * 1000L);
                        } else {
                            player.teleport(back);
                        }
                    } else {
                        resultMsg = Msg.ARENA_TYPE_CLASSIC_END_RESULT_LOSE;
                        playerData.accumulateArenaClassicLoses();
                        //等级保护：无段位情形下不扣经验，取得段位后不会扣到无段位
                        double expDeducted = cfgManager.getArenaClassicRewardWinExp() * cfgManager.getArenaClassicRewardLoseExpRate();
                        List<Tier> tiers = DuelTimePlugin.getInstance().getLevelManager().getTiers();
                        if (tiers.size() > 1) {
                            double expNeededForFirstTier = tiers.get(0).getExpForLevelUp() * tiers.get(1).getLevel();
                            if (playerData.getExp() < expNeededForFirstTier) {
                                expChange = 0;
                            } else {
                                expChange = -1 * (playerData.getExp() - expDeducted > expNeededForFirstTier ? expDeducted : playerData.getExp() - expNeededForFirstTier);
                            }
                        } else {
                            expChange = -1 * cfgManager.getArenaClassicRewardWinExp() * cfgManager.getArenaClassicRewardLoseExpRate();
                        }
                        //输家处召唤闪电效果
                        Location loc = ((ClassicGamerData) gamerData).getRecentLocation();
                        loc.getWorld().strikeLightningEffect(loc);
                        if (player.isDead()) {
                            //死亡时，若开启了自动复活，则执行复活操作，反之为其手动复活设置复活点为原先的位置
                            if (DuelTimePlugin.getInstance().getCfgManager().isArenaClassicAutoRespawnEnabled()) {
                                if ((DuelTimePlugin.getInstance().getCfgManager().getArenaClassicAutoRespawnCode().equalsIgnoreCase(RespawnCode.SPIGOT.name()))) {
                                    player.spigot().respawn();
                                } else {
                                    player.setHealth(player.getMaxHealth());
                                }
                                Bukkit.getScheduler().runTaskLater(DuelTimePlugin.getInstance(), () -> player.teleport(back), 5);
                            } else {
                                ClassicArenaListener.respawnLocMap.put(player.getName(), back);
                            }
                        } else {
                            //若未死亡，说明是输入退出指令的情形（或者被自动复活了），则直接传送
                            player.teleport(back);
                        }
                    }
                    playerData.setExp(playerData.getExp() + expChange);
                    playerData.accumulateTotalGameNumber();
                    playerData.accumulateArenaClassicTime(time);
                    playerData.accumulateTotalGameTime(time);
                    playerDataCache.set(playerName, playerData);
                    if (result == ClassicArenaRecordData.Result.WIN) {
                        MsgBuilder.send(Msg.ARENA_TYPE_CLASSIC_EARN_POINT, player,
                                "" + cfgManager.getArenaClassicRewardWinPoint(), "" + playerData.getPoint());
                    }
                }
                //用一句话告知结果（若还在线）
                if (player.isOnline()) {
                    MsgBuilder.send(resultMsg, player, false);
                }
                //用多行提示语告知比赛历程的具体信息，体现DT3重视过程的设计理念
                String opponentPlayerName = getOpponent(playerName);
                double totalDamage = UtilMath.round(classicGamerData.getTotalDamage());
                double maxDamage = UtilMath.round(classicGamerData.getMaxDamage());
                double averageDamage = classicGamerData.getHitTime() != 0 ? UtilMath.round(classicGamerData.getTotalDamage() / classicGamerData.getHitTime()) : 0;
                MsgBuilder.sendsClickable(Msg.ARENA_TYPE_CLASSIC_END_NOTIFY_INFO, player, false,
                        getArenaData().getName(),
                        opponentPlayerName,
                        DuelTimePlugin.getInstance().getLevelManager().getTier(opponentPlayerName).getTitle(),
                        "" + time,
                        UtilFormat.distinguishPositiveNumber(expChange),
                        "" + totalDamage,
                        "" + maxDamage,
                        "" + averageDamage);
                //记录比赛数据
                ClassicArenaRecordData recordData = new ClassicArenaRecordData(
                        playerName,
                        getArenaData().getId(),
                        opponentPlayerName,
                        result,
                        time,
                        expChange,
                        classicGamerData.getHitTime(),
                        UtilMath.round(classicGamerData.getTotalDamage()),
                        UtilMath.round(classicGamerData.getMaxDamage()),
                        classicGamerData.getHitTime() != 0 ? UtilMath.round(classicGamerData.getTotalDamage() / classicGamerData.getHitTime()) : 0,
                        new SimpleDateFormat("yyyy/M/d HH:mm").format(new Date())
                );
                recordCache.add(player, recordData);
            }
        } else {
            //强制停赛，则直接把玩家带回大厅或原点
            Location lobby = DuelTimePlugin.getInstance().getCacheManager().getLocationCache().get(LocationCache.InternalType.LOBBY.getId());
            for (BaseGamerData gamerData : getGamerDataList()) {
                Location back = getSafeBackLocation(lobby != null ? lobby : ((ClassicGamerData) gamerData).getOriginalLocation());
                gamerData.getPlayer().teleport(back);
            }
        }
        //告知观众比赛结果并传送回原位置
        ArenaManager arenaManager = DuelTimePlugin.getInstance().getArenaManager();
        if (getArenaData().hasFunction(CLASSIC_SPECTATE)) {
            if ((boolean) (getArenaData().getFunctionData(CLASSIC_SPECTATE)[3])) {
                for (BossBar bossBar : healthBossBars.values()) {
                    bossBar.removeAll();
                }
                healthBossBars.clear();
            }
            for (BaseSpectatorData spectatorData : new ArrayList<>(getSpectatorDataList())) {
                Player spectator = spectatorData.getPlayer();
                Location logLocation = ((ClassicSpectatorData) spectatorData).getOriginalLocation();
                if (spectator != null) {
                    switch (result) {
                        case CLEAR:
                            MsgBuilder.sends(Msg.ARENA_TYPE_CLASSIC_FUNCTION_SPECTATE_OVER, spectator,
                                    getName(), UtilFormat.toString(getGamerDataList()), winner.getName(), "" + time);
                            break;
                        case DRAW:
                            MsgBuilder.sends(Msg.ARENA_TYPE_CLASSIC_FUNCTION_SPECTATE_OVER_IN_A_DRAW, spectator,
                                    getName(), UtilFormat.toString(getGamerDataList()), "" + time);
                            break;
                        case STOPPED:
                            MsgBuilder.send(Msg.ARENA_TYPE_CLASSIC_FUNCTION_SPECTATE_OVER_BY_STOP, spectator,
                                    getName());
                    }
                    //恢复原本的游戏模式
                    spectator.setGameMode(((ClassicSpectatorData) spectatorData).getOriginalGameMode());
                    //根据配置实现延时传送
                    if (cfgManager.isArenaClassicDelayedBackEnabled()) {
                        AtomicInteger countdown = new AtomicInteger(cfgManager.getArenaClassicDelayedBackTime());
                        String originalWorldName = spectator.getWorld().getName();
                        Location originalLocation = ((ClassicSpectatorData) spectatorData).getOriginalLocation();
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                countdown.getAndDecrement();
                                if (countdown.get() == 0) {
                                    if (ViaVersion.getOnlinePlayers().contains(spectator) && spectator.getWorld().getName().equals(originalWorldName)) {
                                        spectator.teleport(originalLocation);
                                    }
                                    this.cancel();
                                } else if (cfgManager.getArenaClassicDelayedBackTime() - countdown.get() >= 2) {
                                    MsgBuilder.sendTitle(Msg.ARENA_TYPE_CLASSIC_DELAYED_BACK_SPECTATOR_TITLE, Msg.ARENA_TYPE_CLASSIC_DELAYED_BACK_SPECTATOR_SUBTITLE, 0, 25, 0, spectator, ViaVersion.TitleType.LINE,
                                            "" + countdown);
                                }
                            }
                        }.runTaskTimer(DuelTimePlugin.getInstance(), 20, 20);
                        BaseArenaListener.tempMovePermit.put(spectator.getName(), System.currentTimeMillis() + (cfgManager.getArenaClassicDelayedBackTime() + 1) * 1000L);
                    } else {
                        spectator.teleport(logLocation);
                    }
                }
                //从ArenaManager中移除观赛者与竞技场的对应关系并随之清空观赛者列表
                arenaManager.removeSpectator(spectator);
            }
        }
        //全服广播
        if (result == Result.CLEAR) {
            MsgBuilder.broadcast(Msg.ARENA_TYPE_CLASSIC_END_BROADCAST, false,
                    getName(), winner.getName(), getOpponent(winner).getName(), "" + time);
        } else if (result == Result.DRAW) {
            MsgBuilder.broadcast(Msg.ARENA_TYPE_CLASSIC_END_BROADCAST_DRAW, false,
                    getName(), getGamerDataList().get(0).getPlayer().getName(), getGamerDataList().get(1).getPlayer().getName(), "" + time);
        }
        //清空参赛者数据
        setGamerDataList(new ArrayList<>());
        //将竞技场恢复为等待状态
        setState(State.WAITING);
    }

    private Location getSafeBackLocation(Location preferred) {
        if (preferred != null && UtilGeometry.getArena(preferred) == null) {
            return preferred;
        }
        if (preferred != null && preferred.getWorld() != null) {
            return preferred.getWorld().getSpawnLocation();
        }
        return Bukkit.getWorlds().get(0).getSpawnLocation();
    }

    public void confirmResult(ClassicArena.Result result, Player winner) {
        this.result = result;
        this.winner = winner;
    }

    public Stage getStage() {
        return stage;
    }

    public void addSpectator(Player player) {
        if (getGamerData(player.getName()) != null) {
            return;
        }
        addSpectatorData(new ClassicSpectatorData(player, player.getLocation(), player.getGameMode()));
        if (getArenaData().hasFunction(CLASSIC_SPECTATE)) {
            for (BossBar bossBar : healthBossBars.values()) {
                bossBar.addPlayer(player);
            }
        }
    }

    public void updateGamerHealthSpectated(Player player) {
        if (!healthBossBars.isEmpty()) {
            BossBar bossBar = healthBossBars.get(player.getName());
            bossBar.setProgress(player.getHealth() / player.getMaxHealth());
            bossBar.setTitle(MsgBuilder.get(Msg.ARENA_TYPE_CLASSIC_FUNCTION_SPECTATE_GAMER_HEALTH_BOSSBAR, player,
                    player.getName(), "" + UtilFormat.round(player.getHealth(), 1), "" + UtilFormat.round(player.getMaxHealth(), 1)));
        } else {

        }
    }

    public HashMap<String, BossBar> getHealthBossBars() {
        return healthBossBars;
    }

    public Location getPlayerStartLocationMap(String playerName) {
        return playerStartLocationMap.get(playerName);
    }

    public enum Stage {
        COUNTDOWN,
        GAME
    }

    public enum Result {
        CLEAR,
        DRAW,
        STOPPED
    }

    public enum RespawnCode {
        SPIGOT,
        SETHEALTH
    }
}
