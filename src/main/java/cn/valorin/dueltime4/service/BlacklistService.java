package cn.valorin.dueltime4.service;

import cn.valorin.dueltime4.repository.BlacklistRepository;

import java.util.List;

public class BlacklistService {

    private final BlacklistRepository repo;

    public BlacklistService(BlacklistRepository repo) { this.repo = repo; }

    public boolean isBlacklisted(String playerName) { return repo.isBlacklisted(playerName); }
    public void add(String playerName, String reason) { repo.add(playerName, reason); }
    public void remove(String playerName) { repo.remove(playerName); }
    public List<String> list() { return repo.getAll(); }
}
