package cn.valorin.dueltime4.command;

import org.bukkit.command.CommandSender;

public abstract class SubCommand {
    private final String name;
    private final String[] aliases;
    private final String permission;
    private final String usage;
    private final boolean playerOnly;

    public SubCommand(String name, String[] aliases, String permission, String usage, boolean playerOnly) {
        this.name = name;
        this.aliases = aliases;
        this.permission = permission;
        this.usage = usage;
        this.playerOnly = playerOnly;
    }

    public abstract void execute(CommandSender sender, String[] args);

    public String getName() { return name; }
    public String[] getAliases() { return aliases; }
    public String getPermission() { return permission; }
    public String getUsage() { return usage; }
    public boolean isPlayerOnly() { return playerOnly; }
}
