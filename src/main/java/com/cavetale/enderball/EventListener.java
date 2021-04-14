package com.cavetale.enderball;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@RequiredArgsConstructor
public final class EventListener implements Listener {
    private final EnderballPlugin plugin;

    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getGame().onJoin(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onPlayerQuit(PlayerQuitEvent event) {
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock)) return;
        FallingBlock fallingBlock = (FallingBlock) event.getEntity();
        if (fallingBlock.getMaterial() != Material.DRAGON_EGG) return;
        Game game = plugin.getGameAt(fallingBlock.getLocation());
        if (game == null) return;
        if (event.getBlock().isEmpty()) {
            game.onBallLand(fallingBlock, event.getBlock(), event);
        } else {
            game.onBallFall(fallingBlock, event.getBlock(), event);
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    void onEntityDropItem(EntityDropItemEvent event) {
        if (!(event.getEntity() instanceof FallingBlock)) return;
        FallingBlock fallingBlock = (FallingBlock) event.getEntity();
        if (fallingBlock.getMaterial() != Material.DRAGON_EGG) return;
        Game game = plugin.getGameAt(fallingBlock.getLocation());
        if (game == null) return;
        game.onBallBreak(fallingBlock, event);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    void onPlayerInteract(PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        switch (event.getAction()) {
        case LEFT_CLICK_BLOCK:
        case RIGHT_CLICK_BLOCK:
            break;
        default:
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (block.getType() != Material.DRAGON_EGG) return;
        Game game = plugin.getGameAt(block);
        if (game == null) return;
        game.onKickBall(player, block, event);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        // EntityDamageByEntityEvent is not fired when left clicking
        // falling blocks!
        if (!(event.getRightClicked() instanceof FallingBlock)) return;
        FallingBlock fallingBlock = (FallingBlock) event.getRightClicked();
        if (fallingBlock.getMaterial() != Material.DRAGON_EGG) return;
        Game game = plugin.getGameAt(fallingBlock.getLocation());
        if (game == null) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        game.onHeaderBall(player, fallingBlock);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.DRAGON_EGG) return;
        Game game = plugin.getGameAt(block);
        game.onPlaceBall(event.getPlayer(), block);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked().isOp()) return;
        switch (event.getSlotType()) {
        case ARMOR:
            event.setCancelled(true);
        default:
            break;
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked().isOp()) return;
        event.setCancelled(true);
    }
}
