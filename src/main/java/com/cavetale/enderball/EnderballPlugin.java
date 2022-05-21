package com.cavetale.enderball;

import com.cavetale.core.util.Json;
import com.cavetale.enderball.struct.Save;
import com.cavetale.enderball.util.Gui;
import com.cavetale.fam.trophy.Highscore;
import com.cavetale.mytems.item.trophy.TrophyCategory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@Getter
public final class EnderballPlugin extends JavaPlugin {
    private EnderballCommand enderballCommand = new EnderballCommand(this);
    private EventListener eventListener = new EventListener(this);
    private final List<Game> games = new ArrayList<>();
    private Save save = new Save();
    private File saveFile;
    private File boardFile;
    private File stateFile;
    private List<Highscore> highscore = List.of();

    @Override
    public void onEnable() {
        enderballCommand.enable();
        eventListener.enable();
        saveFile = new File(getDataFolder(), "save.json");
        boardFile = new File(getDataFolder(), "board.json");
        stateFile = new File(getDataFolder(), "state.json");
        getDataFolder().mkdirs();
        load();
        games.add(new Game(this,
                           Json.load(boardFile, GameBoard.class, GameBoard::new),
                           Json.load(stateFile, GameState.class, GameState::new)));
        games.get(0).enable();
        Gui.enable(this);
        computeHighscore();
    }

    @Override
    public void onDisable() {
        Gui.disable(this);
        games.get(0).disable();
        games.clear();
        save();
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

    private void load() {
        save = Json.load(saveFile, Save.class, Save::new);
    }

    public void save() {
        if (save == null) return;
        Json.save(saveFile, save, true);
    }

    public void computeHighscore() {
        highscore = Highscore.of(save.getScore());
    }

    public int rewardHighscore() {
        return Highscore.reward(save.getScore(),
                                "enderball_event",
                                TrophyCategory.CUP,
                                text("Enderball", LIGHT_PURPLE),
                                hi -> {
                                    int goals = save.getGoals(hi.uuid);
                                    int assists = save.getAssists(hi.uuid);
                                    return "You collected " + hi.score + " point" + (hi.score == 1 ? "" : "s")
                                        + (assists > 0
                                           ? ", assisted " + assists + " time" + (assists == 1 ? "" : "s")
                                           : "")
                                        + (goals > 0
                                           ? " and scored " + goals + " goal" + (goals == 1 ? "" : "s")
                                           : "");
                                });
    }
}
