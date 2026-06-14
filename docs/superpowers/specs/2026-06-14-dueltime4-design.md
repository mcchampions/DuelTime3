# DuelTime4 设计规格书

> 日期: 2026-06-14 | 状态: 已确认

## 目标

从 DuelTime3（v3.1.3）全面重构为 DuelTime4（v4.0.0），插件名改为 DuelTime4，文件夹改为 DuelTime4。
仅支持 Paper API 1.21.11 及以上版本，Java 21。

## 核心决策汇总

| 决策点 | 选择 |
|--------|------|
| 整体方向 | 全面推进（代码质量 + 性能 + 可扩展 + 运维） |
| 数据迁移 | 全数据从 DT3 迁移，支持 SQLite/MySQL 源库 |
| 数据访问 | 纯 JDBC + HikariCP，去掉 MyBatis |
| 竞技场类型 | Classic 1v1 + Team 2v2/3v3 + FFA 混战 |
| 多服支持 | 单服为主，SQLite/MySQL 可选，不做跨服同步 |
| 配置格式 | YAML，结构重新设计 |
| 架构模型 | 服务化重构（Service + Repository + 构造器注入） |
| 去掉的依赖 | MyBatis、CMI-API、CMILib、自建多版本兼容层 |
| Java 版本 | Java 21 |

---

## 一、包结构

```
cn.valorin.dueltime4/
├── DuelTimePlugin.java              # Composition Root
├── service/
│   ├── ArenaService.java
│   ├── MatchService.java
│   ├── PlayerService.java
│   ├── SpectateService.java
│   ├── RankingService.java
│   ├── ShopService.java
│   ├── BlacklistService.java
│   └── MigrationService.java
├── arena/
│   ├── Arena.java                   # 抽象基类 + 状态机
│   ├── ArenaState.java
│   ├── ClassicArena.java
│   ├── TeamArena.java
│   └── FFAArena.java
├── player/
│   ├── Gamer.java
│   ├── Spectator.java
│   └── PlayerProfile.java
├── repository/
│   ├── ArenaRepository.java
│   ├── PlayerRepository.java
│   ├── RecordRepository.java
│   ├── LocationRepository.java
│   └── BlacklistRepository.java
├── jdbc/
│   ├── DatabaseManager.java
│   └── SqlHelper.java
├── command/
│   ├── CommandManager.java
│   ├── SubCommand.java
│   └── impl/                         # 各子命令实现
├── config/
│   ├── Config.java
│   └── Messages.java
├── listener/
│   ├── ArenaProtectionListener.java
│   ├── ArenaMatchListener.java
│   ├── ArenaSpectateListener.java
│   ├── PlayerDataListener.java
│   ├── ChatListener.java
│   ├── GuiListener.java
│   └── RankingListener.java
├── gui/
│   ├── Gui.java
│   └── PagedGui.java
├── hook/
│   ├── PlaceholderApiExpansion.java
│   └── HologramManager.java
├── event/
│   ├── ArenaStartEvent.java
│   ├── ArenaEndEvent.java
│   ├── PlayerJoinArenaEvent.java
│   ├── PlayerLeaveArenaEvent.java
│   ├── RankingRefreshEvent.java
│   └── MigrationCompleteEvent.java
├── migration/
│   └── DuelTime3SourceReader.java
└── util/
```

---

## 二、核心架构

手动构造器注入，`DuelTimePlugin.onEnable()` 作为 Composition Root 按序组装：

```
onEnable():
  1. Config + Messages
  2. DatabaseManager → 初始化 HikariCP 连接池
     ├── ArenaRepository
     ├── PlayerRepository
     ├── RecordRepository
     ├── LocationRepository
     ├── ShopRepository
     └── BlacklistRepository
  3. Service 层（构造器注入 Repository）
     ├── PlayerService
     ├── ArenaService
     ├── MatchService
     ├── SpectateService
     ├── RankingService
     ├── ShopService
     ├── BlacklistService
     └── MigrationService
  4. CommandManager(services...)
  5. ListenerManager(services...)
  6. Hook 注册
  7. 定时任务
```

**关键约束：**
- Service 之间可互相依赖（构造器注入）
- Arena 子类是纯领域对象，不依赖任何 Service
- Repository 只被 Service 调用，不被其他层直接访问

---

## 三、竞技场状态机

