package com.cavetale.enderball;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

@RequiredArgsConstructor
public final class Lobby {
    private final EnderballPlugin plugin;

    public World getWorld() {
        return Bukkit.getWorlds().get(0);
    }

    public boolean isWorld(World world) {
        return world.equals(getWorld());
    }

    public void warp(Player player) {
        player.eject();
        player.leaveVehicle();
        player.teleport(getWorld().getSpawnLocation());
        player.setFallDistance(0f);
        player.setVelocity(new Vector());
        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.setExhaustion(0f);
    }

    public List<Player> getPlayers() {
        return getWorld().getPlayers();
    }
}
