package com.cavetale.enderball;

import com.cavetale.afk.AFKPlugin;
import com.cavetale.core.event.minigame.MinigameFlag;
import com.cavetale.core.event.minigame.MinigameMatchCompleteEvent;
import com.cavetale.core.event.minigame.MinigameMatchType;
import com.cavetale.core.font.GuiOverlay;
import com.cavetale.core.font.VanillaEffects;
import com.cavetale.core.money.Money;
import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.enderball.util.Fireworks;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.item.WardrobeItem;
import com.cavetale.mytems.util.Gui;
import com.winthier.creative.BuildWorld;
import com.winthier.creative.file.Files;
import com.winthier.creative.review.MapReview;
import com.winthier.title.TitlePlugin;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
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
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import static com.cavetale.core.font.Unicode.tiny;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.textOfChildren;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;
import static net.kyori.adventure.title.Title.Times.times;

/**
 * Runtime class of one game.
 */
@RequiredArgsConstructor @Getter
public final class Game {
    public static final long GAME_TIME = 60L * 15L * 1000L;
    private final EnderballPlugin plugin;
    private final BuildWorld buildWorld;
    private final World world;
    private GameBoard board = new GameBoard();
    private GameState state = new GameState();
    private Random random = new Random();
    private BossBar bossBar;
    private int hungerTicks = 0;
    private int fireworkTicks = 0;
    private final Map<UUID, Nation> nationVotes = new HashMap<>();
    private boolean obsolete;
    @Setter private boolean skip;
    public static final List<String> TITLES = List.of("Fußball", "Striker", "Goal", "Soccer", "EnderballChampion");

    public static Game in(World world) {
        for (Game game : EnderballPlugin.getInstance().getGames()) {
            if (world.equals(game.getWorld())) return game;
        }
        return null;
    }

    public static void ifIn(World world, Consumer<Game> callback) {
        final Game game = in(world);
        if (game != null) callback.accept(game);
    }

