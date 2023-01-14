package com.cavetale.enderball;

import com.cavetale.enderball.struct.Vec3i;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;

@Data
public final class GameBall implements Serializable {
    private Vec3i blockVector = Vec3i.ZERO;
    private UUID entityUuid = null;
    private UUID lastKicker = null;
    private UUID assistance = null;
    private long kickCooldown;
    private final Map<UUID, Kick> kicks = new HashMap<>();

    public boolean isEntity() {
        return entityUuid != null;
    }

    public boolean isBlock() {
        return entityUuid == null;
    }

    public FallingBlock getEntity() {
        if (entityUuid == null) return null;
        Entity entity = Bukkit.getEntity(entityUuid);
        return entity instanceof FallingBlock
            ? (FallingBlock) entity
            : null;
    }

    public void removeEntity() {
        FallingBlock entity = getEntity();
        if (entity != null) {
            entity.remove();
        }
        entityUuid = null;
    }

    public Block getBlock(World world) {
        if (entityUuid != null) return null;
        Block result = blockVector.toBlock(world);
        return result.getType() == Material.DRAGON_EGG
            ? result
            : null;
    }

    public void removeBlock(World world) {
        Block block = getBlock(world);
        if (block != null) block.setType(Material.AIR);
    }

    public void remove(World world) {
        if (isBlock()) {
            removeBlock(world);
        } else if (isEntity()) {
            removeEntity();
        }
    }

    public Player getLastKickerPlayer() {
        return lastKicker != null
            ? Bukkit.getPlayer(lastKicker)
            : null;
    }

    public void setLastKicker(UUID uuid) {
        assistance = lastKicker;
        this.lastKicker = uuid;
    }

    public Location getLocation(World world) {
        return entityUuid != null
            ? getEntity().getLocation()
            : getBlock(world).getLocation().add(0.0, 0.5, 0.0);
    }
}
