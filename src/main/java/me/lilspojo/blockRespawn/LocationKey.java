package me.lilspojo.blockRespawn;

import org.bukkit.Location;
import java.util.Objects;

public final class LocationKey {
    public final String world;
    public final int x, y, z;
    // Fetch block location data
    public LocationKey(Location loc) {
        this.world = loc.getWorld().getName();
        this.x = loc.getBlockX();
        this.y = loc.getBlockY();
        this.z = loc.getBlockZ();
    }
    // Location equals check
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LocationKey)) return false;
        LocationKey k = (LocationKey) o;
        return x == k.x && y == k.y && z == k.z && Objects.equals(world, k.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, x, y, z);
    }
}