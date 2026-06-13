package cn.valorin.dueltime.arena;

import cn.valorin.dueltime.DuelTimePlugin;
import cn.valorin.dueltime.arena.base.BaseArena;
import cn.valorin.dueltime.arena.base.BaseArenaData;
import cn.valorin.dueltime.arena.base.BaseGamerData;
import cn.valorin.dueltime.arena.type.ArenaType;
import cn.valorin.dueltime.data.mapper.ClassicArenaDataMapper;
import cn.valorin.dueltime.data.pojo.ClassicArenaData;
import cn.valorin.dueltime.event.arena.*;
import cn.valorin.dueltime.gui.CustomInventoryManager;
import cn.valorin.dueltime.yaml.message.Msg;
import cn.valorin.dueltime.yaml.message.MsgBuilder;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * 活跃态竞技场管理器，负责Arena竞技场实例的缓存工作
 * 但出于一些考虑，这个类没有并入cache缓存包中
 * 首先，BaseArena是一个活跃态的竞技场对象，并非直接存入数据库的对象（指BaseArenaData)
 * 其次，竞技场数据没有跨服共享的意义，与其他模块的数据共同点少
 */
public class ArenaManager {
    private final Map<String, BaseArena> arenaMap = new HashMap<>();
    private final Map<String, String> gamerArenaMap = new HashMap<>();
    private final Map<String, String> spectatorArenaMap = new HashMap<>();
    private final Map<String, String> waitingPlayerToArenaMap = new HashMap<>();
    private final Map<String, List<String>> waitingArenaToPlayersMap = new HashMap<>();

    public ArenaManager() {
        reload();
    }

