package com.cavetale.enderball;

import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.core.event.player.PlayerTeamQuery;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.fam.trophy.Highscore;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
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
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

@RequiredArgsConstructor
public final class EventListener implements Listener {
    private final EnderballPlugin plugin;

    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onPlayerJoin(PlayerJoinEvent event) {
        Game.ifIn(event.getPlayer().getWorld(), game -> game.onJoin(event.getPlayer()));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private void onPlayerQuit(PlayerQuitEvent event) {
        Game.ifIn(event.getPlayer().getWorld(), game -> event.getPlayer().getInventory().clear());
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    private void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock)) return;
        FallingBlock fallingBlock = (FallingBlock) event.getEntity();
        if (fallingBlock.getBlockData().getMaterial() != Material.DRAGON_EGG) return;
        final Game game = Game.in(fallingBlock.getWorld());
        if (game == null) return;
        if (event.getBlock().isEmpty()) {
            game.onBallLand(fallingBlock, event.getBlock(), event);
        } else {
            game.onBallFall(fallingBlock, event.getBlock(), event);
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    private void onEntityDropItem(EntityDropItemEvent event) {
        if (!(event.getEntity() instanceof FallingBlock)) return;
        final FallingBlock fallingBlock = (FallingBlock) event.getEntity();
        if (fallingBlock.getBlockData().getMaterial() != Material.DRAGON_EGG) return;
        final Game game = Game.in(fallingBlock.getWorld());
        if (game == null) return;
        game.onBallBreak(fallingBlock, event);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    private void onPlayerInteract(PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;
        final Game game = Game.in(player.getWorld());
        if (game == null) return;
        switch (event.getAction()) {
        case LEFT_CLICK_BLOCK:
            event.setCancelled(true);
        case RIGHT_CLICK_BLOCK:
            break;
        default:
            return;
        }
        final Block block = event.getClickedBlock();
        if (block == null) return;
        if (block.getType() != Material.DRAGON_EGG) return;
        if (game == null) return;
        game.onKickBall(player, block, event);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    private void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        // EntityDamageByEntityEvent is not fired when left clicking
        // falling blocks!
        if (!(event.getRightClicked() instanceof FallingBlock)) return;
        FallingBlock fallingBlock = (FallingBlock) event.getRightClicked();
        if (fallingBlock.getBlockData().getMaterial() != Material.DRAGON_EGG) return;
        final Game game = Game.in(fallingBlock.getWorld());
        if (game == null) return;
        event.setCancelled(true);
        final Player player = event.getPlayer();
        game.onHeaderBall(player, fallingBlock);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    private void onBlockPlace(BlockPlaceEvent event) {
        final Block block = event.getBlock();
        final Game game = Game.in(block.getWorld());
        if (game == null) return;
        if (block.getType() != Material.DRAGON_EGG) {
            if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                event.setCancelled(true);
            }
            return;
        }
        game.onPlaceBall(event.getPlayer(), block);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    private void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (player.isOp()) return;
        final Game game = Game.in(player.getWorld());
        if (game == null) return;
        switch (event.getSlotType()) {
        case ARMOR:
            event.setCancelled(true);
        default:
            break;
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    private void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (player.isOp()) return;
        final Game game = Game.in(player.getWorld());
        if (game == null) return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    private void onPlayerHud(PlayerHudEvent event) {
        final Player player = event.getPlayer();
        final Game game = Game.in(player.getWorld());
        final List<Component> lines = new ArrayList<>();
        if (game != null) {
            game.onSidebar(player, lines);
        }
        if (game == null && plugin.getSave().isEvent()) {
            lines.addAll(Highscore.sidebar(plugin.getHighscore()));
        }
        if (lines.isEmpty()) return;
        event.sidebar(PlayerHudPriority.HIGHEST, lines);
        event.bossbar(PlayerHudPriority.HIGH, game.getBossBar());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    private void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Game game = Game.in(player.getWorld());
        if (game != null) game.onFoodLevelChange(event, player);
    }

    @EventHandler
    private void onPlayerTeamQuery(PlayerTeamQuery query) {
        for (Game game : plugin.getGames()) {
            if (game.getState().getTeams().isEmpty()) continue;
            if (game.getState().getNations().isEmpty()) continue;
            for (GameTeam gameTeam : GameTeam.values()) {
                String teamName = "enderball." + game.getName() + "." + gameTeam.name().toLowerCase();
                Nation nation = game.getState().getNations().get(gameTeam.ordinal());
                Component teamDisplayName = Component.join(JoinConfiguration.noSeparators(),
                                                           nation.component,
                                                           Component.text(nation.name, gameTeam.textColor));
                PlayerTeamQuery.Team team = new PlayerTeamQuery.Team(teamName, teamDisplayName, gameTeam.textColor);
                for (Player player : game.getTeamPlayers(gameTeam)) {
                    query.setTeam(player, team);
                }
            }
        }
    }

    /**
     * Joining in the lobby while a game is going puts you in the game
     * as a viewer.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPlayerSpawnLocation(PlayerSpawnLocationEvent event) {
        if (!plugin.getGames().isEmpty() && plugin.getLobby().isWorld(event.getSpawnLocation().getWorld())) {
            final Game game = plugin.getGames().get(0);
            final Location spawnLocation = game.getViewerLocation();
            event.setSpawnLocation(spawnLocation);
            plugin.getLogger().info("Setting player spawn location in game: " + game.getWorld().getName() + ": " + Vec3i.of(spawnLocation));
            // PlayerSpawnLocationEvent#setSpawnLocation does not retain pitch and yaw.
            Bukkit.getScheduler().runTask(plugin, () -> event.getPlayer().teleport(spawnLocation));
        }
    }
}
