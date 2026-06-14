package cn.valorin.dueltime4.service;

import cn.valorin.dueltime4.DuelTimePlugin;
import cn.valorin.dueltime4.arena.*;
import cn.valorin.dueltime4.config.Config;
import cn.valorin.dueltime4.event.ArenaEndEvent;
import cn.valorin.dueltime4.event.ArenaStartEvent;
import cn.valorin.dueltime4.event.PlayerJoinArenaEvent;
import cn.valorin.dueltime4.event.PlayerLeaveArenaEvent;
import cn.valorin.dueltime4.player.Gamer;
import cn.valorin.dueltime4.player.PlayerProfile;
import cn.valorin.dueltime4.player.Spectator;
import cn.valorin.dueltime4.repository.RecordRepository;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.SimpleDateFormat;
import java.util.*;

public class MatchService {

    private final ArenaService arenaService;
    private final PlayerService playerService;
    private final RecordRepository recordRepo;
    private final Config config;
    private final Map<String, MatchSession> sessions = new HashMap<>();

    public MatchService(ArenaService arenaService, PlayerService playerService,
                        RecordRepository recordRepo, Config config) {
        this.arenaService = arenaService;
        this.playerService = playerService;
        this.recordRepo = recordRepo;
        this.config = config;
    }

    /**
     * Start a match: called when waiting queue reaches min players or admin forces start.
     */
    public void startMatch(String arenaId, List<Player> players) {
        Arena arena = arenaService.get(arenaId);
        if (arena == null) return;
        if (arena.getState() != ArenaState.WAITING) return;

        List<Gamer> gamers = new ArrayList<>();
        for (Player p : players) {
            Gamer g = new Gamer(p);
            gamers.add(g);
            arena.addGamer(g);
            arenaService.addGamerMapping(p.getName(), arenaId);
            Bukkit.getPluginManager().callEvent(new PlayerJoinArenaEvent(p, arena));
        }

        arena.setState(ArenaState.STARTING);
        String type = arena.getTypeName();
        int countdown = config.getArenaCountdown(type);
        MatchSession session = new MatchSession(arena, gamers);

        if (countdown > 0) {
            session.stage = "COUNTDOWN";
            session.countdownRemaining = countdown;
        } else {
            session.stage = "GAME";
        }

        sessions.put(arenaId, session);

        arena.setTimer(new BukkitRunnable() {
            int tick = countdown > 0 ? -countdown : 0;

            @Override
            public void run() {
                if (arena.getState() == ArenaState.ENDING) { cancel(); return; }

                if ("COUNTDOWN".equals(session.stage)) {
                    tick++;
                    if (tick >= 0) {
                        session.stage = "GAME";
                        arena.setState(ArenaState.IN_PROGRESS);
                        arena.start();
                        Bukkit.getPluginManager().callEvent(new ArenaStartEvent(arena, gamers));
                        tick = 0;
                        return;
                    }
                    int remaining = -tick;
                    for (Gamer g : gamers) {
                        g.getPlayer().sendActionBar(
                            net.kyori.adventure.text.Component.text("§e" + remaining + " §7seconds..."));
                    }
                } else {
                    tick++;
                    arena.tick(tick);

                    Map<String, Object> endResult = arena.end();
                    String reason = (String) endResult.get("reason");
                    if (!"DRAW".equals(reason) || allDead(arena)) {
                        endMatch(arenaId, endResult);
                        cancel();
                        return;
                    }

                    int timeLimit = config.getArenaTimeLimit(arena.getTypeName());
                    if (timeLimit > 0 && tick >= timeLimit) {
                        endMatch(arenaId, Map.of("reason", "DRAW"));
                        cancel();
                    }
                }
            }
        }.runTaskTimer(DuelTimePlugin.getInstance(), 0, 20));
    }

    private boolean allDead(Arena arena) {
        return arena.getGamers().stream().allMatch(Gamer::isDead);
    }

