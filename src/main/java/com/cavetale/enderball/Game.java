package com.cavetale.enderball;

import com.cavetale.enderball.struct.Cuboid;
import com.cavetale.enderball.struct.Vec3i;
import com.cavetale.enderball.util.Fireworks;
import com.cavetale.enderball.util.Json;
import com.destroystokyo.paper.Title;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

/**
 * Runtime class of one game.
 */
@RequiredArgsConstructor @Getter
public final class Game {
    private final EnderballPlugin plugin;
    private final GameBoard board;
    private final GameState state;
    private BukkitTask task;
    private Random random = new Random();
    private BossBar bossBar;
    private int hungerTicks = 0;
    private int fireworkTicks = 0;

    public void enable() {
        board.prep();
        state.prep();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
        bossBar = Bukkit.createBossBar("Enderball", BarColor.GREEN, BarStyle.SOLID);
        bossBar.setVisible(true);
    }

    public void disable() {
        saveState();
        task.cancel();
        bossBar.removeAll();
    }

    public void saveBoard() {
        Json.save(plugin.getBoardFile(), board, true);
    }

    public void saveState() {
        Json.save(plugin.getStateFile(), state, true);
    }

    public World getWorld() {
        return Bukkit.getWorld(board.getWorld());
    }

    public void resetGame() {
        state.getScores().set(0, 0);
        state.getScores().set(1, 0);
        state.getTeams().clear();
        state.setPhase(GamePhase.IDLE);
        removeAllBalls();
    }

    GameBall getBall(Block block) {
        for (GameBall ball : state.getBalls()) {
            if (ball.isBlock() && ball.getBlockVector().isSimilar(block)) return ball;
        }
        return null;
    }

    GameBall getBall(Entity entity) {
        UUID uuid = entity.getUniqueId();
        for (GameBall ball : state.getBalls()) {
            if (uuid.equals(ball.getEntityUuid())) return ball;
        }
        return null;
    }

    GameBall getOrCreateBall(Block block) {
        GameBall gameBall = getBall(block);
        if (gameBall == null) {
            gameBall = new GameBall();
            gameBall.setBlockVector(Vec3i.of(block));
            state.getBalls().add(gameBall);
        }
        return gameBall;
    }

    GameBall getOrCreateBall(Entity entity) {
        GameBall gameBall = getBall(entity);
        if (gameBall == null) {
            gameBall = new GameBall();
            gameBall.setEntityUuid(entity.getUniqueId());
            state.getBalls().add(gameBall);
        }
        return gameBall;
    }

    GameTeam getTeam(Player player) {
        return state.getTeams().get(player.getUniqueId());
    }

    public void onKickBall(Player player, Block block, PlayerInteractEvent event) {
        event.setCancelled(true);
        //if (state.getPhase() != GamePhase.KICKOFF && state.getPhase() != GamePhase.PLAY) return;
        GameTeam team = getTeam(player);
        if (team == null) {
            warpOutside(player);
            return;
        }
        if (state.getPhase() == GamePhase.KICKOFF && state.getKickoffTeam() != team.toIndex()) return;
        GameBall gameBall = getOrCreateBall(block);
        Kick.Strength strength = player.isSprinting() ? Kick.Strength.LONG : Kick.Strength.SHORT;
        Kick.Height height = Kick.Height.of(event.getAction());
        block.setType(Material.AIR, false);
        Location ballLocation = block.getLocation().add(0.5, 0.0, 0.5);
        Vector vec = ballLocation.toVector().subtract(player.getLocation().toVector());
        vec.setY(0.0).normalize().setY(height.height).multiply(strength.strength);
        FallingBlock fallingBlock = block.getWorld().spawnFallingBlock(ballLocation, Material.DRAGON_EGG.createBlockData());
        fallingBlock.setDropItem(true);
        fallingBlock.setVelocity(vec);
        gameBall.setEntityUuid(fallingBlock.getUniqueId());
        gameBall.setLastKicker(player.getUniqueId());
        player.setFoodLevel(Math.max(0, player.getFoodLevel() - 1));
        switch (strength) {
        case SHORT:
            ballLocation.getWorld().playSound(ballLocation, Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.MASTER, 1.0f, 2.0f);
            break;
        case LONG:
            ballLocation.getWorld().playSound(ballLocation, Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.MASTER, 1.0f, 1.66f);
            break;
        default: break;
        }
        if (state.getPhase() == GamePhase.KICKOFF) newPhase(GamePhase.PLAY);
    }