    /**
     * 根据各个类型的场地数据(ArenaData)载入所有竞技场
     */
    public void reload() {
        Map<String, BaseArena> loadedArenaMap = new HashMap<>();
        SqlSessionFactory sqlSessionFactory = DuelTimePlugin.getInstance().getMyBatisManager().getFactory(this.getClass());
        try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
            //载入经典类型竞技场
            ClassicArenaDataMapper classicArenaDataMapper = sqlSession.getMapper(ClassicArenaDataMapper.class);
            classicArenaDataMapper.createTableIfNotExists();
            for (ClassicArenaData arenaData : classicArenaDataMapper.getAll()) {
                ClassicArena classicArena;
                try {
                    classicArena = new ClassicArena(arenaData);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    continue;
                }
                loadedArenaMap.put(arenaData.getId(), classicArena);
            }
            //载入其他类型竞技场...
            arenaMap.clear();
            arenaMap.putAll(loadedArenaMap);
            gamerArenaMap.clear();
            spectatorArenaMap.clear();
            waitingPlayerToArenaMap.clear();
            waitingArenaToPlayersMap.clear();
        }
    }

    public BaseArena get(String id) {
        return arenaMap.get(id);
    }


    public BaseArena getOf(Player player) {
        return arenaMap.get(gamerArenaMap.get(player.getName()));
    }

    public BaseArena getSpectate(Player player) {
        return arenaMap.get(spectatorArenaMap.get(player.getName()));
    }

    //已弃用，现改用缓存来处理玩家-竞技场的对应关系
    @Deprecated
    public BaseArena getOfWithoutCache(Player player) {
        String playerName = player.getName();
        for (BaseArena arena : arenaMap.values()) {
            List<BaseGamerData> gamerDataList = arena.getGamerDataList();
            if (gamerDataList == null || gamerDataList.isEmpty()) {
                continue;
            }
            for (BaseGamerData gamerData : arena.getGamerDataList()) {
                if (gamerData.getPlayer().getName().equals(playerName)) {
                    return arena;
                }
            }
        }
        return null;
    }

    //经由本管理器来调用比赛开始的方法，这么做会事先发布比赛尝试开始的事件，同时顺带清理等待者列表、载入玩家-竞技场映射（这些工作可能会被不走这个方法的开发者疏漏），最后发布比赛开始事件（若未被取消）
    public void start(String id, Object data, Player... players) {
        BaseArena arena = get(id);
        ArenaTryToStartEvent event = new ArenaTryToStartEvent(arena, players);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            for (Player player : players) {
                if (waitingPlayerToArenaMap.containsKey(player.getName())) {
                    removeWaitingPlayer(player);
                }
            }
            return;
        }
        waitingArenaToPlayersMap.remove(id);
        for (Player player : players) {
            addGamerToMap(player, id);
            player.closeInventory();
            waitingPlayerToArenaMap.remove(player.getName());
        }
        arena.start(data, players);
        Bukkit.getServer().getPluginManager().callEvent(new ArenaStartEvent(arena));
        updateStartInventory();
    }

    //经由本管理器来调用比赛结束的方法，这么做会事先发布比赛结束的事件，同时顺带清除相关的玩家-竞技场映射（这些工作可能会被不走这个方法的开发者疏漏）
    public void end(String id) {
        BaseArena arena = get(id);
        ArenaTryToEndEvent event = new ArenaTryToEndEvent(arena);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            for (BaseGamerData gamerData : arena.getGamerDataList()) {
                removeGamerFromMap(gamerData.getPlayerName());
            }
            // 兜底清理：移除所有指向该竞技场的残留映射（如 join() 等途径产生的）
            gamerArenaMap.values().removeIf(id::equals);
            arena.end();
            updateStartInventory();
            Bukkit.getServer().getPluginManager().callEvent(new ArenaEndEvent(arena));
        }
    }

    public void stop(String id, String reason) {
        BaseArena arena = get(id);
        Bukkit.getServer().getPluginManager().callEvent(new ArenaTryToStopEvent(arena, reason));
        for (BaseGamerData gamerData : arena.getGamerDataList()) {
            removeGamerFromMap(gamerData.getPlayerName());
        }
        // 兜底清理：移除所有指向该竞技场的残留映射
        gamerArenaMap.values().removeIf(id::equals);
        updateStartInventory();
        Bukkit.getServer().getPluginManager().callEvent(new ArenaStopEvent(arena, reason));
    }

    //经由本管理器来整合并调用玩家中途加入的方法，这么做会事先发布玩家加入的的事件
    //实际的加入逻辑（如添加GamerData、传送等）由对应竞技场类型的监听器处理
    public void join(Player player, String id, ArenaTryToJoinEvent.Way way) {
        BaseArena arena = get(id);
        ArenaTryToJoinEvent event = new ArenaTryToJoinEvent(player, arena, way);
        Bukkit.getServer().getPluginManager().callEvent(event);
        updateStartInventory();
    }

    //过后添加一个自定义报错类型...............................................
    public void addGamerToMap(Player player, String id) {
        gamerArenaMap.put(player.getName(), id);
    }

    //过后添加一个自定义报错类型...............................................
    public void removeGamerFromMap(String playerName) {
        gamerArenaMap.remove(playerName);
    }

    //经由本管理器来整合并调用玩家观战的方法，这么做会事先发布玩家观战的的事件，同时顺带载入玩家-竞技场映射（这些工作可能会被不走这个方法的开发者疏漏）
    public void spectate(Player player, String id) {
        BaseArena arena = get(id);
        ArenaTryToSpectateEvent event = new ArenaTryToSpectateEvent(player, arena);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            spectatorArenaMap.put(player.getName(), id);
        }
    }

    //过后添加一个自定义报错类型...............................................
    public void removeSpectator(Player player) {
        BaseArena arena = getSpectate(player);
        if (arena == null) {
            spectatorArenaMap.remove(player.getName());
            return;
        }
        ArenaTryToQuitSpectateEvent event = new ArenaTryToQuitSpectateEvent(player, arena);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            arena.removeSpectatorData(player);
            spectatorArenaMap.remove(player.getName());
        }
    }

    public void addWaitingPlayer(Player player, String id) {
        String playerName = player.getName();
        boolean isSwitch = !waitingPlayerToArenaMap.getOrDefault(playerName, id).equals(id);
        BaseArena arena = get(id);
        ArenaTryToWaitEvent event = new ArenaTryToWaitEvent(player, arena);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        if (isSwitch) {
            String oldId = waitingPlayerToArenaMap.get(playerName);
            List<String> watingPlayerList = waitingArenaToPlayersMap.getOrDefault(oldId, new ArrayList<>());
            watingPlayerList.remove(player.getName());
            waitingArenaToPlayersMap.put(oldId, watingPlayerList);
        }
        waitingPlayerToArenaMap.put(playerName, id);
        List<String> watingPlayerList = waitingArenaToPlayersMap.getOrDefault(id, new ArrayList<>());
        watingPlayerList.add(playerName);
        waitingArenaToPlayersMap.put(id, watingPlayerList);
        if (watingPlayerList.size() >= arena.getArenaData().getMinPlayerNumber()) {
            player.closeInventory();
            start(arena.getId(), null, watingPlayerList.stream().map(Bukkit::getPlayerExact).filter(Objects::nonNull).toArray(Player[]::new));
            return;
        }
        Bukkit.getServer().getPluginManager().callEvent(new ArenaWaitEvent(player, arena));
        MsgBuilder.send(isSwitch ? Msg.ARENA_WAIT_SWITCH : Msg.ARENA_WAIT_START, player, arena.getName());
        updateStartInventory();
    }

    public void removeWaitingPlayer(Player player) {
        String arenaId = waitingPlayerToArenaMap.remove(player.getName());
        if (arenaId == null) {
            updateStartInventory();
            return;
        }
        List<String> waitingPlayers = waitingArenaToPlayersMap.get(arenaId);
        if (waitingPlayers != null) {
            waitingPlayers.remove(player.getName());
            if (waitingPlayers.isEmpty()) {
                waitingArenaToPlayersMap.remove(arenaId);
            }
        }
        updateStartInventory();
    }

    public BaseArena getWaitingFor(Player player) {
        return arenaMap.get(waitingPlayerToArenaMap.get(player.getName()));
    }

    public List<String> getWaitingPlayers(String id) {
        return waitingArenaToPlayersMap.getOrDefault(id, new ArrayList<>());
    }

    public Map<String, BaseArena> getMap() {
        return arenaMap;
    }

    public List<BaseArena> getList() {
        return new ArrayList<>(arenaMap.values());
    }

    public int size() {
        return arenaMap.size();
    }

    public void add(BaseArena arena) {
        BaseArenaData arenaData = arena.getArenaData();
        arenaMap.put(arenaData.getId(), arena);
        try (SqlSession sqlSession = DuelTimePlugin.getInstance().getMyBatisManager().getFactory(this.getClass()).openSession(true)) {
            if (arenaData.getTypeId().equals(ArenaType.InternalType.CLASSIC.getId())) {
                sqlSession.getMapper(ClassicArenaDataMapper.class).add((ClassicArenaData) arenaData);
            }
        }
    }

    public void update(BaseArenaData arenaData) {
        BaseArena arena = arenaMap.get(arenaData.getId());
        arena.setArenaData(arenaData);
        arenaMap.put(arenaData.getId(), arena);
        try (SqlSession sqlSession = DuelTimePlugin.getInstance().getMyBatisManager().getFactory(this.getClass()).openSession(true)) {
            if (arenaData.getTypeId().equals(ArenaType.InternalType.CLASSIC.getId())) {
                sqlSession.getMapper(ClassicArenaDataMapper.class).update((ClassicArenaData) arenaData);
            }
        }
    }

    public void delete(String id) {
        arenaMap.remove(id);
        try (SqlSession sqlSession = DuelTimePlugin.getInstance().getMyBatisManager().getFactory(this.getClass()).openSession(true)) {
            sqlSession.getMapper(ClassicArenaDataMapper.class).delete(id);
        }
    }

    private static void updateStartInventory() {
        CustomInventoryManager customInventoryManager = DuelTimePlugin.getInstance().getCustomInventoryManager();
        customInventoryManager.updatePage(customInventoryManager.getStart());
    }
}