    public void enable() {
        board.load(world);
        bossBar = BossBar.bossBar(text("Enderball"), 1.0f,
                                  BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
        setupPhase(GamePhase.WAIT_FOR_PLAYERS);
    }

    public void bringPlayersFromLobby() {
        for (Player player : plugin.getLobby().getPlayers()) {
            player.eject();
            player.leaveVehicle();
            if (AFKPlugin.isAfk(player)) {
                player.teleport(getViewerLocation());
            } else {
                player.teleport(board.getKickoff().toCenterFloorLocation(world));
            }
            player.setGameMode(GameMode.ADVENTURE);
            player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
            player.setFoodLevel(20);
            player.setSaturation(20.0f);
            player.setExhaustion(0f);
        }
    }

    public void disable() {
        for (Player player : world.getPlayers()) {
            plugin.getLobby().warp(player);
        }
        Files.deleteWorld(world);
    }

    public String getName() {
        return buildWorld.getPath();
    }

    public void resetGame() {
        state.getScores().set(0, 0);
        state.getScores().set(1, 0);
        state.getTeams().clear();
        state.setNations(new ArrayList<>());
        removeAllBalls();
        for (Player player : getPresentPlayers()) {
            clearInventory(player);
            TitlePlugin.getInstance().setColor(player, null);
        }
    }

    private GameBall getBall(Block block) {
        for (GameBall ball : state.getBalls()) {
            if (ball.isBlock() && ball.getBlockVector().isSimilar(block)) return ball;
        }
        return null;
    }

    private GameBall getBall(Entity entity) {
        UUID uuid = entity.getUniqueId();
        for (GameBall ball : state.getBalls()) {
            if (uuid.equals(ball.getEntityUuid())) return ball;
        }
        return null;
    }

    private GameBall getOrCreateBall(Block block) {
        GameBall gameBall = getBall(block);
        if (gameBall == null) {
            gameBall = new GameBall();
            gameBall.setBlockVector(Vec3i.of(block));
            state.getBalls().add(gameBall);
        }
        return gameBall;
    }

    private GameBall getOrCreateBall(Entity entity) {
        GameBall gameBall = getBall(entity);
        if (gameBall == null) {
            gameBall = new GameBall();
            gameBall.setEntityUuid(entity.getUniqueId());
            state.getBalls().add(gameBall);
        }
        return gameBall;
    }

    private GameTeam getTeam(Player player) {
        return state.getTeams().get(player.getUniqueId());
    }

    private GameTeam getTeam(UUID uuid) {
        return state.getTeams().get(uuid);
    }

    public void onKickBall(Player player, Block block, PlayerInteractEvent event) {
        event.setCancelled(true);
        if (state.getPhase() != GamePhase.KICKOFF && state.getPhase() != GamePhase.PLAY) return;
        GameTeam team = getTeam(player);
        if (team == null) {
            warpOutside(player);
            return;
        }
        if (state.getPhase() == GamePhase.KICKOFF && state.getKickoffTeam() != team.toIndex()) return;
        GameBall ball = getOrCreateBall(block);
        Kick.Strength strength = player.isSprinting() ? Kick.Strength.LONG : Kick.Strength.SHORT;
        Kick.Height height = Kick.Height.of(event.getAction());
        Vector vector = player.getLocation().getDirection();
        double rnd = (0.25 * (random.nextDouble() - random.nextDouble())) + 1.0;
        vector.setY(0.0).normalize().setY(height.height).multiply(strength.strength * rnd);
        Kick kick = new Kick(player, ball, strength, height, vector);
        if (ball.getKickCooldown() > System.currentTimeMillis()) {
            ball.getKicks().put(player.getUniqueId(), kick);
        } else {
            kick(kick);
        }
    }

    /**
     * Perform a kick.  This could be delayed due to the kick
     * cooldown.
     */
    private void kick(Kick kick) {
        if (!kick.ball.isBlock()) return;
        Player player = kick.player;
        Block block = kick.ball.getBlock(world);
        block.setType(Material.AIR, false);
        Location ballLocation = block.getLocation().add(0.5, 0.0, 0.5);
        FallingBlock fallingBlock = ballLocation.getWorld().spawn(ballLocation, FallingBlock.class, e -> e.setBlockData(Material.DRAGON_EGG.createBlockData()));
        fallingBlock.setDropItem(true);
        fallingBlock.setVelocity(kick.vector);
        fallingBlock.setGlowing(true);
        kick.ball.setEntityUuid(fallingBlock.getUniqueId());
        kick.ball.setLastKicker(player.getUniqueId());
        switch (kick.strength) {
        case SHORT: {
            ballLocation.getWorld().playSound(ballLocation, Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.MASTER, 1.0f, 2.0f);
            final int food = Math.max(0, player.getFoodLevel() - 5);
            player.setFoodLevel(food);
            player.setSaturation((float) food);
            break;
        }
        case LONG: {
            ballLocation.getWorld().playSound(ballLocation, Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.MASTER, 1.0f, 1.66f);
            final int food = Math.max(0, player.getFoodLevel() - 10);
            player.setFoodLevel(food);
            player.setSaturation((float) food);
            break;
        }
        default: break;
        }
        if (state.getPhase() == GamePhase.KICKOFF) newPhase(GamePhase.PLAY);
        state.addBallContact(player.getUniqueId());
        if (plugin.getSave().isEvent()) {
            plugin.getSave().addScore(player.getUniqueId(), 1);
            plugin.computeHighscore();
        }
    }

    public void onPlaceBall(Player player, Block block) {
        GameBall gameBall = new GameBall();
        gameBall.setBlockVector(Vec3i.of(block));
        state.getBalls().add(gameBall);
    }

    private GameTeam getGoal(Vec3i vec) {
        for (int i = 0; i < 2; i += 1) {
            Cuboid cuboid = board.getGoals().get(i);
            if (cuboid.contains(vec)) return GameTeam.of(i);
        }
        return null;
    }

    private boolean ballZoneAction(GameBall gameBall) {
        GameTeam goal = getGoal(gameBall.getBlockVector());
        if (goal != null) {
            scoreGoal(goal, gameBall); // calls removeAllBalls()
            return true;
        }
        if (gameBall.isEntity() && gameBall.getEntity() != null) {
            BoundingBox bb = gameBall.getEntity().getBoundingBox();
            if (!board.getField().contains(bb)
                && board.getGoals().get(0).contains(bb)
                && board.getGoals().get(1).contains(bb)) {
                kickoff(gameBall.getBlockVector(), gameBall.getLastKicker()); // calls removeAllBalls()
                return true;
            }
        }
        if (!board.getField().contains(gameBall.getBlockVector())) {
            kickoff(gameBall.getBlockVector(), gameBall.getLastKicker()); // calls removeAllBalls()
            return true;
        }
        return false;
    }

    public void onBallLand(FallingBlock entity, Block block, EntityChangeBlockEvent event) {
        GameBall gameBall = getOrCreateBall(entity);
        gameBall.setBlockVector(Vec3i.of(block));
        if (state.getPhase() == GamePhase.PLAY) {
            if (ballZoneAction(gameBall)) {
                event.setCancelled(true);
                return;
            }
        }
        gameBall.setEntityUuid(null);
        gameBall.setLastKicker(null);
        gameBall.getKicks().clear();
        gameBall.setKickCooldown(System.currentTimeMillis() + 500L);
    }

    public void onBallFall(FallingBlock entity, Block block, EntityChangeBlockEvent event) {
        GameBall gameBall = getOrCreateBall(block);
        gameBall.setEntityUuid(entity.getUniqueId());
        gameBall.getKicks().clear();
        gameBall.setKickCooldown(0);
    }

    public void onBallBreak(FallingBlock entity, EntityDropItemEvent event) {
        event.setCancelled(true);
        GameBall gameBall = getBall(entity);
        if (gameBall == null) {
            entity.remove();
        }
        if (state.getPhase() == GamePhase.PLAY) {
            if (!ballZoneAction(gameBall)) {
                removeBall(gameBall);
            }
        }
    }

    private void kickoff(Vec3i vec, UUID lastKicker) {
        removeAllBalls();
        GameTeam team = state.getTeams().get(lastKicker);
        Vec3i kickoffVector;
        if (team == null) {
            kickoffVector = board.getKickoff();
        } else {
            team = team.other();
            kickoffVector = board.getField().clamp(vec).withY(board.getField().getMin().y);
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
        Block block = kickoffVector.toBlock(world);
        block.setType(Material.DRAGON_EGG, false);
        GameBall gameBall = new GameBall();
        gameBall.setBlockVector(kickoffVector);
        state.getBalls().add(gameBall);
        newPhase(GamePhase.KICKOFF);
    }

    public void removeAllBalls() {
        for (GameBall gameBall : state.getBalls()) {
            gameBall.remove(world);
        }
        state.getBalls().clear();
    }

    public void removeBall(GameBall gameBall) {
        gameBall.remove(world);
        state.getBalls().remove(gameBall);
    }

    public Component getScoreComponent() {
        Nation a = getTeamNation(GameTeam.RED);
        Nation b = getTeamNation(GameTeam.BLUE);
        TextColor white = WHITE;
        return textOfChildren(text(a.name, GameTeam.RED.textColor),
                              (empty().equals(a.component)
                               ? GameTeam.RED.chatBlock
                               : a.component),
                              space(),
                              text("" + state.getScores().get(0), GameTeam.RED.textColor, BOLD),
                              text(" : ", WHITE),
                              text("" + state.getScores().get(1), GameTeam.BLUE.textColor, BOLD),
                              space(),
                              (empty().equals(b.component)
                               ? GameTeam.BLUE.chatBlock
                               : b.component),
                              text(b.name, GameTeam.BLUE.textColor));
    }

    private void scoreGoal(GameTeam goal, GameBall gameBall) {
        removeAllBalls();
        if (state.getPhase() != GamePhase.PLAY) return;
        GameTeam team = goal.other();
        int index = team.toIndex();
        state.getScores().set(index, state.getScores().get(index) + 1);
        Player player = gameBall.getLastKickerPlayer();
        Component title = text("Goal!", team.textColor);
        Component subtitle = getScoreComponent();
        Component chat;
        if (player != null) {
            GameTeam playerTeam = getTeam(player);
            chat = textOfChildren(text(player.getName() + " scored " + (team == playerTeam ? "a goal" : "an own goal") + " for ", WHITE),
                                  text(getTeamName(team) + "!", team.textColor, BOLD));
            if (playerTeam == team && plugin.getSave().isEvent() && !plugin.getSave().isTesting()) {
                String cmd = "titles unlockset " + player.getName() + " " + String.join(" ", TITLES);
                plugin.getLogger().info("Running command: " + cmd);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
        } else {
            chat = textOfChildren(text("Goal for ", WHITE),
                                  text(getTeamName(team) + "!", team.textColor, BOLD));
        }
        if (plugin.getSave().isEvent()) {
            if (gameBall.getLastKicker() != null && getTeam(gameBall.getLastKicker()) == team) {
                plugin.getSave().addScore(gameBall.getLastKicker(), 10);
                plugin.getSave().addGoals(gameBall.getLastKicker(), 1);
                Money.get().give(gameBall.getLastKicker(), 1000.0, plugin, "Enderball Goal");
                if (gameBall.getAssistance() != null
                    && getTeam(gameBall.getAssistance()) == team
                    && !gameBall.getAssistance().equals(gameBall.getLastKicker())) {
                    plugin.getSave().addScore(gameBall.getAssistance(), 5);
                    plugin.getSave().addAssists(gameBall.getLastKicker(), 1);
                    Money.get().give(gameBall.getAssistance(), 500, plugin, "Enderball Goal Assist");
                }
            }
            if (gameBall.getLastKicker() != null && getTeam(gameBall.getLastKicker()) != team) {
                // Own goal penalty
                plugin.getSave().addScore(gameBall.getLastKicker(), -10);
            }
            plugin.computeHighscore();
        }
        Title theTitle = Title.title(title, subtitle, times(Duration.ZERO, Duration.ofSeconds(3), Duration.ZERO));
        for (Player target : getPresentPlayers()) {
            target.sendMessage(chat);
            target.showTitle(theTitle);
        }
        plugin.getLogger().info("Goal for " + team + ": " + (player != null ? player.getName() : "null"));
        bossBar.name(chat);
        state.setKickoffTeam(goal.toIndex());
        if (Math.abs(getScore(GameTeam.RED) - getScore(GameTeam.BLUE)) >= 3) {
            newPhase(GamePhase.END);
        } else {
            newPhase(GamePhase.GOAL);
        }
    }

    public List<Player> getPresentPlayers() {
        return world.getPlayers();
    }

    public boolean isOnField(Player player) {
        if (board.getField().contains(player.getLocation())) return true;
        for (Cuboid goal : board.getGoals()) {
            if (goal.contains(player.getLocation())) return true;
        }
        return false;
    }

    public List<Player> getEligiblePlayers() {
        List<Player> list = new ArrayList<>();
        for (Player player : world.getPlayers()) {
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

    public Location getViewerLocation() {
        final Set<Vec3i> set = new HashSet<>();
        for (Cuboid c : board.getViewers()) {
            set.addAll(c.enumerate());
        }
        final List<Vec3i> list = new ArrayList<>(set);
        Collections.shuffle(list);
        for (Vec3i it : list) {
            if (!it.toBlock(world).isEmpty()) continue;
            final Location result = list.get(random.nextInt(list.size())).toCenterFloorLocation(world);
            final Location lookAt = board.getKickoff().toCenterLocation(world);
            result.setDirection(lookAt.toVector().subtract(result.toVector()));
            return result;
        }
        throw new IllegalStateException("No viewer location found: " + buildWorld.getPath());
    }

    public void warpOutside(Player player) {
        player.eject();
        player.leaveVehicle();
        player.teleport(getViewerLocation(), TeleportCause.PLUGIN);
    }

    public void teleportPlayersStartLocations(List<Player> players) {
        final Location spawn = board.getKickoff().toLocation(world);
        for (int i = 0; i < 2; i += 1) {
            final GameTeam team = GameTeam.of(i);
            final List<Vec3i> spawns = board.getSpawns().get(i).enumerate();
            Collections.shuffle(spawns);
            // Bug: We assume there are enough spawns for all players!
            final Iterator<Vec3i> iter = spawns.iterator();
            for (Player player : players) {
                if (getTeam(player) != team) continue;
                final Location location = iter.next().toLocation(world);
                final Vector lookAt = spawn.toVector().subtract(location.toVector()).normalize();
                location.setDirection(lookAt);
                player.teleport(location, TeleportCause.PLUGIN);
                player.setGameMode(GameMode.SURVIVAL);
                player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
                player.setFoodLevel(20);
                player.setSaturation(20.0f);
                player.setExhaustion(0f);
            }
        }
    }

    private void makeFlags() {
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
                player.sendMessage(textOfChildren(text("Your team flag is ", team.textColor),
                                                  nation.component,
                                                  text(nation.name, team.textColor)));
                player.showTitle(Title.title(empty(),
                                             text(nation.name, team.textColor)));
            }
        }
        // WARNING: Assumes board along z axis!
        int centerZ = board.getArea().getCenter().getZ();
        for (Chunk chunk : world.getLoadedChunks()) {
            int x = chunk.getX() << 4;
            int z = chunk.getZ() << 4;
            // WARNING: Assumes board along z axis!
            for (BlockState blockState : chunk.getTileEntities()) {
                Block block = blockState.getBlock();
                if (!board.getArea().contains(block)) continue;
                if (blockState instanceof Banner) {
                    Banner oldBanner = (Banner) blockState;
                    int teamIndex = blockState.getZ() < centerZ ? 0 : 1;
                    Nation nation = state.getNations().get(teamIndex);
                    DyeColor baseColor = nation.bannerDyeColor;
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
                    Banner banner = (Banner) block.getState();
                    banner.setPatterns(nation.bannerPatterns);
                    banner.update();
                }
            }
        }
    }

    private void makeTeams() {
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
            player.sendMessage(textOfChildren(text("You play for team ", WHITE),
                                              text(getTeamName(team), team.textColor)));
            plugin.getLogger().info(player.getName() + " plays on team " + getTeamName(team));
        }
        teleportPlayersStartLocations(players);
    }

    public void newPhase(GamePhase phase) {
        setupPhase(phase);
    }

    private void setupPhase(GamePhase phase) {
        state.setPhase(phase);
        switch (phase) {
        case IDLE:
            bossBar.name(text("Paused", GRAY));
            bossBar.progress(1.0f);
            resetGame();
            break;
        case WAIT_FOR_PLAYERS:
            removeAllBalls();
            bossBar.name(text("Preparing the Game", GRAY));
            state.setWaitForPlayersStarted(System.currentTimeMillis());
            break;
        case TEAMS:
            makeTeams();
            setupPhase(GamePhase.PICK_FLAG);
            break;
        case PICK_FLAG:
            bossBar.name(text("Pick a Nation", GREEN));
            nationVotes.clear();
            state.setPickFlagStarted(System.currentTimeMillis());
            break;
        case PLAYERS: {
            makeFlags();
            for (Player player : getPresentPlayers()) {
                GameTeam team = getTeam(player);
                if (team == null) continue;
                clearInventory(player);
                dress(player, team);
                if (plugin.getSave().isEvent() && !plugin.getSave().isTesting()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ml add " + player.getName());
                }
            }
            state.setKickoffTeam(random.nextInt(2));
            state.setGameStarted(System.currentTimeMillis());
            setupPhase(GamePhase.KICKOFF);
            break;
        }
        case KICKOFF: {
            GameTeam team = GameTeam.of(state.getKickoffTeam());
            Nation nation = getTeamNation(team);
            Component txt = textOfChildren(text("Kickoff for ", WHITE),
                                           nation.component,
                                           text(nation.name, team.textColor));
            Title title = Title.title(empty(), txt,
                                      times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO));
            for (Player player : getPresentPlayers()) {
                player.showTitle(title);
            }
            state.setKickoffStarted(System.currentTimeMillis());
            bossBar.name(txt);
            if (state.getBalls().isEmpty()) {
                Block block = board.getKickoff().toBlock(world);
                block.setType(Material.DRAGON_EGG, false);
                GameBall gameBall = new GameBall();
                gameBall.setBlockVector(board.getKickoff());
                state.getBalls().add(gameBall);
            }
            break;
        }
        case PLAY: {
            bossBar.name(getScoreComponent());
            break;
        }
        case GOAL:
            state.setGoalStarted(System.currentTimeMillis());
            break;
        case END: {
            for (Player player : world.getPlayers()) {
                if (getTeam(player) != null) {
                    clearInventory(player);
                    TitlePlugin.getInstance().setColor(player, null);
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
            Component text;
            Component title;
            if (winnerTeam != null) {
                if (plugin.getSave().isEvent()) {
                    for (Player player : getTeamPlayers(winnerTeam)) {
                        Money.get().give(player.getUniqueId(), 1000.0, plugin, "Enderball Victory");
                        plugin.getSave().addScore(player.getUniqueId(), 3);
                    }
                    plugin.computeHighscore();
                }
                text = textOfChildren(text("Team ", WHITE),
                                      text(getTeamName(winnerTeam), winnerTeam.textColor),
                                      text(" wins!", WHITE));
                Nation nation = getTeamNation(winnerTeam);
                title = textOfChildren(nation.component,
                                       text(nation.name, winnerTeam.textColor),
                                       text(" Wins!", WHITE));
                plugin.getLogger().info("Winner: " + getTeamName(winnerTeam));
            } else {
                text = text("It's a draw!", WHITE);
                title = text("Draw", GRAY);
                plugin.getLogger().info("Winner: Draw");
            }
            Title theTitle = Title.title(title, getScoreComponent(),
                                         times(Duration.ofSeconds(1), Duration.ofSeconds(3), Duration.ofSeconds(1)));
            for (Player target : getPresentPlayers()) {
                target.sendMessage(text);
                target.showTitle(theTitle);
                target.playSound(target.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, SoundCategory.MASTER, 0.5f, 1.5f);
            }
            bossBar.name(title);
            final MinigameMatchCompleteEvent event = new MinigameMatchCompleteEvent(MinigameMatchType.ENDERBALL);
            for (UUID uuid : state.getBallContacts().keySet()) {
                final GameTeam team = getTeam(uuid);
                if (team == null) continue;
                event.addPlayerUuid(uuid);
                if (team == winnerTeam) {
                    event.addWinnerUuid(uuid);
                }
                if (plugin.getSave().isEvent()) {
                    event.addFlags(MinigameFlag.EVENT);
                }
            }
            event.callEvent();
            MapReview.start(world, buildWorld).remindAll();
            break;
        }
        default:
            break;
        }
    }

    public void tick() {
        if (state.getPhase().isPlaying()) {
            if (hungerTicks++ % 20 == 0) {
                for (Player player : getPresentPlayers()) {
                    if (getTeam(player) == null) continue;
                    final int food = player.isSprinting()
                        ? Math.max(0, player.getFoodLevel() - 1)
                        : Math.min(20, player.getFoodLevel() + 2);
                    player.setFoodLevel(food);
                    player.setSaturation((float) food);
                }
            }
        }
        switch (state.getPhase()) {
        case IDLE: return;
        case WAIT_FOR_PLAYERS: {
            List<Player> players = getEligiblePlayers();
            final long total = 20_000;
            final long timeLeft = timeLeft(state.getWaitForPlayersStarted(), total);
            if (timeLeft <= 0 || skip) {
                if (players.isEmpty()) {
                    obsolete = true;
                    return;
                }
                skip = false;
                if (state.isManual()) {
                    newPhase(GamePhase.KICKOFF);
                } else {
                    newPhase(GamePhase.TEAMS);
                }
            } else {
                bossBar.progress(clamp1(1f - (float) timeLeft / (float) total));
            }
            break;
        }
        case PICK_FLAG: {
            long total = 60_000L;
            long timeLeft = timeLeft(state.getPickFlagStarted(), total);
            bossBar.progress(clamp1((float) timeLeft / (float) total));
            int playerCount = state.getTeams().size();
            int votesCast = nationVotes.size();
            if (timeLeft <= 0 || votesCast >= playerCount || skip) {
                skip = false;
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
            long total = 30_000;
            long timeLeft = timeLeft(state.getKickoffStarted(), total);
            if (timeLeft <= 0 || skip) {
                skip = false;
                newPhase(GamePhase.PLAY);
            } else {
                bossBar.progress(clamp1((float) timeLeft / (float) total));
            }
            break;
        }
        case PLAY: {
            for (GameBall gameBall : new ArrayList<>(state.getBalls())) {
                if (!gameBall.getKicks().isEmpty() && gameBall.getKickCooldown() <= System.currentTimeMillis()) {
                    List<UUID> uuids = new ArrayList<>(gameBall.getKicks().keySet());
                    uuids.removeIf(u -> Bukkit.getPlayer(u) == null || getTeam(u) == null);
                    if (!uuids.isEmpty()) {
                        UUID uuid = uuids.get(random.nextInt(uuids.size()));
                        Kick kick = gameBall.getKicks().get(uuid);
                        kick(kick);
                    }
                    gameBall.getKicks().clear();
                    gameBall.setKickCooldown(0L);
                    continue;
                }
                FallingBlock fallingBlock = gameBall.getEntity();
                if (fallingBlock != null) {
                    gameBall.setBlockVector(Vec3i.of(fallingBlock.getLocation()));
                }
                if (ballZoneAction(gameBall)) return;
            }
            long total = GAME_TIME;
            long timeLeft = timeLeft(state.getGameStarted(), total);
            if (timeLeft <= 0) {
                newPhase(GamePhase.END);
            } else {
                bossBar.progress(clamp1((float) timeLeft / (float) total));
            }
            for (GameBall ball : state.getBalls()) {
                if (ball.isEntity()) {
                    FallingBlock entity = ball.getEntity();
                    if (entity == null) continue;
                    entity.getWorld().spawnParticle(Particle.FIREWORK, entity.getLocation(), 1, 0, 0, 0, 0);
                }
            }
            for (UUID uuid : List.copyOf(state.getTeams().keySet())) {
                if (Bukkit.getPlayer(uuid) == null) {
                    state.getTeams().remove(uuid);
                }
            }
            for (Player player : getEligiblePlayers()) {
                if (getTeam(player) == null) {
                    // Late Joining
                    GameTeam team = countTeamSize(GameTeam.RED) < countTeamSize(GameTeam.BLUE)
                        ? GameTeam.RED
                        : GameTeam.BLUE;
                    state.getTeams().put(player.getUniqueId(), team);
                    player.setGameMode(GameMode.SURVIVAL);
                    player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
                    player.setFoodLevel(20);
                    player.setSaturation(20.0f);
                    dress(player, team);
                    if (plugin.getSave().isEvent() && !plugin.getSave().isTesting()) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ml add " + player.getName());
                    }
                    Nation nation = getTeamNation(team);
                    player.sendMessage(textOfChildren(text("Welcome to ", WHITE),
                                                      text("Team ", team.textColor),
                                                      nation.component,
                                                      text(nation.name, team.textColor)));
                    for (Player other : getPresentPlayers()) {
                        other.sendMessage(textOfChildren(text(player.getName() + " joined ", team.textColor),
                                                         nation.component,
                                                         text(nation.name, team.textColor)));
                    }
                }
            }
            break;
        }
        case GOAL: {
            long total = 20_000L;
            long timeLeft = timeLeft(state.getGoalStarted(), total);
            if (timeLeft <= 0) {
                newPhase(GamePhase.KICKOFF);
                teleportPlayersStartLocations(getEligiblePlayers());
            } else {
                bossBar.progress(clamp1((float) timeLeft / (float) total));
                if ((fireworkTicks++ % 10) == 0) {
                    Cuboid field = board.getField();
                    int x = field.getMin().x + random.nextInt(field.getMax().x - field.getMin().x + 1);
                    int z = field.getMin().z + random.nextInt(field.getMax().z - field.getMin().z + 1);
                    int y = field.getMin().y;
                    Fireworks.spawnFirework(new Vec3i(x, y, z).toLocation(world));
                }
            }
            break;
        }
        case END: {
            long total = 60_000L;
            long timeLeft = timeLeft(state.getEndStarted(), total);
            if (timeLeft <= 0) {
                obsolete = true;
            } else {
                if ((fireworkTicks++ % 10) == 0) {
                    Cuboid field = board.getField();
                    int x = field.getMin().x + random.nextInt(field.getMax().x - field.getMin().x + 1);
                    int z = field.getMin().z + random.nextInt(field.getMax().z - field.getMin().z + 1);
                    int y = field.getMin().y;
                    Fireworks.spawnFirework(new Vec3i(x, y, z).toLocation(world));
                }
                bossBar.progress(clamp1((float) timeLeft / (float) total));
            }
            break;
        }
        default: break;
        }
    }

    private Gui openNationGui(Player player, GameTeam team) {
        int nationCount = 0;
        for (Nation nation : Nation.values()) {
            nationCount += 1;
        }
        int rows = (nationCount - 1) / 9 + 1;
        int size = rows * 9;
        Nation nation = nationVotes.get(player.getUniqueId());
        GuiOverlay.Builder builder = GuiOverlay.BLANK.builder(size, BLACK);
        if (nation != null) {
            builder.title(textOfChildren(text("Nation Vote ", WHITE), nation.component, text(nation.name)));
        } else {
            builder.title(text("Nation Vote", WHITE));
        }
        Gui gui = new Gui(plugin)
            .size(size)
            .title(builder.build());
        updateNationGui(player, gui, team);
        gui.open(player);
        return gui;
    }

    private int countNationVotes(Nation nation, GameTeam team) {
        if (nationVotes == null) return 0;
        int result = 0;
        for (Map.Entry<UUID, Nation> entry : nationVotes.entrySet()) {
            if (entry.getValue() == nation && getTeam(entry.getKey()) == team) {
                result += 1;
            }
        }
        return result;
    }

    private void updateNationGui(Player player, Gui gui, GameTeam team) {
        List<Nation> nations = new ArrayList<>();
        for (Nation nation : Nation.values()) {
            nations.add(nation);
        }
        nations.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        int nextSlot = 0;
        for (Nation nation : nations) {
            int votes = countNationVotes(nation, team);
            ItemStack item = nation.mytems != null
                ? nation.mytems.createIcon(List.of(text(nation.name, WHITE)))
                : nation.bannerItem.clone();
            item.setAmount(Math.max(1, Math.min(64, 1 + votes)));
            gui.setItem(nextSlot++, item, click -> {
                    if (state.getPhase() != GamePhase.PICK_FLAG) return;
                    nationVotes.put(player.getUniqueId(), nation);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                            for (Player target : getTeamPlayers(team)) {
                                Gui gui2 = Gui.of(target);
                                if (gui2 == null) return;
                                openNationGui(target, team);
                            }
                        });
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1.0f, 1.0f);
                });
        }
    }

