package com.cavetale.enderball;

import com.cavetale.core.font.DefaultFont;
import com.cavetale.enderball.struct.Cuboid;
import com.cavetale.enderball.struct.Vec3i;
import com.cavetale.enderball.util.Fireworks;
import com.cavetale.enderball.util.Gui;
import com.cavetale.enderball.util.Json;
import com.cavetale.mytems.Mytems;
import com.cavetale.sidebar.PlayerSidebarEvent;
import com.cavetale.sidebar.Priority;
import com.winthier.title.TitlePlugin;
import java.time.Duration;
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
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
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
    private final Map<UUID, Nation> nationVotes = new HashMap<>();

    public void enable() {
        board.prep();
        state.prep();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
        bossBar = BossBar.bossBar(Component.text("Enderball"), 1.0f,
                                  BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showBossBar(bossBar);
        }
    }

    public void disable() {
        saveState();
        task.cancel();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.hideBossBar(bossBar);
        }
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
        removeAllBalls();
        for (Player player : getPresentPlayers()) {
            player.getInventory().clear();
            TitlePlugin.getInstance().setColor(player, null);
        }
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

    GameTeam getGoal(Vec3i vec) {
        for (int i = 0; i < 2; i += 1) {
            Cuboid cuboid = board.getGoals().get(i);
            if (cuboid.contains(vec)) return GameTeam.of(i);
        }
        return null;
    }

    boolean ballZoneAction(GameBall gameBall) {
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
        if (state.getPhase() == GamePhase.PLAY) {
            if (!ballZoneAction(gameBall)) {
                removeBall(gameBall);
            }
        }
    }

    void kickoff(Vec3i vec, UUID lastKicker) {
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
        Block block = kickoffVector.toBlock(getWorld());
        block.setType(Material.DRAGON_EGG, false);
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

    public Component getScoreComponent() {
        Nation a = getTeamNation(GameTeam.RED);
        Nation b = getTeamNation(GameTeam.BLUE);
        TextColor white = NamedTextColor.WHITE;
        return Component.text()
            .append(Component.text(a.name, GameTeam.RED.textColor)).append(a.component).append(Component.space())
            .append(Component.text("" + state.getScores().get(0), GameTeam.RED.textColor, TextDecoration.BOLD))
            .append(Component.text(" : ", NamedTextColor.WHITE))
            .append(Component.text("" + state.getScores().get(1), GameTeam.BLUE.textColor, TextDecoration.BOLD))
            .append(Component.space()).append(b.component).append(Component.text(b.name, GameTeam.BLUE.textColor))
            .build();
    }

    void scoreGoal(GameTeam goal, GameBall gameBall) {
        removeAllBalls();
        if (state.getPhase() != GamePhase.PLAY) return;
        GameTeam team = goal.other();
        int index = team.toIndex();
        state.getScores().set(index, state.getScores().get(index) + 1);
        Player player = gameBall.getLastKickerPlayer();
        Component title = Component.text("Goal!", team.textColor);
        Component subtitle = getScoreComponent();
        String chat;
        if (player != null) {
            GameTeam playerTeam = getTeam(player);
            chat = player.getName() + " scored a " + (team == playerTeam ? "goal" : "own goal") + " for "
                + team.chatColor + ChatColor.BOLD + getTeamName(team) + "!";
            if (playerTeam == team && state.isEvent()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "titles unlockset " + player.getName() + " Fußball Striker Goal");
            }
        } else {
            chat = "Goal for " + team.chatColor + ChatColor.BOLD + getTeamName(team) + "!";
        }
        Title theTitle = Title.title(title, subtitle, Title.Times.of(Duration.ZERO, Duration.ofSeconds(3), Duration.ZERO));
        for (Player target : getPresentPlayers()) {
            target.sendMessage(chat);
            target.showTitle(theTitle);
        }
        plugin.getLogger().info("Goal for " + team + ": " + (player != null ? player.getName() : "null"));
        bossBar.name(Component.text(chat));
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
                player.sendMessage(Component.text()
                                   .append(Component.text("Your team flag is ", team.textColor))
                                   .append(nation.component).append(Component.text(nation.name, team.textColor))
                                   .build());
                player.showTitle(Title.title(Component.empty(),
                                             Component.text(nation.name, team.textColor)));
            }
        }
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
                player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
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
            bossBar.name(Component.text("Paused", NamedTextColor.GRAY));
            bossBar.progress(1.0f);
            resetGame();
            break;
        case WAIT_FOR_PLAYERS:
            removeAllBalls();
            bossBar.name(Component.text("Preparing the Game", NamedTextColor.GRAY));
            state.setWaitForPlayersStarted(System.currentTimeMillis());
            break;
        case TEAMS:
            makeTeams();
            setupPhase(GamePhase.PICK_FLAG);
            break;
        case PICK_FLAG:
            bossBar.name(Component.text("Pick a Nation", NamedTextColor.GREEN));
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
            Nation nation = getTeamNation(team);
            Component txt = Component.text()
                .append(Component.text("Kickoff for ", NamedTextColor.WHITE))
                .append(nation.component).append(Component.text(nation.name, team.textColor))
                .build();
            Title title = Title.title(Component.empty(), txt,
                                      Title.Times.of(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO));
            for (Player player : getPresentPlayers()) {
                player.showTitle(title);
            }
            state.setKickoffStarted(System.currentTimeMillis());
            bossBar.name(txt);
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
            bossBar.name(getScoreComponent());
            break;
        }
        case GOAL:
            state.setGoalStarted(System.currentTimeMillis());
            break;
        case END: {
            for (Player player : getWorld().getPlayers()) {
                if (getTeam(player) != null) {
                    player.getInventory().clear();
                    TitlePlugin.getInstance().setColor(player, null);
                    if (state.isEvent()) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ml add " + player.getName());
                    }
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
            Component title;
            if (winnerTeam != null) {
                text = "Team " + winnerTeam.chatColor + getTeamName(winnerTeam) + ChatColor.RESET + " wins!";
                Nation nation = getTeamNation(winnerTeam);
                title = Component.text().append(nation.component)
                    .append(Component.text(nation.name, winnerTeam.textColor))
                    .append(Component.text(" Wins!", NamedTextColor.WHITE))
                    .build();
                plugin.getLogger().info("Winner: " + getTeamName(winnerTeam));
            } else {
                text = "It's a draw!";
                title = Component.text("Draw", NamedTextColor.GRAY);
                plugin.getLogger().info("Winner: Draw");
            }
            Title theTitle = Title.title(title, getScoreComponent(),
                                         Title.Times.of(Duration.ofSeconds(1), Duration.ofSeconds(3), Duration.ofSeconds(1)));
            for (Player target : getPresentPlayers()) {
                target.sendMessage(text);
                target.showTitle(theTitle);
                target.playSound(target.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, SoundCategory.MASTER, 0.5f, 1.5f);
            }
            bossBar.name(title);
            break;
        }
        default:
            break;
        }
    }

    void tick() {
        switch (state.getPhase()) {
        case IDLE: return;
        case WAIT_FOR_PLAYERS: {
            List<Player> players = getEligiblePlayers();
            if (players.isEmpty()) {
                state.setWaitForPlayersStarted(System.currentTimeMillis());
                return;
            }
            long total = 30000;
            long timeLeft = timeLeft(state.getWaitForPlayersStarted(), total);
            if (timeLeft <= 0) {
                newPhase(GamePhase.TEAMS);
            } else {
                bossBar.progress(clamp1((float) timeLeft / (float) total));
            }
            break;
        }
        case PICK_FLAG: {
            long total = 30000;
            long timeLeft = timeLeft(state.getPickFlagStarted(), total);
            bossBar.progress(clamp1((float) timeLeft / (float) total));
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
                bossBar.progress(clamp1((float) timeLeft / (float) total));
            }
            break;
        }
        case PLAY: {
            for (GameBall gameBall : new ArrayList<>(state.getBalls())) {
                FallingBlock fallingBlock = gameBall.getEntity();
                if (fallingBlock != null) {
                    gameBall.setBlockVector(Vec3i.of(fallingBlock.getLocation()));
                }
                if (ballZoneAction(gameBall)) return;
            }
            long total = 60L * 15L * 1000L;
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
                bossBar.progress(clamp1((float) timeLeft / (float) total));
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
                newPhase(GamePhase.WAIT_FOR_PLAYERS);
            } else {
                if ((fireworkTicks++ % 10) == 0) {
                    Cuboid field = board.getField();
                    int x = field.getMin().x + random.nextInt(field.getMax().x - field.getMin().x + 1);
                    int z = field.getMin().z + random.nextInt(field.getMax().z - field.getMin().z + 1);
                    int y = field.getMin().y;
                    Fireworks.spawnFirework(new Vec3i(x, y, z).toLocation(getWorld()));
                }
                bossBar.progress(clamp1((float) timeLeft / (float) total));
            }
            break;
        }
        default: break;
        }
    }

    Gui openNationGui(Player player, GameTeam team) {
        int rows = (Nation.values().length - 1) / 9 + 1;
        int size = rows * 9;
        Gui gui = new Gui(plugin)
            .size(size)
            .title(DefaultFont.guiBlankOverlay(size, NamedTextColor.BLACK,
                                               Component.text("Nation Vote", NamedTextColor.WHITE)));
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

    void updateNationGui(Player player, Gui gui, GameTeam team) {
        for (Nation nation : Nation.values()) {
            int slot = nation.ordinal();
            int votes = countNationVotes(nation, team);
            ItemStack item = nation.bannerItem.clone();
            item.setAmount(Math.max(1, Math.min(64, votes)));
            gui.setItem(slot, item, click -> {
                    if (state.getPhase() != GamePhase.PICK_FLAG) return;
                    nationVotes.put(player.getUniqueId(), nation);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                            for (Player target : getTeamPlayers(team)) {
                                Gui gui2 = Gui.of(target);
                                if (gui2 == null) return;
                                updateNationGui(target, gui2, team);
                                target.updateInventory();
                            }
                        });
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 1.0f, 1.0f);
                });
        }
    }

    void dress(Player player, GameTeam team) {
        player.getInventory().clear();
        if (team == null) return;
        //ItemStack helmet = state.getNations().get(team.ordinal()).bannerItem.clone();
        //player.getInventory().setHelmet(helmet);
        player.getInventory().setChestplate(dye(Material.LEATHER_CHESTPLATE, team));
        player.getInventory().setLeggings(dye(Material.LEATHER_LEGGINGS, team));
        player.getInventory().setBoots(dye(Material.LEATHER_BOOTS, team));
        player.getInventory().setItemInOffHand(Mytems.MAGIC_MAP.createItemStack(player));
        TitlePlugin.getInstance().setColor(player, team.textColor);
    }

    ItemStack dye(Material mat, GameTeam team) {
        ItemStack item = new ItemStack(mat);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(team.dyeColor.getColor());
        meta.displayName(Component.text(getTeamName(team), team.textColor));
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }

    public void onJoin(Player player) {
        player.showBossBar(bossBar);
        dress(player, getTeam(player));
    }

    long timeLeft(long other, long total) {
        return Math.max(0L, other + total - System.currentTimeMillis());
    }

    public int getScore(GameTeam team) {
        return state.getScores().get(team.toIndex());
    }

    float clamp1(float in) {
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

    public void onSidebar(PlayerSidebarEvent event, Player player) {
        if (player.getGameMode() == GameMode.SPECTATOR) return;
        switch (state.getPhase()) {
        case WAIT_FOR_PLAYERS: {
            Component[] lines = {
                Component.text("Game starting soon!", NamedTextColor.GREEN),
                Component.text("Stand on the playing", NamedTextColor.YELLOW),
                Component.text("field to join.", NamedTextColor.YELLOW),
            };
            event.add(plugin, Priority.HIGHEST, lines);
            break;
        }
        case PICK_FLAG: {
            GameTeam team = getTeam(player);
            if (team == null) return;
            List<Component> lines = new ArrayList<>();
            StringBuilder sb = new StringBuilder(team.chatColor + "Your team:" + ChatColor.WHITE);
            for (Player member : getTeamPlayers(team)) {
                String name = member.getName();
                if (sb.length() + 1 + name.length() >= 48) {
                    lines.add(Component.text(sb.toString()));
                    sb = new StringBuilder(name);
                } else {
                    sb.append(" ").append(name);
                }
            }
            if (sb.length() > 0) lines.add(Component.text(sb.toString()));
            event.add(plugin, Priority.HIGHEST, lines);
            break;
        }
        case KICKOFF: case PLAY: case GOAL: {
            GameTeam team = getTeam(player);
            if (team == null) return;
            Nation nation = getTeamNation(team);
            List<Component> lines = new ArrayList<>();
            lines.add(Component.text().content("Your team ").color(NamedTextColor.GRAY)
                      .append(nation.component)
                      .append(Component.text(nation.name, team.textColor))
                      .build());
            StringBuilder sb = new StringBuilder();
            for (Player member : getTeamPlayers(team)) {
                String name = member.getName();
                if (sb.length() + 1 + name.length() >= 48) {
                    lines.add(Component.text(sb.toString()));
                    sb = new StringBuilder(name);
                } else {
                    sb.append(" ").append(name);
                }
            }
            if (sb.length() > 0) lines.add(Component.text(sb.toString()));
            lines.add(Component.empty());
            lines.addAll(List.of(new Component[] {
                        Component.text("Punch Ball", NamedTextColor.YELLOW)
                        .append(Component.text(" High Kick", NamedTextColor.WHITE)),
                        Component.text("Right Click Ball", NamedTextColor.YELLOW)
                        .append(Component.text(" Shallow", NamedTextColor.WHITE)),
                        Component.text("Sprint", NamedTextColor.YELLOW)
                        .append(Component.text(" More Power", NamedTextColor.WHITE)),
                        Component.text("Click Falling Ball", NamedTextColor.YELLOW)
                        .append(Component.text(" Block", NamedTextColor.WHITE)),
                    }));
            event.add(plugin, Priority.HIGHEST, lines);
            break;
        }
        default: break;
        }
    }
}
