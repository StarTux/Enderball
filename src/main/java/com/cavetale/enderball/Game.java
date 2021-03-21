package com.cavetale.enderball;

import com.cavetale.enderball.struct.Cuboid;
import com.cavetale.enderball.struct.Vec3i;
import com.cavetale.enderball.util.Fireworks;
import com.cavetale.enderball.util.Items;
import com.cavetale.enderball.util.Json;
import com.destroystokyo.paper.Title;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import lombok.Data;
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
    List<Nation> nations = Arrays
        .asList(new Nation("Denmark", "H4sIAAAAAAAAAC3KsQ6CQBBF0QdoWNbEysqSCuMfWKJ2xlgQWzPCSjbirJkdC/7eEL3dSa4FMiwOpHR1En1gwK4NUt9h9fLsWqGH7sR1tzsxO7HIlHqLvPPxPdBYYHYK4gyABKastue62ZSwWNZDaJ9HVq9jQ30BcyFVJxzt9BrkfyONbYb5PgxB8CuZ+GFN8AUlcpDTnwAAAA=="),
                new Nation("UK", "H4sIAAAAAAAAAJ3OvQrCQBAE4DGHGE9IZ+OjWPrTW4itbC5nOLzswd6m8O01YGEKU2S74WOGtYDB5kRKNy85JAbsrkQRGmy7wN4JPXRfx97fa2L2YmGUWovqEJN7nlmDvq7UrlFeSNULZ4vPRonVN8M0MRssjykmwXAjkwmb1yuc/FA1ponWf+pmUXajN4DFEHvWAm+OsPJfdgEAAA=="),
                new Nation("Germany", "H4sIAAAAAAAAAE3Juw7CMAxG4b8JiBAkWJh4FEYuOwNiRaY1VdTgSI4ZeHuKxNDhDEdfBDxWJzK6sdZUBIi7AJc6bF9JuFV62l65uz9IhDXCG/UR60Mu7XAWS/a5Ur9EuJAZq9QIwAUs/g+Xq8f8WHLRUTaYkk5pNtb89i3W4AtmrsszmQAAAA=="),
                new Nation("Austria", "H4sIAAAAAAAAAFXJsQrCMBAG4N8EMUbo6OCjOKrdO4irnDXWo/EClxPx7VVw0PHji4DHYkdGh6SViwBxFeD4jOWNJfVKF1s/rmzpeCKRpBHeaIhoNrn0YyvG9tzTMEfoyCyp1AjABcy+hsvVY7otueh7GvyW/hcw+fAu5vACZm89qJsAAAA="),
                new Nation("Brazil", "H4sIAAAAAAAAAFXJMQvCMBCG4c9EMU3B0cGf4qh2dxBXOeMZgu0Frufgv7eCgw7v8PJEwKM9kNGZdSxVgLgJcOWG9VCEk9LdtlmZ5XIlEdYIb5QjVru+pkcnVux1otwgHMmMVcYIwAUsvw83qMdiX/uqk8zxR+mH2qnZZ59iDm8X07OImwAAAA=="),
                new Nation("Belgium", "H4sIAAAAAAAAAE3JuwoCMRBG4d8EMUZYS8FHsfTSW4itzK7jEowzkIzIvr0rWGxxisMXAY/VkYyuXGpSAeI2wKU7Nq8k3BV62G7gnPVza0mES4Q36iOafdbueRJLNlyoXyKcyYyL1AjABSz+D2fVY37QrGWUNabUTqkZm/32LebwBRvSzgycAAAA"),
                new Nation("Canada", "H4sIAAAAAAAAAHXNPQvCMBSF4WOCtEbo6OBPcfRjdxBXuY2xDTY3kFwV/70KDu2Q8fBweA2gsdyT0Nml7CMDZl1D+StWwbOziW6yefVe3KUlZpcMtFBn0GyHaO8HFi/vE3UL1EcScYmzAVDVqP4byiaN+S4OMX2lwZhCLpG2bRwZJrdnX6RgizEpxlQ7JWD2mw8WhQ8OR93KIwEAAA=="),
                new Nation("France", "H4sIAAAAAAAAAE3JMQvCMBCG4c8EMU2ho4M/xVHt7iCucq1nDdYLXE7Ef28Fhw7v8PJEwKM+kNGZtaQsQNwEuHTF+pmEe6Wbbd/3ZHzpSIQ1whsNEc1uzP2jFUv2OdFQIRzJjFVKBOACVv+Hs+Kx3Ocx6yQ15tTNqZla/PYl5vAFRMyVH5sAAAA="),
                new Nation("Ireland", "H4sIAAAAAAAAAE3JsQrCMBAG4N+EYhrB0cFHcdS6O4irXOtZj9YLJCfi21vEoePHFwGPVUNGF85FkgJxG+Dkhs1TlLtMd9u9H2J8bUmVc4Q36iPW+zF1w1FN7HOmvkY4kRlnLRGAC1j+DdcWj+qQxpSnWWBeNq/q1xNfag5f4NQhi5sAAAA="),
                new Nation("Mexico", "H4sIAAAAAAAAAFXMuwoCMRSE4TFBjFG3tPBRLL30FmIrZ2PcDe6eQHJEfHsjWMRiip8PxgIaiwMJXXzKITJgNwYq3LAeA3uX6C7bVx/EX1ti9slCC3UWzW6I7nFkCfI+UzeHOZGIT5wtyqnB7NdQkjWm+zjEVKRBTW1Nqz8aXUXLssk3nywKH4a3gJm2AAAA"),
                new Nation("Netherlands", "H4sIAAAAAAAAAI3JvQ5BQRAG0M8useYmt1R4FKWfXiFaGde6NtZsMjsi3h6JQiXKk0OAR7Ni413UmooANAtw6YjpNUnslE82v5+Txf2BRaISvHFPaBe5dJe1WLLHlvsJwobNokolAMOA8cdwWj1Gy5KLvqbFn5W/q/lVwODNm5jDE+IRw2vRAAAA"),
                new Nation("Poland", "H4sIAAAAAAAAAE3JuwrCQBBG4d9dxHWElBY+iqWX3kJsZYxrdkmchckY8e01YJHTHT4CPFYHNr5E7XMRgDYBLt+xfmaJtfLDtu+ULV5vLBKV4I0bQrXrSt0exbJ9ztwsEU5sFlV6AuACFv+HH5J6zPelK/qjChNzQ5rQ2Gzcl5jDFxCI6COcAAAA"),
                new Nation("Russia", "H4sIAAAAAAAAAKXJwQ4BMRAG4F8bVDfZo4NHcWT37iCuMqrWRE2TdkS8PRIHZ45fPg9YNB0p7WKpnAXwCwfDR8yvLDEUOunyfmaN+wOJxOJhlQaPdpVyuPSirI8tDTO4DanGItUDmDhMP4ZJ1WK8zimX17T4v8J3NT8WMHrzJmrwBIAiSiUHAQAA"),
                new Nation("Spain", "H4sIAAAAAAAAAHXJvQrCMBQG0M/GYo3SVfBRHP3ZHcRVbmtqo+m9kF6Rvr0KDungeDgWMFjsSensYu+FAbsukPkrVp1nV0dqdDO4EOR1qYjZRQujdLMot0Hqx4HV63Ci2xzFkVRd5N4CyAvMfobp5G6Q7yRI/NQS6TVB/l7bVslN08tCn1Q5qjguYPLlkzXDG+0i9RvwAAAA"),
                new Nation("Sweden", "H4sIAAAAAAAAAJ3MsQrCMBSF4WNiMabgKvgojtruDuIqtzG2wXgDyRXp21vBwVEczvDzwbGARt2Q0MnnEhIDdmOgwgXre2DvMl1lO/oY0/PcEbPPFlqot1jtYnK3liXIeKR+CXMgEZ+5WACVweLTUFI0qn2KKU9S4zfSw9D9Y8p9X86nzd75YFF4AbvWklrvAAAA"),
                new Nation("Ukraine", "H4sIAAAAAAAAAH3JPQoCMRAG0M9EMUbwAHoTS396C7FdZtcxOxgnkJ0VvL0K1paPFwGP5YGMLlwHKQrEdYCTKzYPUe4q3WybJfXWtHnkpiVVrhHeKEWsdrl096Oa2OtMaYFwIjOuOkQALmD+M9yz95jtSy71M1P8KWDy5ajm8AZ1xLEMoAAAAA=="),
                new Nation("USA", "H4sIAAAAAAAAAE2JvQrCQBAGP+8Qzw1YWfkolv70KcRW1uQMh5c92NsUvr0GLDIwxTAEeDQXNr5HrakIQIcAl3rsxySxU37ZUWP/eLJIVII3Hgi7Uy7d+yqW7HPjYYvQsllUqQTABWz+DVerx/pcclHMLJflxWp+ruacxBy+DPYRHpkAAAA="),
                new Nation("Italy", "H4sIAAAAAAAAAI3JPQ8BQRAG4NcusUZcqfBTlD56hWhl7qy7jTObzI6If49EoRLlk4cAj+mGjQ9RS8oC0CLApRPm1ySxUT7b8t4li8eaRaISvHFLqFZ9bi5bsWSPPbcThB2bRZVCAIYB44/hrHiM1rnP+poZ/qz6u6pfBQzevIk5PAHLPNgt0QAAAA=="),
                new Nation("Switzerland", "H4sIAAAAAAAAAH3JsQrCMBRG4d+EYozQyclHcbS6O4ir3KaxBNsbuLkOvr0UHNpBz3b4PGCxPZHSLUpJmQG/dzCpw25MHIPQQw8Su3tLzFE8rFLvUR+HHJ5n1qTvK/UbuAupRuHiAVQO6+/DhGJRNXnIgqk5jb9J51QvqP1DeUHAatoXq8EH9v2cIuoAAAA="),
                new Nation("South Korea", "H4sIAAAAAAAAAHXMvQrCMBSG4c8EaU1BFycvxdGf3UFc5aSNJbSeQHI6ePcqODRCxpcHXgNoNCcSurmYfGDA7Goo32H79OzaSA/ZR9fdLTG7aKCFeoP1YQztcGbx8rpSv0J9IREXORkAVY3q19Dj1Gksj2EM8UMNZqYkzQgZ2SLp1oai2Wy5yUzKpv6WwOKbE4vCG88mtgYkAQAA"),
                new Nation("Norway", "H4sIAAAAAAAAAHXJuwoCMRBG4d9EMWZhSwsfxdJLbyG2Msa4hs1OIBkR394VLGJhcYrDZwGNZkdCJ59LSAzYlYEKVyyHwN5lusn6eQ/izxdi9tlCC3UW7SYm1+9ZgryO1C1gDiTiMxcLYGow/z5ULhqzbYopj9KipvifhprwQ8VV1IxNPvtgUXgD30GWRdEAAAA=")
                );

    public void enable() {
        board.prep();
        state.prep();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
        bossBar = Bukkit.createBossBar("Enderball", BarColor.GREEN, BarStyle.SOLID);
        bossBar.setVisible(true);
        if (state.getTeamFlags().size() == 2 && state.getTeamNames().size() == 2) {
            teamFlagItems.clear();
            teamFlagItems.add(Items.deserialize(state.getTeamFlags().get(0), state.getTeamNames().get(0), GameTeam.RED));
            teamFlagItems.add(Items.deserialize(state.getTeamFlags().get(1), state.getTeamNames().get(1), GameTeam.BLUE));
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

    public void warpOutside(Player player) {
        player.teleport(board.getOutside().toLocation(getWorld()), TeleportCause.PLUGIN);
    }

    @Data
    static final class Nation {
        protected final String name;
        protected final String base64;
    }

    void makeFlags() {
        Collections.shuffle(nations);
        List<String> teamFlags = new ArrayList<>();
        List<String> teamNames = new ArrayList<>();
        Nation red = nations.get(0);
        Nation blue = nations.get(1);
        teamFlags.add(red.base64);
        teamFlags.add(blue.base64);
        teamNames.add(red.name);
        teamNames.add(blue.name);
        state.setTeamFlags(teamFlags);
        state.setTeamNames(teamNames);
        teamFlagItems.clear();
        teamFlagItems.add(Items.deserialize(red.base64, red.name, GameTeam.RED));
        teamFlagItems.add(Items.deserialize(blue.base64, blue.name, GameTeam.BLUE));
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
            resetGame();
            makeFlags();
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
        if (state.getTeamNames().size() != 2) return team.humanName;
        return state.getTeamNames().get(team.ordinal());
    }
}