```
DISABLED ←→ WAITING → STARTING → IN_PROGRESS → ENDING → WAITING
                              ↘ (forceStop) ↗

状态说明:
  WAITING       — 等待玩家加入队列
  STARTING      — 倒计时阶段（如启用），不接受新加入
  IN_PROGRESS   — 比赛进行中
  ENDING        — 结算清理（短暂态）
  DISABLED      — 管理员停用
```

Arena 抽象基类定义模板方法：

| 方法 | Classic (1v1) | Team (2v2/3v3) | FFA (2-N人) |
|------|---------------|----------------|-------------|
| `canJoin(player)` | 对面不能是自己 | 队伍未满 | 人数未满 |
| `onStart()` | 传送到 p1/p2 | 传送两队至出生区 | 散点随机传送 |
| `onTick(sec)` | 检测死亡→判定 | 检测团队存活→判定 | 检测存活人数≤1 |
| `onEnd()` | 计算单人奖罚 | 团队均分奖罚 | 排名阶梯奖罚 |

---

## 四、数据层（JDBC）

**DatabaseManager：** 管理 HikariCP 连接池，全局统一数据库类型（SQLite 或 MySQL）。

**SqlHelper：** 模板方法封装 JDBC 操作。

```java
SqlHelper {
    <T> List<T> query(sql, RowMapper<T>, params...)
    <T> Optional<T> queryOne(sql, RowMapper<T>, params...)
    int update(sql, params...)
    long insert(sql, params...)
    begin() / commit() / rollback()
}
```

每个 Repository 接口对应一张表，SQL 直接写在实现类中，无 XML Mapper。

建表在 `onEnable` 时自动执行（`CREATE TABLE IF NOT EXISTS`），幂等安全。

---

## 五、事件系统

精简为 4 个核心事件，均为"事后通知"（不可取消）：

- `ArenaStartEvent(arena, gamers)` — 比赛正式开始
- `ArenaEndEvent(arena, result)` — 比赛结算完成
- `PlayerJoinArenaEvent(player, arena)` — 玩家进入竞技场
- `PlayerLeaveArenaEvent(player, arena)` — 玩家离开竞技场
- `RankingRefreshEvent(topList)` — 排名刷新
- `MigrationCompleteEvent(result)` — 迁移完成

**去掉 DT3 的 TryTo* 事件**：需要在执行前阻止的操作，改为调用对应 Service 方法，通过返回值判断。

---

## 六、命令系统

保持 `/dueltime` 别名 `/dt`。`SubCommand` 基类统一管理别名、权限、用法描述。构造器注入所需 Service。

支持的命令：

```
/dt help                  /dt arena create/delete/list/toggle
/dt send <player> <id>   /dt accept              /dt decline
/dt join <id>            /dt quit                /dt spectate <id>
/dt start <id>           /dt stop <id>           /dt reload
/dt shop                 /dt rank                /dt record [player]
/dt lobby                /dt blacklist ...       /dt lang <language>
/dt migrate              /dt level
```

---

## 七、配置 & 消息

### config.yml 结构

```yaml
core:
  prefix: "&7&l[&bDuelTime&7&l] "
  language: "zh_CN"

database:
  type: sqlite              # sqlite | mysql
  mysql:
    host: localhost
    port: 3306
    database: dueltime
    username: root
    password: ""

arena:
  defaults:
    classic:
      countdown: 5
      countdown-freeze: true
      time-limit: 300
      allow-spectate: true
      show-health-bossbar: true
      reward:
        win-exp: 30
        win-point: 1
        lose-exp-rate: 0.3
      win-streak:                        # 连胜奖励
        enabled: true
        bonus-point:                      # 连胜额外积分
          "2": 1                          # 2连胜 +1
          "3": 2                          # 3连胜 +2
          "5": 3                          # 5连胜 +3
          "10": 5                         # 10连胜 +5
        bonus-exp-rate:                   # 连胜额外经验倍率
          "3": 0.2                        # 3连胜 +20%经验
          "5": 0.5                        # 5连胜 +50%
          "10": 1.0                       # 10连胜 +100%
      auto-respawn: true
      delayed-back: 5
    team:
      team-size: 2
      countdown: 10
      # ...
    ffa:
      min-players: 3
      max-players: 8
      # ...

level:
  chat-prefix: "&f[%level%&f]"
  tiers:
    - { level: 0, title: "&7无段位", exp-to-next: 10 }
    - { level: 1, title: "&2青铜I", exp-to-next: 20 }
    # ...

ranking:
  refresh-seconds: 30
  hologram: { enabled: true, max-size: 10 }

record:
  show-cooldown: 10
  print-cost: 1

shop:                                       # 积分商城（配置化，不再存数据库）
  items:
    - id: "diamond_sword"
      material: DIAMOND_SWORD
      name: "&b钻石剑"
      lore:
        - "&7一把锋利的钻石剑"
      cost: 50
      commands:                              # 购买后执行的命令
        - "give %player% diamond_sword 1"
    - id: "exp_bottle"
      material: EXPERIENCE_BOTTLE
      name: "&e经验瓶 x16"
      cost: 20
      commands:
        - "give %player% experience_bottle 16"

migration:
  enabled: false
  old-plugin-folder: "plugins/DuelTime3"
  old-database:
    type: sqlite
    sqlite: { path: "plugins/DuelTime3/dueltime.db" }
    mysql: { host: localhost, port: 3306, database: dueltime, username: root, password: "" }
```