    /**
     * End a match: rewards, records, cleanup, broadcast.
     */
    public void endMatch(String arenaId, Map<String, Object> result) {
        Arena arena = arenaService.get(arenaId);
        if (arena == null) return;

        arena.setState(ArenaState.ENDING);
        arena.cancelTimer();
        MatchSession session = sessions.remove(arenaId);
        int duration = session != null ? session.getElapsed() : 0;

        String reason = (String) result.get("reason");
        List<Gamer> gamers = new ArrayList<>(arena.getGamers());
        String arenaType = arena.getTypeName();
        String time = new SimpleDateFormat("yyyy/M/d HH:mm").format(new Date());

        for (Gamer g : gamers) {
            arenaService.removeGamerMapping(g.getPlayerName());
            Player player = g.getPlayer();
            PlayerProfile profile = playerService.getOrCreate(g.getPlayerName());
            double expChange = 0;

            if ("CLEAR".equals(reason)) {
                Object winnerObj = result.get("winner");
                boolean isWinner = isWinner(g, winnerObj, arena);
                if (isWinner) {
                    int basePoint = config.getArenaWinPoint(arenaType);
                    double baseExp = config.getArenaWinExp(arenaType);
                    int streakBonus = playerService.getWinStreakBonus(arenaType, profile.getWinStreak() + 1);
                    double expRate = playerService.getWinStreakExpRate(arenaType, profile.getWinStreak() + 1);

                    expChange = baseExp * (1 + expRate);
                    profile.onWin();
                    profile.addPoint(basePoint + streakBonus);
                    profile.addExp(expChange);
                    profile.incrementWins();
                    g.setResult("WIN");
                } else {
                    double expLoss = config.getArenaWinExp(arenaType) * config.getArenaLoseExpRate(arenaType);
                    expChange = -expLoss;
                    profile.onLose();
                    profile.addExp(expChange);
                    profile.incrementLoses();
                    g.setResult("LOSE");
                }
            } else {
                profile.onDraw();
                profile.incrementDraws();
                g.setResult("DRAW");
            }
            profile.addTime(duration);
            playerService.save(profile);

            // Record to DB
            String opponent = null;
            if (arena instanceof ClassicArena ca) {
                opponent = ca.getOpponentName(g.getPlayerName());
            }
            recordRepo.insert(g.getPlayerName(), arenaId, arenaType, opponent,
                g.getResult(), duration, expChange, g.getHitCount(),
                g.getTotalDamage(), g.getMaxDamage(),
                g.getHitCount() > 0 ? g.getTotalDamage() / g.getHitCount() : 0,
                time);

            // Teleport back
            Location back = arenaService.getLobby();
            if (back == null) back = g.getOriginalLocation();
            if (player.isOnline()) player.teleport(back);

            Bukkit.getPluginManager().callEvent(new PlayerLeaveArenaEvent(player, arena));
        }

        // Return spectators
        for (Spectator s : new ArrayList<>(arena.getSpectators())) {
            s.getPlayer().teleport(s.getOriginalLocation());
            s.getPlayer().setGameMode(s.getOriginalGameMode());
            arenaService.removeSpectatorMapping(s.getPlayerName());
        }
        arena.clearSpectators();

        Bukkit.getPluginManager().callEvent(new ArenaEndEvent(arena, result));

        arena.clearGamers();
        arena.setState(ArenaState.WAITING);
    }

    private boolean isWinner(Gamer g, Object winnerObj, Arena arena) {
        if (winnerObj instanceof Gamer w) {
            return w.getPlayerName().equals(g.getPlayerName());
        }
        if (winnerObj instanceof Integer teamIdx && arena instanceof TeamArena ta) {
            return ta.getTeam(g.getPlayerName()) == teamIdx;
        }
        return false;
    }

    public void forceStop(String arenaId, String reason) {
        endMatch(arenaId, Map.of("reason", "STOPPED", "stopReason", reason));
    }

    public void shutdown() {
        for (String id : new ArrayList<>(sessions.keySet())) {
            forceStop(id, "Server shutdown");
        }
    }

    public void recordDamage(Player attacker, Player victim, double damage) {
        Arena arena = arenaService.getByPlayer(attacker);
        if (arena != null) {
            Gamer gamer = arena.getGamer(attacker.getName());
            if (gamer != null) {
                gamer.recordHit(damage);
            }
        }
    }

    private static class MatchSession {
        Arena arena;
        List<Gamer> gamers;
        String stage = "GAME";
        int countdownRemaining;
        final long startTime = System.currentTimeMillis();

        MatchSession(Arena arena, List<Gamer> gamers) {
            this.arena = arena;
            this.gamers = gamers;
        }

        int getElapsed() {
            return (int) ((System.currentTimeMillis() - startTime) / 1000);
        }
    }
}
