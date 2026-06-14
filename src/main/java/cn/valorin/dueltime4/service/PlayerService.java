package cn.valorin.dueltime4.service;

import cn.valorin.dueltime4.config.Config;
import cn.valorin.dueltime4.player.PlayerProfile;
import cn.valorin.dueltime4.repository.PlayerRepository;

import java.util.*;

public class PlayerService {

    private final PlayerRepository repo;
    private final Config config;

    public PlayerService(PlayerRepository repo, Config config) {
        this.repo = repo;
        this.config = config;
    }

    public PlayerProfile getOrCreate(String playerName) {
        return repo.findByName(playerName).orElseGet(() -> new PlayerProfile(playerName));
    }

    public void save(PlayerProfile profile) {
        repo.upsert(profile);
    }

    public List<PlayerProfile> getTopByExp(int limit) {
        return repo.findTop(limit, "exp");
    }

    public List<PlayerProfile> getTopByPoint(int limit) {
        return repo.findTop(limit, "point");
    }

    /** Calculate win streak bonus points for a player at their current streak */
    public int getWinStreakBonus(String arenaType, int currentStreak) {
        if (!config.getWinStreakEnabled(arenaType)) return 0;
        Map<String, Object> bonusPoints = config.getWinStreakSection(arenaType, "bonus-point");
        if (bonusPoints.isEmpty()) return 0;

        int bonus = 0;
        for (int streak = currentStreak; streak >= 0; streak--) {
            Object val = bonusPoints.get(String.valueOf(streak));
            if (val instanceof Number) {
                bonus = ((Number) val).intValue();
                break;
            }
        }
        return bonus;
    }

    /** Calculate win streak bonus exp rate (e.g. 0.5 = +50%) */
    public double getWinStreakExpRate(String arenaType, int currentStreak) {
        if (!config.getWinStreakEnabled(arenaType)) return 0;
        Map<String, Object> bonusRates = config.getWinStreakSection(arenaType, "bonus-exp-rate");
        if (bonusRates.isEmpty()) return 0;

        double rate = 0;
        for (int streak = currentStreak; streak >= 0; streak--) {
            Object val = bonusRates.get(String.valueOf(streak));
            if (val instanceof Number) {
                rate = ((Number) val).doubleValue();
                break;
            }
        }
        return rate;
    }
}
