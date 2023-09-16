package com.cavetale.enderball;

import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.core.event.player.PlayerTeamQuery;
import com.cavetale.fam.trophy.Highscore;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
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
        event.getPlayer().getInventory().clear();
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock)) return;
        FallingBlock fallingBlock = (FallingBlock) event.getEntity();
        if (fallingBlock.getBlockData().getMaterial() != Material.DRAGON_EGG) return;
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
        if (fallingBlock.getBlockData().getMaterial() != Material.DRAGON_EGG) return;
        Game game = plugin.getGameAt(fallingBlock.getLocation());
        if (game == null) return;
        game.onBallBreak(fallingBlock, event);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    void onPlayerInteract(PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;
        switch (event.getAction()) {
        case LEFT_CLICK_BLOCK:
            event.setCancelled(true);
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
        if (fallingBlock.getBlockData().getMaterial() != Material.DRAGON_EGG) return;
        Game game = plugin.getGameAt(fallingBlock.getLocation());
        if (game == null) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        game.onHeaderBall(player, fallingBlock);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.DRAGON_EGG) {
            if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                event.setCancelled(true);
            }
            return;
        }
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    private void onPlayerHud(PlayerHudEvent event) {
        Player player = event.getPlayer();
        Game game = plugin.getGameAt(player.getLocation());
        List<Component> lines = new ArrayList<>();
        if (game != null) {
            game.onSidebar(player, lines);
        }
        if (plugin.getSave().isEvent() && !plugin.getGame().getState().getPhase().isPlaying()) {
            lines.addAll(Highscore.sidebar(plugin.getHighscore()));
        }
        if (lines.isEmpty()) return;
        event.sidebar(PlayerHudPriority.HIGHEST, lines);
        event.bossbar(PlayerHudPriority.HIGH, game.getBossBar());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Game game = plugin.getGameAt(player.getLocation());
        if (game != null) game.onFoodLevelChange(event, player);
    }

    @EventHandler
    protected void onPlayerTeamQuery(PlayerTeamQuery query) {
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
}
