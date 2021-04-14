package com.cavetale.enderball;

import com.cavetale.enderball.struct.Cuboid;
import com.cavetale.enderball.struct.Vec3i;
import com.cavetale.enderball.util.Fireworks;
import com.cavetale.enderball.util.Gui;
import com.cavetale.enderball.util.Items;
import com.cavetale.enderball.util.Json;
import com.destroystokyo.paper.Title;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
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
    private List<ItemStack> teamFlagItems = new ArrayList<>();
    private final Map<UUID, Nation> nationVotes = new HashMap<>();

    public void enable() {
        board.prep();
        state.prep();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
        bossBar = Bukkit.createBossBar("Enderball", BarColor.GREEN, BarStyle.SOLID);
        bossBar.setVisible(true);
        if (state.getNations().size() == 2) {
            teamFlagItems.clear();
            teamFlagItems.add(state.getNations().get(0).makeTeamFlag(GameTeam.RED));
            teamFlagItems.add(state.getNations().get(1).makeTeamFlag(GameTeam.BLUE));
        }
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
        state.setNations(new ArrayList<>());
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

    GameTeam getTeam(UUID uuid) {
        return state.getTeams().get(uuid);
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
        fallingBlock.setGlowing(true);
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
            // WARNING: Assumes board along z axis!
            if ((team == GameTeam.RED && kickoffVector.z >= board.getField().getMax().z)
                || (team == GameTeam.BLUE && kickoffVector.z <= board.getField().getMin().z)) {
                int dist1 = Math.abs(kickoffVector.x - board.getField().getMin().x);
                int dist2 = Math.abs(kickoffVector.x - board.getField().getMax().x);
                if (dist1 < dist2) {
                    kickoffVector = kickoffVector.withX(board.getField().getMin().x);
                } else {
                    kickoffVector = kickoffVector.withX(board.getField().getMax().x);
                }
            }
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
        return ""
            + GameTeam.RED.chatColor + getTeamName(GameTeam.RED) + " " + state.getScores().get(0)
            + ChatColor.WHITE + " : "
            + GameTeam.BLUE.chatColor + state.getScores().get(1) + " " + getTeamName(GameTeam.BLUE);
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
                + team.chatColor + ChatColor.BOLD + getTeamName(team) + "!";
            if (playerTeam == team) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "titles unlockset " + player.getName() + " Fußball");
            }
        } else {
            chat = "Goal for " + team.chatColor + ChatColor.BOLD + getTeamName(team) + "!";
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
            list.add(player);
        }
        return list;
    }

    boolean isOnField(Player player) {
        if (board.getField().contains(player.getLocation())) return true;
        for (Cuboid goal : board.getGoals()) {
            if (goal.contains(player.getLocation())) return true;
        }
        return false;
    }

    public List<Player> getEligiblePlayers() {
        List<Player> list = new ArrayList<>();
        for (Player player : getWorld().getPlayers()) {
            if (!isOnField(player)) continue;
            if (player.getGameMode() == GameMode.SPECTATOR) continue;
            list.add(player);
        }
        return list;
    }

    public List<Player> getTeamPlayers(GameTeam team) {
        List<Player> list = new ArrayList<>();
        for (Map.Entry<UUID, GameTeam> entry : state.getTeams().entrySet()) {
            if (entry.getValue() != team) continue;
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) list.add(player);
        }
        return list;
    }

    public void warpOutside(Player player) {
        player.teleport(board.getOutside().toLocation(getWorld()), TeleportCause.PLUGIN);
    }

    void makeFlags() {
        List<Nation> nations = new ArrayList<>(Arrays.asList(Nation.values()));
        state.setNations(new ArrayList<>());
        for (GameTeam team : GameTeam.values()) {
            Map<Nation, Integer> nationScores = new EnumMap<>(Nation.class);
            for (Nation nation : nations) {
                nationScores.put(nation, countNationVotes(nation, team));
            }
            Collections.sort(nations, (b, a) -> Integer.compare(nationScores.get(a), nationScores.get(b)));
            // Pick one random nation among the ones with max score.
            Nation nation = nations.get(0);
            final int score = nationScores.get(nation);
            for (int i = 1; i < nations.size(); i += 1) {
                Nation nation2 = nations.get(i);
                int score2 = nationScores.get(nation2);
                if (score != score2) break;
                if (random.nextInt(i + 1) == 0) nation = nation2;
            }
            nations.remove(nation);
            state.getNations().add(nation);
            for (Player player : getTeamPlayers(team)) {
                player.sendMessage(team.chatColor + "Your team flag is " + nation.name);
            }
        }
        teamFlagItems.clear();
        teamFlagItems.add(state.getNations().get(0).makeTeamFlag(GameTeam.RED));
        teamFlagItems.add(state.getNations().get(1).makeTeamFlag(GameTeam.BLUE));
        // WARNING: Assumes board along z axis!
        int centerZ = board.getArea().getCenter().getZ();
        for (Chunk chunk : getWorld().getLoadedChunks()) {
            int x = chunk.getX() << 4;
            int z = chunk.getZ() << 4;
            // WARNING: Assumes board along z axis!
            for (BlockState blockState : chunk.getTileEntities()) {
                Block block = blockState.getBlock();
                if (!board.getArea().contains(block)) continue;
                if (blockState instanceof Banner) {
                    Banner oldBanner = (Banner) blockState;
                    ItemStack itemStack = blockState.getZ() < centerZ
                        ? teamFlagItems.get(0)
                        : teamFlagItems.get(1);
                    DyeColor baseColor = Items.getDyeColor(itemStack.getType());
                    if (baseColor == null) baseColor = DyeColor.BLACK;
                    Material mat;
                    try {
                        mat = block.getType().name().endsWith("WALL_BANNER")
                            ? Material.valueOf(baseColor.name() + "_WALL_BANNER")
                            : Material.valueOf(baseColor.name() + "_BANNER");
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }
                    if (block.getBlockData() instanceof Rotatable) {
                        BlockFace rotation = ((Rotatable) block.getBlockData()).getRotation();
                        Rotatable rotatable = (Rotatable) mat.createBlockData();
                        rotatable.setRotation(rotation);
                        block.setBlockData(rotatable);
                    } else if (block.getBlockData() instanceof Directional) {
                        BlockFace rotation = ((Directional) block.getBlockData()).getFacing();
                        Directional directional = (Directional) mat.createBlockData();
                        directional.setFacing(rotation);
                        block.setBlockData(directional);
                    } else {
                        block.setType(mat);
                    }
                    BannerMeta meta = (BannerMeta) itemStack.getItemMeta();
                    Banner banner = (Banner) block.getState();
                    banner.setPatterns(meta.getPatterns());
                    banner.update();
                }
            }
        }
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
            player.sendMessage("You play for team " + team.chatColor + getTeamName(team));
            plugin.getLogger().info(player.getName() + " plays on team " + getTeamName(team));
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
        setupPhase(phase);
        saveState();
    }

    void setupPhase(GamePhase phase) {
        state.setPhase(phase);
        switch (phase) {
        case IDLE:
            break;
        case TEAMS:
            resetGame();
            makeTeams();
            setupPhase(GamePhase.PICK_FLAG);
            break;
        case PICK_FLAG:
            bossBar.setTitle(ChatColor.GREEN + "Pick a Nation");
            nationVotes.clear();
            state.setPickFlagStarted(System.currentTimeMillis());
            break;
        case PLAYERS: {
            makeFlags();
            for (Player player : getPresentPlayers()) {
                GameTeam team = getTeam(player);
                if (team == null) continue;
                dress(player, team);
            }
            state.setKickoffTeam(random.nextInt(2));
            state.setGameStarted(System.currentTimeMillis());
            setupPhase(GamePhase.KICKOFF);
            break;
        }
        case KICKOFF: {
            GameTeam team = GameTeam.of(state.getKickoffTeam());
            String txt = "Kickoff for " + team.chatColor + getTeamName(team);
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
            for (Player player : getWorld().getPlayers()) {
                if (getTeam(player) != null) {
                    player.getInventory().clear();
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ml add " + player.getName());
                }
            }
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
                text = "Team " + winnerTeam.chatColor + getTeamName(winnerTeam) + ChatColor.RESET + " wins!";
                title = "" + winnerTeam.chatColor + getTeamName(winnerTeam) + " Wins!";
                plugin.getLogger().info("Winner: " + getTeamName(winnerTeam));
            } else {
                text = "It's a draw!";
                title = ChatColor.GRAY + "Draw";
                plugin.getLogger().info("Winner: Draw");
            }
            for (Player target : getPresentPlayers()) {
                target.sendMessage(text);
                target.sendTitle(new Title(title, getScoreString(), 20, 60, 20));
                target.playSound(target.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, SoundCategory.MASTER, 0.5f, 1.5f);
            }
            bossBar.setTitle(title);
            break;
        }
        default:
            break;
        }
    }

    void tick() {
        for (Player player : getPresentPlayers()) {
            bossBar.addPlayer(player);
        }
        switch (state.getPhase()) {
        case IDLE: return;
        case PICK_FLAG: {
            long total = 30000;
            long timeLeft = timeLeft(state.getPickFlagStarted(), total);
            bossBar.setProgress(clamp1((double) timeLeft / (double) total));
            int playerCount = state.getTeams().size();
            int votesCast = nationVotes.size();
            if (timeLeft <= 0 || votesCast >= playerCount) {
                for (Player player : getEligiblePlayers()) {
                    GameTeam team = getTeam(player);
                    if (team == null) continue;
                    player.closeInventory();
                }
                newPhase(GamePhase.PLAYERS);
            } else {
                for (Player player : getEligiblePlayers()) {
                    GameTeam team = getTeam(player);
                    if (team == null) continue;
                    if (nationVotes.get(player.getUniqueId()) == null) {
                        Gui gui = Gui.of(player);
                        if (gui == null) {
                            openNationGui(player, team);
                        }
                    }
                }
            }
            break;
        }
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
                newPhase(GamePhase.PLAYERS);
            } else {
                if ((fireworkTicks++ % 10) == 0) {
                    Cuboid field = board.getField();
                    int x = field.getMin().x + random.nextInt(field.getMax().x - field.getMin().x + 1);
                    int z = field.getMin().z + random.nextInt(field.getMax().z - field.getMin().z + 1);
                    int y = field.getMin().y;
                    Fireworks.spawnFirework(new Vec3i(x, y, z).toLocation(getWorld()));
                }
                bossBar.setProgress(clamp1((double) timeLeft / (double) total));
            }
            break;
        }
        default: break;
        }
    }

    Gui openNationGui(Player player, GameTeam team) {
        int rows = (Nation.values().length - 1) / 9 + 1;
        int size = rows * 9;
        Gui gui = new Gui(plugin).size(size).title("Nation Vote");
        updateNationGui(player, gui, team);
        gui.open(player);
        return gui;
    }

    int countNationVotes(Nation nation, GameTeam team) {
        if (nationVotes == null) return 0;
        int result = 0;
        for (Map.Entry<UUID, Nation> entry : nationVotes.entrySet()) {
            if (entry.getValue() == nation && getTeam(entry.getKey()) == team) {
                result += 1;
            }
        }
        return result;
    }

    void updateNationGui(Player player) {
        Gui gui = Gui.of(player);
        if (gui == null) return;
        GameTeam team = getTeam(player);
        if (team == null) return;
        updateNationGui(player, gui, team);
    }

    void updateNationGui(Player player, Gui gui, GameTeam team) {
        int i = 0;
        for (Nation nation : Nation.values()) {
            int slot = i++;
            int votes = countNationVotes(nation, team);
            ItemStack item = nation.makeTeamFlag(team);
            item.setAmount(1 + votes);
            gui.setItem(slot, item, click -> {
                    if (state.getPhase() != GamePhase.PICK_FLAG) return;
                    nationVotes.put(player.getUniqueId(), nation);
                    for (Player target : getEligiblePlayers()) {
                        updateNationGui(target);
                    }
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1.0f, 1.0f);
                });
        }
    }

    void dress(Player player, GameTeam team) {
        player.getInventory().clear();
        if (team == null) return;
        //player.getInventory().setHelmet(dye(Material.LEATHER_HELMET, team));
        player.getInventory().setHelmet(teamFlagItems.get(team.ordinal()).clone());
        player.getInventory().setChestplate(dye(Material.LEATHER_CHESTPLATE, team));
        player.getInventory().setLeggings(dye(Material.LEATHER_LEGGINGS, team));
        player.getInventory().setBoots(dye(Material.LEATHER_BOOTS, team));
    }

    ItemStack dye(Material mat, GameTeam team) {
        ItemStack item = new ItemStack(mat);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(team.dyeColor.getColor());
        meta.setDisplayName(team.chatColor + getTeamName(team));
        meta.addItemFlags(ItemFlag.values());
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

    public String getTeamName(GameTeam team) {
        if (state.getNations().size() != 2) return team.humanName;
        return state.getNations().get(team.ordinal()).name;
    }
}