    public void dress(Player player, GameTeam team) {
        clearInventory(player);
        if (team == null) return;
        player.getInventory().setChestplate(dye(Material.LEATHER_CHESTPLATE, team));
        player.getInventory().setLeggings(dye(Material.LEATHER_LEGGINGS, team));
        player.getInventory().setBoots(dye(Material.LEATHER_BOOTS, team));
        player.getInventory().setItemInOffHand(Mytems.MAGIC_MAP.createItemStack());
        TitlePlugin.getInstance().setColor(player, team.textColor);
    }

    private ItemStack dye(Material mat, GameTeam team) {
        ItemStack item = new ItemStack(mat);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(team.dyeColor.getColor());
        meta.displayName(text(getTeamName(team), team.textColor));
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }

    public void onJoin(Player player) {
        dress(player, getTeam(player));
    }

    private long timeLeft(long other, long total) {
        return Math.max(0L, other + total - System.currentTimeMillis());
    }

    public int getScore(GameTeam team) {
        return state.getScores().get(team.toIndex());
    }

    public int countTeamSize(GameTeam team) {
        int result = 0;
        for (GameTeam it : state.getTeams().values()) {
            if (it == team) result += 1;
        }
        return result;
    }

    private static float clamp1(float in) {
        return Math.max(0, Math.min(1, in));
    }

