package cn.valorin.dueltime4.util;

import org.bukkit.Location;

public final class GeometryUtil {

    private GeometryUtil() {}

    public static boolean isInCuboid(Location loc, Location corner1, Location corner2) {
        if (!corner1.getWorld().equals(loc.getWorld())) return false;
        double minX = Math.min(corner1.getX(), corner2.getX());
        double maxX = Math.max(corner1.getX(), corner2.getX());
        double minY = Math.min(corner1.getY(), corner2.getY());
        double maxY = Math.max(corner1.getY(), corner2.getY());
        double minZ = Math.min(corner1.getZ(), corner2.getZ());
        double maxZ = Math.max(corner1.getZ(), corner2.getZ());
        return loc.getX() >= minX && loc.getX() <= maxX
            && loc.getY() >= minY && loc.getY() <= maxY
            && loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }
}
