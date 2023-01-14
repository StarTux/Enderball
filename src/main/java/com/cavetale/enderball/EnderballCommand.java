package com.cavetale.enderball;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.enderball.struct.Cuboid;
import com.cavetale.enderball.util.WorldEdit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
        rootNode.addChild("select")
            .completableList(Arrays.asList("area", "field", "kickoff", "goal0", "goal1", "spawn0", "spawn1", "outside"))
            .playerCaller(this::select);
        rootNode.addChild("save").denyTabCompletion()
            .senderCaller(this::save);
        rootNode.addChild("reset").denyTabCompletion()
            .senderCaller(this::reset);
        rootNode.addChild("start").denyTabCompletion()
            .senderCaller(this::start);
        rootNode.addChild("event").arguments("true|false")
            .description("Set event state")
            .completers(CommandArgCompleter.list("true", "false"))
            .senderCaller(this::event);
        rootNode.addChild("testing").arguments("true|false")
            .description("Set testing state")
            .completers(CommandArgCompleter.list("true", "false"))
            .senderCaller(this::testing);
        rootNode.addChild("manual").arguments("true|false")
            .description("Set manual mode")
            .completers(CommandArgCompleter.list("true", "false"))
            .senderCaller(this::manual);
        rootNode.addChild("tojava").denyTabCompletion()
            .description("Serialize all nation flags to Java")
            .senderCaller(this::toJava);
        rootNode.addChild("highlight").denyTabCompletion()
            .description("Highlight the field")
            .playerCaller(this::highlight);
        rootNode.addChild("kick").arguments("<player>")
            .description("Kick a player from the game")
            .completers(CommandArgCompleter.NULL)
            .playerCaller(this::kick);
        CommandNode teamNode = rootNode.addChild("team")
            .description("Team commands");
        teamNode.addChild("reset").denyTabCompletion()
            .description("Reset teams")
            .senderCaller(this::teamClear);
        teamNode.addChild("red").arguments("<players>")
            .description("Add players to red team")
            .completers(CommandArgCompleter.NULL, CommandArgCompleter.REPEAT)
            .senderCaller(this::teamRed);
        teamNode.addChild("blue").arguments("<players>")
            .description("Add players to blue team")
            .completers(CommandArgCompleter.NULL, CommandArgCompleter.REPEAT)
            .senderCaller(this::teamBlue);
        CommandNode scoreNode = rootNode.addChild("score").description("Score subcommands");
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

    protected Cuboid requireWorldEditSelection(Player player) throws CommandWarn {
        Cuboid cuboid = WorldEdit.getSelection(player);
        if (cuboid == null) throw new CommandWarn("Make a selection first!");
        return cuboid;
    }

    protected boolean select(Player player, String[] args) {
        if (args.length != 1) return false;
        switch (args[0]) {
        case "world":
            plugin.getGame().getBoard().setWorld(player.getWorld().getName());
            player.sendMessage("World = " + plugin.getGame().getBoard().getWorld());
            break;
        case "area":
            plugin.getGame().getBoard().setArea(requireWorldEditSelection(player));
            player.sendMessage("Area = " + plugin.getGame().getBoard().getArea());
            break;
        case "field":
            plugin.getGame().getBoard().setField(requireWorldEditSelection(player));
            player.sendMessage("Field = " + plugin.getGame().getBoard().getField());
            break;
        case "kickoff":
            plugin.getGame().getBoard().setKickoff(requireWorldEditSelection(player).getMin());
            player.sendMessage("Kickoff = " + plugin.getGame().getBoard().getKickoff());
            break;
        case "goal0":
            plugin.getGame().getBoard().getGoals().set(0, requireWorldEditSelection(player));
            player.sendMessage("Goal[0] = " + plugin.getGame().getBoard().getGoals().get(0));
            break;
        case "goal1":
            plugin.getGame().getBoard().getGoals().set(1, requireWorldEditSelection(player));
            player.sendMessage("Goal[1] = " + plugin.getGame().getBoard().getGoals().get(1));
            break;
        case "spawn0":
            plugin.getGame().getBoard().getSpawns().set(0, requireWorldEditSelection(player));
            player.sendMessage("Spawn[0] = " + plugin.getGame().getBoard().getSpawns().get(0));
            break;
        case "spawn1":
            plugin.getGame().getBoard().getSpawns().set(1, requireWorldEditSelection(player));
            player.sendMessage("Spawn[1] = " + plugin.getGame().getBoard().getSpawns().get(1));
            break;
        case "outside":
            plugin.getGame().getBoard().setOutside(requireWorldEditSelection(player).getMin());
            player.sendMessage("Outside = " + plugin.getGame().getBoard().getOutside());
            break;
        default:
            throw new CommandWarn("Unknown: " + args[0]);
        }
        plugin.getGame().saveBoard();
        return true;
    }

    protected boolean save(CommandSender sender, String[] args) {
        plugin.getGame().saveState();
        sender.sendMessage("State saved!");
        return true;
    }

    protected boolean reset(CommandSender sender, String[] args) {
        plugin.getGame().newPhase(GamePhase.IDLE);
        plugin.getGame().saveState();
        sender.sendMessage("Game reset");
        return true;
    }

    protected boolean start(CommandSender sender, String[] args) {
        plugin.getGame().resetGame();
        plugin.getGame().newPhase(GamePhase.WAIT_FOR_PLAYERS);
        sender.sendMessage("Game started");
        return true;
    }

    protected boolean manual(CommandSender sender, String[] args) {
        if (args.length > 1) return false;
        if (args.length >= 1) {
            try {
                plugin.getGame().getState().setManual(Boolean.parseBoolean(args[0]));
            } catch (IllegalArgumentException iae) {
                throw new CommandWarn("Boolean expected: " + args[0]);
            }
            plugin.getGame().saveState();
        }
        sender.sendMessage(text("Manual Mode: " + plugin.getGame().getState().isManual(),
                                YELLOW));
        return true;
    }

    protected boolean toJava(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        List<String> lines = new ArrayList<>();
        for (Nation nation : Nation.values()) {
            lines.add("// " + nation.name());
            lines.addAll(com.cavetale.mytems.util.JavaItem.serializeToLines(nation.bannerItem));
        }
        sender.sendMessage(String.join("\n", lines));
        return true;
    }

    private void highlight(Player player) {
        for (Game game : plugin.getGames()) {
            if (!player.getWorld().equals(game.getWorld())) continue;
            for (Cuboid cuboid : game.getBoard().getAllAreas()) {
                cuboid.highlight(player);
            }
        }
        player.sendMessage(text("All areas highlighted", AQUA));
    }

    private boolean kick(Player player, String[] args) {
        if (args.length != 1) return false;
        Game game = plugin.getGameAt(player.getLocation());
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

    private boolean event(CommandSender sender, String[] args) {
        if (args.length > 1) return false;
        if (args.length == 1) {
            try {
                Boolean value = Boolean.parseBoolean(args[0]);
                plugin.getSave().setEvent(value);
                plugin.save();
            } catch (IllegalArgumentException iae) {
                throw new CommandWarn("Not a boolean: " + args[0]);
            }
        }
        sender.sendMessage(plugin.getSave().isEvent()
                           ? text("Event mode enabled", GREEN)
                           : text("Event mode disabled", RED));
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

    protected boolean teamClear(CommandSender sender, String[] args) {
        if (args.length == 0) return false;
        Game game = plugin.getGame();
        game.getState().setTeams(new HashMap<>());
        game.saveState();
        sender.sendMessage(text("Teams were reset", YELLOW));
        return true;
    }

    protected boolean teamRed(CommandSender sender, String[] args) {
        return teamMembers(GameTeam.RED, sender, args);
    }

    protected boolean teamBlue(CommandSender sender, String[] args) {
        return teamMembers(GameTeam.BLUE, sender, args);
    }

    protected boolean teamMembers(GameTeam team, CommandSender sender, String[] args) {
        if (args.length == 0) return false;
        Game game = plugin.getGame();
        Map<UUID, GameTeam> addMap = new HashMap<>();
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
        sender.sendMessage(text("Added to team " + team.humanName
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