    public void onPlaceBall(Player player, Block block) {
        GameBall gameBall = new GameBall();
        gameBall.setBlockVector(Vec3i.of(block));
        state.getBalls().add(gameBall);
    }

    GameTeam getGoal(Block block) {
        for (int i = 0; i < 2; i += 1) {
            Cuboid cuboid = board.getGoals().get(i);
            if (cuboid.contains(block)) return GameTeam.of(i);
        }
        return null;
    }

    public void onBallLand(FallingBlock entity, Block block, EntityChangeBlockEvent event) {
        GameBall gameBall = getOrCreateBall(entity);
        if (state.getPhase() == GamePhase.PLAY) {
            GameTeam goal = getGoal(block);
            if (goal != null) {
                entity.remove();
                event.setCancelled(true);
                scoreGoal(goal, gameBall);
                return;
            }
            if (!board.getField().contains(entity.getLocation())) {
                entity.remove();
                event.setCancelled(true);
                kickoff(block, gameBall.getLastKicker());
                return;
            }
        }
        gameBall.setEntityUuid(null);
        gameBall.setBlockVector(Vec3i.of(block));
        gameBall.setLastKicker(null);
    }

    public void onBallFall(FallingBlock entity, Block block, EntityChangeBlockEvent event) {
        GameBall gameBall = getOrCreateBall(block);
        gameBall.setEntityUuid(entity.getUniqueId());
    }

    public void onBallBreak(FallingBlock entity, EntityDropItemEvent event) {
        event.setCancelled(true);
        GameBall gameBall = getBall(entity);
        if (gameBall == null) {
            entity.remove();
        }
        removeBall(gameBall);
        if (state.getPhase() == GamePhase.PLAY) {
            GameTeam goal = getGoal(entity.getLocation().getBlock());
            if (goal != null) {
                scoreGoal(goal, gameBall);
                return;
            }
            if (!board.getField().contains(entity.getLocation())) {
                kickoff(entity.getLocation().getBlock(), gameBall.getLastKicker());
                return;
            }
        }
    }

    void kickoff(Block block, UUID lastKicker) {
        removeAllBalls();
        GameTeam team = state.getTeams().get(lastKicker);
        Vec3i kickoffVector;
        if (team == null) {
            kickoffVector = board.getKickoff();
        } else {
            team = team.other();
            kickoffVector = board.getField().clamp(Vec3i.of(block)).withY(board.getField().getMin().y);
        }
        state.setKickoffTeam(team != null ? team.toIndex() : random.nextInt(2));
        Block block2 = kickoffVector.toBlock(getWorld());
        block2.setType(Material.DRAGON_EGG, false);
        GameBall gameBall = new GameBall();
        gameBall.setBlockVector(kickoffVector);
        state.getBalls().add(gameBall);
        newPhase(GamePhase.KICKOFF);
    }

    public void removeAllBalls() {
        for (GameBall gameBall : state.getBalls()) {
            gameBall.remove(getWorld());
        }
        state.getBalls().clear();
    }

    public void removeBall(GameBall gameBall) {
        gameBall.remove(getWorld());
        state.getBalls().remove(gameBall);
    }

    public String getScoreString() {
        return "" + GameTeam.RED.chatColor + state.getScores().get(0)
            + ChatColor.WHITE + " : "
            + GameTeam.BLUE.chatColor + state.getScores().get(1);
    }

