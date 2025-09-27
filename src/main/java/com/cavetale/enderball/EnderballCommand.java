package com.cavetale.enderball;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.event.minigame.MinigameMatchType;
import com.cavetale.core.playercache.PlayerCache;
import com.winthier.creative.BuildWorld;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class EnderballCommand extends AbstractCommand<EnderballPlugin> {
    protected EnderballCommand(final EnderballPlugin plugin) {
        super(plugin, "enderball");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("start").arguments("<map>")
            .description("Start a game")
            .completers(CommandArgCompleter.supplyList(this::getMapPaths))
            .senderCaller(this::start);
        rootNode.addChild("stop").denyTabCompletion()
            .description("Stop a game")
            .playerCaller(this::stop);
        rootNode.addChild("event").arguments("true|false")
            .description("Set event state")
            .completers(CommandArgCompleter.BOOLEAN)
            .senderCaller(this::event);
        rootNode.addChild("pause").arguments("true|false")
            .description("Set pause state")
            .completers(CommandArgCompleter.BOOLEAN)
            .senderCaller(this::pause);
        rootNode.addChild("testing").arguments("true|false")
            .description("Set testing state")
            .completers(CommandArgCompleter.list("true", "false"))
            .senderCaller(this::testing);
        rootNode.addChild("manual").arguments("true|false")
            .description("Set manual mode")
            .completers(CommandArgCompleter.list("true", "false"))
            .playerCaller(this::manual);
        rootNode.addChild("tojava").denyTabCompletion()
            .description("Serialize all nation flags to Java")
            .senderCaller(this::toJava);
        rootNode.addChild("kick").arguments("<player>")
            .description("Kick a player from the game")
            .completers(CommandArgCompleter.NULL)
            .playerCaller(this::kick);
        rootNode.addChild("skip").denyTabCompletion()
            .description("Skip cooldowns")
            .playerCaller(this::skip);
        CommandNode teamNode = rootNode.addChild("team")
            .description("Team commands");
        teamNode.addChild("reset").denyTabCompletion()
            .description("Reset teams")
            .playerCaller(this::teamClear);
        teamNode.addChild("red").arguments("<players>")
            .description("Add players to red team")
            .completers(CommandArgCompleter.NULL, CommandArgCompleter.REPEAT)
            .playerCaller(this::teamRed);
        teamNode.addChild("blue").arguments("<players>")
            .description("Add players to blue team")
            .completers(CommandArgCompleter.NULL, CommandArgCompleter.REPEAT)
            .playerCaller(this::teamBlue);
        final CommandNode scoreNode = rootNode.addChild("score").description("Score subcommands");
        scoreNode.addChild("reset").denyTabCompletion()
            .description("Reset scores")
            .senderCaller(this::scoreReset);
        scoreNode.addChild("add").arguments("<player> <amount>")
            .description("Manipulate score")
            .completers(PlayerCache.NAME_COMPLETER,
                        CommandArgCompleter.integer(i -> i != 0))
            .senderCaller(this::scoreAdd);
        scoreNode.addChild("reward").denyTabCompletion()
            .description("Reward scores")
            .senderCaller(this::scoreReward);
    }

    private List<String> getMapPaths() {
        final List<String> result = new ArrayList<>();
        for (BuildWorld buildWorld : BuildWorld.findMinigameWorlds(MinigameMatchType.ENDERBALL, false)) {
            result.add(buildWorld.getPath());
        }
        return result;
    }

    private boolean start(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        final BuildWorld buildWorld = BuildWorld.findWithPath(args[0]);
        if (buildWorld == null) {
            throw new CommandWarn("Build world not found: " + args[0]);
        }
        if (buildWorld.getRow().parseMinigame() != MinigameMatchType.ENDERBALL) {
            throw new CommandWarn("Not an Enderball map: " + buildWorld.getName());
        }
        buildWorld.makeLocalCopyAsync(world -> {
                final Game game = new Game(plugin, buildWorld, world);
                try {
                    game.enable();
                } catch (IllegalStateException iae) {
                    plugin.getLogger().log(Level.SEVERE, "Enabling game: " + buildWorld.getPath(), iae);
                    game.disable();
                    sender.sendMessage(text("...Error: " + iae.getMessage(), RED));
                }
                plugin.getGames().add(game);
                game.bringPlayersFromLobby();
            });
        sender.sendMessage(text("Starting game...", YELLOW));
        return true;
    }

    private void stop(Player player) {
        final Game game = Game.in(player.getWorld());
        if (game == null) throw new CommandWarn("No game here");
        game.disable();
        plugin.getGames().remove(game);
        player.sendMessage(text("Game stopped", YELLOW));
    }

    private boolean manual(Player player, String[] args) {
        if (args.length > 1) return false;
        final Game game = Game.in(player.getWorld());
        if (game == null) throw new CommandWarn("No game here");
        if (args.length >= 1) {
            game.getState().setManual(CommandArgCompleter.requireBoolean(args[0]));
        }
        player.sendMessage(text("Manual Mode: " + game.getState().isManual(), YELLOW));
        return true;
    }

    private boolean toJava(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        List<String> lines = new ArrayList<>();
        for (Nation nation : Nation.values()) {
            lines.add("// " + nation.name());
            lines.addAll(com.cavetale.mytems.util.JavaItem.serializeToLines(nation.bannerItem));
        }
        sender.sendMessage(String.join("\n", lines));
        return true;
    }

    private boolean kick(Player player, String[] args) {
        if (args.length != 1) return false;
        final Game game = Game.in(player.getWorld());
        if (game == null) throw new CommandWarn("No game here");
        PlayerCache playerCache = PlayerCache.require(args[0]);
        GameTeam team = game.getState().getTeams().remove(playerCache.uuid);
        if (team == null) {
            throw new CommandWarn("Player not in team: " + playerCache.name);
        }
        Player target = Bukkit.getPlayer(playerCache.uuid);
        if (target != null) {
            game.warpOutside(target);
        }
        player.sendMessage(text("Player kicked from team " + team.humanName + ": " + target.getName(), YELLOW));
        return true;
    }

    private void skip(Player player) {
        final Game game = Game.in(player.getWorld());
        if (game == null) throw new CommandWarn("No game here");
        game.setSkip(true);
        player.sendMessage(text("Skipping...", YELLOW));
    }

    private boolean event(CommandSender sender, String[] args) {
        if (args.length > 1) return false;
        if (args.length == 1) {
            final boolean value = CommandArgCompleter.requireBoolean(args[0]);
            plugin.getSave().setEvent(value);
            plugin.save();
        }
        sender.sendMessage(plugin.getSave().isEvent()
                           ? text("Event mode enabled", GREEN)
                           : text("Event mode disabled", RED));
        return true;
    }

    private boolean pause(CommandSender sender, String[] args) {
        if (args.length > 1) return false;
        if (args.length == 1) {
            final boolean value = CommandArgCompleter.requireBoolean(args[0]);
            plugin.getSave().setPause(value);
            plugin.save();
        }
        sender.sendMessage(plugin.getSave().isPause()
                           ? text("Pause mode enabled", GREEN)
                           : text("Pause mode disabled", RED));
        return true;
    }

    private boolean testing(CommandSender sender, String[] args) {
        if (args.length > 1) return false;
        if (args.length == 1) {
            try {
                Boolean value = Boolean.parseBoolean(args[0]);
                plugin.getSave().setTesting(value);
                plugin.save();
            } catch (IllegalArgumentException iae) {
                throw new CommandWarn("Not a boolean: " + args[0]);
            }
        }
        sender.sendMessage(plugin.getSave().isTesting()
                           ? text("Testing mode enabled", GREEN)
                           : text("Testing mode disabled", RED));
        return true;
    }

    private boolean teamClear(Player player, String[] args) {
        if (args.length == 0) return false;
        final Game game = Game.in(player.getWorld());
        game.getState().setTeams(new HashMap<>());
        player.sendMessage(text("Teams were reset", YELLOW));
        return true;
    }

    private boolean teamRed(Player player, String[] args) {
        return teamMembers(GameTeam.RED, player, args);
    }

    private boolean teamBlue(Player player, String[] args) {
        return teamMembers(GameTeam.BLUE, player, args);
    }

    private boolean teamMembers(GameTeam team, Player player, String[] args) {
        if (args.length == 0) return false;
        final Game game = Game.in(player.getWorld());
        final Map<UUID, GameTeam> addMap = new HashMap<>();
        for (String arg : args) {
            Player target = Bukkit.getPlayerExact(arg);
            if (target == null) {
                throw new CommandWarn("Player not found: " + arg);
            }
            addMap.put(target.getUniqueId(), team);
        }
        if (game.getState().getTeams() == null) {
            game.getState().setTeams(new HashMap<>());
        }
        game.getState().getTeams().putAll(addMap);
        if (game.getState().getPhase().isPlaying()) {
            for (Map.Entry<UUID, GameTeam> entry : addMap.entrySet()) {
                Player target = Bukkit.getPlayer(entry.getKey());
                if (target == null) continue;
                game.dress(target, entry.getValue());
            }
        }
        player.sendMessage(text("Added to team " + team.humanName
                                + ": " + String.join(" ", args),
                                YELLOW));
        return true;
    }

    private void scoreReset(CommandSender sender) {
        plugin.getSave().getScore().clear();
        plugin.getSave().getGoals().clear();
        plugin.getSave().getAssists().clear();
        plugin.save();
        plugin.computeHighscore();
        sender.sendMessage(text("All scores were reset", AQUA));
    }

    private boolean scoreAdd(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        PlayerCache target = PlayerCache.require(args[0]);
        int value = CommandArgCompleter.requireInt(args[1], i -> true);
        plugin.getSave().addScore(target.uuid, value);
        plugin.computeHighscore();
        sender.sendMessage(text("Score of " + target.name + " is now "
                                + plugin.getSave().getScore(target.uuid), AQUA));
        return true;
    }

    private void scoreReward(CommandSender sender) {
        int count = plugin.rewardHighscore();
        sender.sendMessage(text(count + " highscore(s) rewarded", AQUA));
    }
}
