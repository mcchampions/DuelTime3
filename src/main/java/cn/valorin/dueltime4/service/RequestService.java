package cn.valorin.dueltime4.service;

import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RequestService {

    private final Map<String, Map<String, Request>> pending = new ConcurrentHashMap<>();

    public void sendRequest(Player sender, Player target, String arenaId) {
        if (sender.getName().equals(target.getName())) {
            sender.sendMessage("§cYou cannot invite yourself!");
            return;
        }
        pending.computeIfAbsent(sender.getName(), k -> new ConcurrentHashMap<>())
            .put(target.getName(), new Request(arenaId, System.currentTimeMillis() + 60000));
        target.sendMessage("§e" + sender.getName() + " §7invites you to a duel! §a/dt accept §7or §c/dt decline");
        sender.sendMessage("§aInvitation sent to " + target.getName());
    }

    public Optional<Request> getRequest(String senderName, String targetName) {
        var inner = pending.get(senderName);
        if (inner == null) return Optional.empty();
        Request r = inner.get(targetName);
        if (r != null && r.expired()) {
            inner.remove(targetName);
            return Optional.empty();
        }
        return Optional.ofNullable(r);
    }

    public Optional<Request> accept(Player accepter, String senderName) {
        var inner = pending.remove(senderName);
        if (inner == null) return Optional.empty();
        Request r = inner.remove(accepter.getName());
        return Optional.ofNullable(r).filter(req -> !req.expired());
    }

    public void decline(Player decliner, String senderName) {
        var inner = pending.get(senderName);
        if (inner != null) inner.remove(decliner.getName());
    }

    public record Request(String arenaId, long expiryTime) {
        boolean expired() { return System.currentTimeMillis() > expiryTime; }
    }
}
