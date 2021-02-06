package com.cavetale.enderball;

import com.cavetale.enderball.util.Json;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class EnderballPlugin extends JavaPlugin {
    private EnderballCommand enderballCommand = new EnderballCommand(this);
    private EventListener eventListener = new EventListener(this);
    private final List<Game> games = new ArrayList<>();
    private File boardFile;
    private File stateFile;

    @Override
    public void onEnable() {
        enderballCommand.enable();
        eventListener.enable();
        boardFile = new File(getDataFolder(), "board.json");
        stateFile = new File(getDataFolder(), "state.json");
        getDataFolder().mkdirs();
        games.add(new Game(this,
                           Json.load(boardFile, GameBoard.class, GameBoard::new),
                           Json.load(stateFile, GameState.class, GameState::new)));
        games.get(0).enable();
    }

    @Override
    public void onDisable() {
        games.get(0).disable();
        games.clear();
    }

    public Game getGame() {
        return games.get(0);
    }

    public Game getGameAt(Block block) {
        for (Game game : games) {
            if (game.getBoard().getArea().contains(block)) {
                return game;
            }
        }
        return null;
    }

    public Game getGameAt(Location location) {
        for (Game game : games) {
            if (game.getBoard().getArea().contains(location)) {
                return game;
            }
        }
        return null;
    }
}
