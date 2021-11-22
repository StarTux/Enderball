package com.cavetale.enderball;

import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.enderball.struct.Cuboid;
import com.cavetale.enderball.util.WorldEdit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public final class EnderballCommand implements TabExecutor {
    private final EnderballPlugin plugin;
    private CommandNode rootNode;

    public void enable() {
        rootNode = new CommandNode("enderball");
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
        rootNode.addChild("tojava").denyTabCompletion()
            .description("Serialize all nation flags to Java")
            .senderCaller(this::toJava);
        plugin.getCommand("enderball").setExecutor(this);
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String alias, final String[] args) {
        return rootNode.call(sender, command, alias, args);
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        return rootNode.complete(sender, command, alias, args);
    }

    Cuboid requireWorldEditSelection(Player player) throws CommandWarn {
        Cuboid cuboid = WorldEdit.getSelection(player);
        if (cuboid == null) throw new CommandWarn("Make a selection first!");
        return cuboid;
    }

    boolean select(Player player, String[] args) {
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

    boolean save(CommandSender sender, String[] args) {
        plugin.getGame().saveState();
        sender.sendMessage("State saved!");
        return true;
    }

    boolean reset(CommandSender sender, String[] args) {
        plugin.getGame().newPhase(GamePhase.IDLE);
        plugin.getGame().saveState();
        sender.sendMessage("Game reset");
        return true;
    }

    boolean start(CommandSender sender, String[] args) {
        plugin.getGame().resetGame();
        plugin.getGame().newPhase(GamePhase.WAIT_FOR_PLAYERS);
        sender.sendMessage("Game started");
        return true;
    }

    boolean event(CommandSender sender, String[] args) {
        if (args.length > 1) return false;
        if (args.length >= 1) {
            try {
                plugin.getGame().getState().setEvent(Boolean.parseBoolean(args[0]));
            } catch (IllegalArgumentException iae) {
                throw new CommandWarn("Boolean expected: " + args[0]);
            }
            plugin.getGame().saveState();
        }
        sender.sendMessage(Component.text("Event: " + plugin.getGame().getState().isEvent(),
                                          NamedTextColor.YELLOW));
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
}