    void scoreGoal(GameTeam goal, GameBall gameBall) {
        removeBall(gameBall);
        if (state.getPhase() != GamePhase.PLAY) return;
        GameTeam team = goal.other();
        int index = team.toIndex();
        state.getScores().set(index, state.getScores().get(index) + 1);
        Player player = gameBall.getLastKickerPlayer();
        String title = team.chatColor + "Goal!";
        String subtitle = getScoreString();
        String chat;
        if (player != null) {
            GameTeam playerTeam = getTeam(player);
            chat = player.getName() + " scored a " + (team == playerTeam ? "goal" : "own goal") + " for "
                + team.chatColor + ChatColor.BOLD + team.humanName + "!";
            if (playerTeam == team) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "titles unlockset " + player.getName() + " Fußball");
            }
        } else {
            chat = "Goal for " + team.chatColor + ChatColor.BOLD + team.humanName + "!";
        }
        Title theTitle = new Title(title, subtitle, 0, 20, 0);
        for (Player target : getPresentPlayers()) {
            target.sendMessage(chat);
            target.sendTitle(theTitle);
        }
        plugin.getLogger().info("Goal for " + team + ": " + (player != null ? player.getName() : "null"));
        bossBar.setTitle(chat);
        state.setKickoffTeam(goal.toIndex());
        if (Math.abs(getScore(GameTeam.RED) - getScore(GameTeam.BLUE)) >= 3) {
            newPhase(GamePhase.END);
        } else {
            newPhase(GamePhase.GOAL);
        }
    }

    public List<Player> getPresentPlayers() {
        List<Player> list = new ArrayList<>();
        for (Player player : getWorld().getPlayers()) {
            if (!board.getArea().contains(player.getLocation())) continue;
            if (player.getGameMode() == GameMode.SPECTATOR) continue;
            list.add(player);
        }
        return list;
    }

    public List<Player> getEligiblePlayers() {
        List<Player> list = new ArrayList<>();
        for (Player player : getWorld().getPlayers()) {
            if (!board.getField().contains(player.getLocation())) continue;
            if (player.getGameMode() == GameMode.SPECTATOR) continue;
            list.add(player);
        }
        return list;
    }

    public void warpOutside(Player player) {
        player.teleport(board.getOutside().toLocation(getWorld()), TeleportCause.PLUGIN);
    }

    void makeTeams() {
        List<Player> players = getEligiblePlayers();
        Collections.shuffle(players, random);
        int half = players.size() / 2;
        for (int i = 0; i < half; i += 1) {
            Player player = players.get(i);
            state.getTeams().put(player.getUniqueId(), GameTeam.RED);
        }
        for (int i = half; i < players.size(); i += 1) {
            Player player = players.get(i);
            state.getTeams().put(player.getUniqueId(), GameTeam.BLUE);
        }
        for (Player player : players) {
            GameTeam team = getTeam(player);
            player.sendMessage("You play for team " + team.chatColor + team.humanName);
            plugin.getLogger().info(player.getName() + " plays on team " + team.humanName);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ml add " + player.getName());
        }
        Location spawn = board.getKickoff().toLocation(getWorld());
        for (int i = 0; i < 2; i += 1) {
            GameTeam team = GameTeam.of(i);
            List<Vec3i> spawns = board.getSpawns().get(i).enumerate();
            Collections.shuffle(spawns);
            Iterator<Vec3i> iter = spawns.iterator();
            for (Player player : players) {
                if (getTeam(player) != team) continue;
                Location location = iter.next().toLocation(getWorld());
                Vector lookAt = spawn.toVector().subtract(location.toVector()).normalize();
                location.setDirection(lookAt);
                player.teleport(location, TeleportCause.PLUGIN);
                player.setGameMode(GameMode.SURVIVAL);
                player.setHealth(player.getMaxHealth());
                player.setFoodLevel(20);
                player.setSaturation(20.0f);
            }
        }
    }

    public void newPhase(GamePhase phase) {
        state.setPhase(phase);
        switch (phase) {
        case IDLE:
            break;
        case PLAYERS: {
            makeTeams();
            for (Player player : getPresentPlayers()) {
                dress(player, getTeam(player));
            }
            state.setKickoffTeam(random.nextInt(2));
            state.setGameStarted(System.currentTimeMillis());
            newPhase(GamePhase.KICKOFF);
            break;
        }
        case KICKOFF: {
            GameTeam team = GameTeam.of(state.getKickoffTeam());
            String txt = "Kickoff for " + team.chatColor + team.humanName;
            for (Player player : getPresentPlayers()) {
                player.sendTitle(new Title("", txt, 0, 20, 0));
            }
            state.setKickoffStarted(System.currentTimeMillis());
            bossBar.setTitle(txt);
            if (state.getBalls().isEmpty()) {
                Block block = board.getKickoff().toBlock(getWorld());
                block.setType(Material.DRAGON_EGG, false);
                GameBall gameBall = new GameBall();
                gameBall.setBlockVector(board.getKickoff());
                state.getBalls().add(gameBall);
            }
            break;
        }
        case PLAY: {
            bossBar.setTitle(getScoreString());
            break;
        }
        case GOAL:
            state.setGoalStarted(System.currentTimeMillis());
            break;
        case END: {
            removeAllBalls();
            state.setEndStarted(System.currentTimeMillis());
            int score0 = getScore(GameTeam.RED);
            int score1 = getScore(GameTeam.BLUE);
            GameTeam winnerTeam;
            if (score0 > score1) {
                winnerTeam = GameTeam.RED;
            } else if (score1 > score0) {
                winnerTeam = GameTeam.BLUE;
            } else {
                winnerTeam = null;
            }
            String text;
            String title;
            if (winnerTeam != null) {
                text = "Team " + winnerTeam.chatColor + winnerTeam.humanName + ChatColor.RESET + " wins!";
                title = "" + winnerTeam.chatColor + winnerTeam.humanName;
                plugin.getLogger().info("Winner: " + winnerTeam.humanName);
            } else {
                text = "It's a draw!";
                title = ChatColor.GRAY + "Draw";
                plugin.getLogger().info("Winner: Draw");
            }
            for (Player target : getPresentPlayers()) {
                target.sendMessage(text);
                target.sendTitle(new Title(title, getScoreString(), 0, 20, 0));
            }
            bossBar.setTitle(title);
            break;
        }
        default:
            break;
        }
        saveState();
    }

    void tick() {
        for (Player player : getPresentPlayers()) {
            bossBar.addPlayer(player);
        }
        switch (state.getPhase()) {
        case IDLE: return;
        case KICKOFF: {
            long total = 10000;
            long timeLeft = timeLeft(state.getKickoffStarted(), total);
            if (timeLeft <= 0) {
                newPhase(GamePhase.PLAY);
            } else {
                bossBar.setProgress(clamp1((double) timeLeft / (double) total));
            }
            break;
        }
        case PLAY: {
            long total = 60L * 10L * 1000L;
            long timeLeft = timeLeft(state.getGameStarted(), total);
            if (timeLeft <= 0) {
                newPhase(GamePhase.END);
            } else {
                bossBar.setProgress(clamp1((double) timeLeft / (double) total));
            }
            for (GameBall ball : state.getBalls()) {
                if (ball.isEntity()) {
                    FallingBlock entity = ball.getEntity();
                    if (entity == null) continue;
                    entity.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, entity.getLocation(), 1, 0, 0, 0, 0);
                }
            }
            if (hungerTicks++ % 10 == 0) {
                for (Player player : getPresentPlayers()) {
                    if (getTeam(player) != null) {
                        player.setFoodLevel(Math.min(20, player.getFoodLevel() + 1));
                    }
                }
            }
            for (Player player : getEligiblePlayers()) {
                if (getTeam(player) == null) warpOutside(player);
            }
            break;
        }
        case GOAL: {
            long total = 1000L * 10L;
            long timeLeft = timeLeft(state.getGoalStarted(), total);
            if (timeLeft <= 0) {
                newPhase(GamePhase.KICKOFF);
            } else {
                bossBar.setProgress(clamp1((double) timeLeft / (double) total));
                if ((fireworkTicks++ % 10) == 0) {
                    Cuboid field = board.getField();
                    int x = field.getMin().x + random.nextInt(field.getMax().x - field.getMin().x + 1);
                    int z = field.getMin().z + random.nextInt(field.getMax().z - field.getMin().z + 1);
                    int y = field.getMin().y;
                    Fireworks.spawnFirework(new Vec3i(x, y, z).toLocation(getWorld()));
                }

            }
            break;
        }
        case END: {
            long total = 1000L * 30L;
            long timeLeft = timeLeft(state.getEndStarted(), total);
            if (timeLeft <= 0) {
                resetGame();
                newPhase(GamePhase.PLAYERS);
            } else {
                bossBar.setProgress(clamp1((double) timeLeft / (double) total));
            }
            break;
        }
        default: break;
        }
    }

    void dress(Player player, GameTeam team) {
        player.getInventory().clear();
        if (team == null) return;
        player.getInventory().setHelmet(dye(Material.LEATHER_HELMET, team));
        player.getInventory().setChestplate(dye(Material.LEATHER_CHESTPLATE, team));
        player.getInventory().setLeggings(dye(Material.LEATHER_LEGGINGS, team));
        player.getInventory().setBoots(dye(Material.LEATHER_BOOTS, team));
    }

    ItemStack dye(Material mat, GameTeam team) {
        ItemStack item = new ItemStack(mat);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(team.dyeColor.getColor());
        item.setItemMeta(meta);
        return item;
    }

    public void onJoin(Player player) {
        dress(player, getTeam(player));
    }

    long timeLeft(long other, long total) {
        return Math.max(0L, other + total - System.currentTimeMillis());
    }

    public int getScore(GameTeam team) {
        return state.getScores().get(team.toIndex());
    }

    double clamp1(double in) {
        return Math.max(0, Math.min(1, in));
    }
}