    public Nation getTeamNation(GameTeam team) {
        return state.getNations().get(team.ordinal());
    }

    public String getTeamName(GameTeam team) {
        if (state.getNations().size() != 2) return team.humanName;
        return state.getNations().get(team.ordinal()).name;
    }

    public void onHeaderBall(Player player, FallingBlock fallingBlock) {
        Vector velocity = fallingBlock.getVelocity();
        if (velocity.getY() >= 0) return; // Rising ball cannot be blocked!
        if (Math.abs(velocity.getX()) < 0.01 && Math.abs(velocity.getZ()) < 0.01) return; // Already blocked!
        GameTeam team = getTeam(player);
        if (team == null) return;
        GameBall gameBall = getOrCreateBall(fallingBlock);
        gameBall.setLastKicker(player.getUniqueId());
        fallingBlock.setVelocity(velocity.setX(0).setZ(0));
        fallingBlock.getWorld().playSound(fallingBlock.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.MASTER, 1.0f, 1.5f);
    }

    public static Component formatTime(long millis) {
        long seconds = (millis - 1) / 1000L + 1L;
        long minutes = seconds / 60L;
        return join(noSeparators(),
                    text(minutes),
                    text("m", WHITE),
                    space(),
                    text(seconds % 60L, WHITE),
                    text("s"));
    }