### 消息文件

`messages_zh_CN.yml`，直接按路径字符串读取，不绑定枚举。新增消息不需改代码。

---

## 八、数据迁移

**触发方式：**
- 自动：配置 `migration.enabled: true`，启动时执行，完成后自动改回 false
- 手动：`/dt migrate` 命令，不受 enabled 配置限制

**流程：**
1. 连接旧数据库（SQLite 或 MySQL）
2. 读取 DT3 各表数据
3. 映射 DT3 字段 → DT4 字段
4. 写入 DT4 数据库（事务内完成）
5. 输出迁移报告
6. 旧数据不删不写，可反复重试

**迁移范围：** 竞技场数据、玩家数据（含段位/积分/经验/连胜记录）、比赛记录、位置数据、商店数据（从数据库迁移到 config.yml 的 shop.items 节点）、黑名单数据。

---

## 九、监听器

| 监听器 | 职责 | 依赖 |
|--------|------|------|
| ArenaProtectionListener | 场地方块/移动/实体保护 | ArenaService |
| ArenaMatchListener | 比赛内死亡/退出/伤害逻辑 | MatchService |
| ArenaSpectateListener | 观战者限制 | SpectateService |
| PlayerDataListener | 玩家上线加载/下线保存 | PlayerService |
| ChatListener | 聊天相关 | — |
| GuiListener | GUI 点击转发 | — |
| RankingListener | 排名刷新触发 | RankingService |

---

## 十、GUI

- `Gui` 抽象基类：单页 GUI
- `PagedGui` 抽象基类：分页 GUI（排行榜、记录等）
- `GuiListener` 根据 Inventory identifier 找对应 Gui 实例并转发点击
- 去掉 DT3 的 `CustomInventoryManager` / `CustomInventoryHolder` 多层包装

---

## 十一、外部集成

| 集成 | 用途 | 方式 |
|------|------|------|
| PlaceholderAPI | `%dueltime_xxx%` 占位符 | softdepend |
| DecentHolograms | 排行榜全息投影 | softdepend |

**去掉：** CMI、Multiverse-Core、自建 ViaVersion 多版本兼容层（Paper 1.21.11+ 原生 API 已统一）。

---

## 十二、构建

```xml
<groupId>cn.valorin</groupId>
<artifactId>DuelTime4</artifactId>
<version>4.0.0</version>
<java.version>21</java.version>

依赖: paper-api:1.21.11+, HikariCP:5.x, PlaceholderAPI, DecentHolograms
```

---

## 十三、测试

| 层级 | 方式 | 范围 |
|------|------|------|
| Repository | JUnit + SQLite 内存库 | CRUD 正确性 |
| Service | JUnit + Mock Repository | 业务逻辑 |
| Arena | JUnit | 状态机转换、胜负判定 |
| Migration | 集成测试 | DT3 示例数据迁移验证 |

---

## 十四、DT3 已知 Bug 修复清单

重写过程中修复 DT3 已知 bug，包括但不限于：
- 玩家-竞技场映射残留（`gamerArenaMap` 异常条目未清理）
- 热卸载时监听器已反注册导致 `end()` 调用链异常
- 等待队列切换竞技场时的映射泄漏
- 强制停赛时比分不保存

---

## 十五、与 DT3 的兼容性摘要

| 项 | 兼容？ |
|----|-------|
| 插件名/文件夹 | 否（DuelTime → DuelTime4） |
| config.yml | 否（结构重设计） |
| 数据库格式 | 否（通过迁移命令转换） |
| 消息文件 | 否 |
| API / 事件 | 否（事件精简） |
| PlaceholderAPI 占位符 | 尽量保持同名 |