    public void onSidebar(Player player, List<Component> lines) {
        if (player.getGameMode() == GameMode.SPECTATOR) return;
        switch (state.getPhase()) {
        case WAIT_FOR_PLAYERS: {
            lines.addAll(List.of(text("Game starting soon!", GREEN),
                                 text("Stand on the playing", YELLOW),
                                 text("field to join.", YELLOW)));
            break;
        }
        case PICK_FLAG: {
            GameTeam team = getTeam(player);
            if (team == null) return;
            final List<Component> line = new ArrayList<>();
            line.add(text("Your team:", team.textColor));
            int length = 10;
            for (Player member : getTeamPlayers(team)) {
                String name = member.getName();
                if (length + 1 + name.length() >= 24) {
                    lines.add(join(noSeparators(), line));
                    line.clear();
                    line.add(text(name, WHITE));
                    length = name.length();
                } else {
                    line.add(text(" " + name, WHITE));
                    length += 1 + name.length();
                }
            }
            if (!line.isEmpty()) lines.add(join(noSeparators(), line));
            break;
        }
        case KICKOFF: case PLAY: case GOAL: {
            GameTeam theTeam = getTeam(player);
            if (theTeam == null) return;
            long timeLeft = timeLeft(state.getGameStarted(), GAME_TIME);
            lines.add(textOfChildren(theTeam.chatBlock, text(tiny("time "), GRAY), formatTime(timeLeft)));
            lines.add(textOfChildren(theTeam.chatBlock, Mytems.MOUSE_LEFT, text(" High Kick", GRAY)));
            lines.add(textOfChildren(theTeam.chatBlock, Mytems.MOUSE_RIGHT, text(" Shallow Kick", GRAY)));
            lines.add(textOfChildren(theTeam.chatBlock, VanillaEffects.SPEED, text(" More Power", GRAY)));
            List<GameTeam> order = theTeam == GameTeam.RED
                ? List.of(GameTeam.RED, GameTeam.BLUE)
                : List.of(GameTeam.BLUE, GameTeam.RED);
            for (GameTeam team : order) {
                Nation nation = getTeamNation(team);
                List<Component> names = new ArrayList<>();
                int length = 0;
                names.add(text("Team ", team.textColor));
                if (nation.mytems != null) {
                    length += 2;
                    names.add(nation.mytems.asComponent());
                }
                names.add(text(nation.name, team.textColor));
                List<Player> teamPlayers = getTeamPlayers(team);
                String count = "(" + teamPlayers.size() + ")";
                names.add(text(count, GRAY));
                length += nation.name.length() + 5 + count.length();
                for (Player member : teamPlayers) {
                    String name = member.getName();
                    if (length + 1 + name.length() >= 22) {
                        lines.add(join(noSeparators(), names));
                        names.clear();
                        length = 0;
                    } else {
                        names.add(space());
                        length += 1;
                    }
                    names.add(text(name, team.textColor));
                    length += name.length();
                }
                if (!names.isEmpty()) lines.add(join(noSeparators(), names));
            }
            break;
        }
        default: break;
        }
    }

    public static void clearInventory(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i += 1) {
            ItemStack item = player.getInventory().getItem(i);
            Mytems mytems = Mytems.forItem(item);
            if (mytems != null && mytems.getMytem() instanceof WardrobeItem) {
                continue;
            }
            player.getInventory().clear(i);
        }
    }

    public void onFoodLevelChange(FoodLevelChangeEvent event, Player player) {
        event.setCancelled(true);
    }
}
