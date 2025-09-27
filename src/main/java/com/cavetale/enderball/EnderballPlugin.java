package com.cavetale.enderball;

import com.cavetale.afk.AFKPlugin;
import com.cavetale.core.event.minigame.MinigameMatchType;
import com.cavetale.core.util.Json;
import com.cavetale.enderball.struct.Save;
import com.cavetale.fam.trophy.Highscore;
import com.cavetale.mytems.item.trophy.TrophyCategory;
import com.winthier.creative.vote.MapVote;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@Getter
public final class EnderballPlugin extends JavaPlugin {
    public static final Component TITLE = text("Enderball", LIGHT_PURPLE);
    @Getter private static EnderballPlugin instance;
    private EnderballCommand enderballCommand = new EnderballCommand(this);
    private EventListener eventListener = new EventListener(this);
    private final List<Game> games = new ArrayList<>();
    private Save save = new Save();
    private File saveFile;
    private List<Highscore> highscore = List.of();
    private Lobby lobby = new Lobby(this);

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        enderballCommand.enable();
        eventListener.enable();
        saveFile = new File(getDataFolder(), "save.json");
        getDataFolder().mkdirs();
        load();
        computeHighscore();
        Bukkit.getScheduler().runTaskTimer(this, this::tick, 1L, 1L);
    }

    @Override
    public void onDisable() {
        for (Game game : games) {
            game.disable();
        }
        games.clear();
        save();
    }

    private void tick() {
        for (Game game : List.copyOf(games)) {
            if (game.isObsolete()) {
                games.remove(game);
                game.disable();
            } else {
                try {
                    game.tick();
                } catch (Exception e) {
                    getLogger().log(Level.SEVERE, "Ticking game " + game.getBuildWorld().getPath(), e);
                    games.remove(game);
                    game.disable();
                }
            }
        }
        if (save.isPause() || (save.isEvent() && !games.isEmpty())) {
            MapVote.stop(MinigameMatchType.ENDERBALL);
        } else {
            int available = 0;
            for (Player player : lobby.getPlayers()) {
                if (AFKPlugin.isAfk(player)) continue;
                available += 1;
            }
            if (available < 2) {
                MapVote.stop(MinigameMatchType.ENDERBALL);
            } else if (!MapVote.isActive(MinigameMatchType.ENDERBALL)) {
                MapVote.start(MinigameMatchType.ENDERBALL, vote -> {
                        vote.setLobbyWorld(lobby.getWorld());
                        vote.setTitle(TITLE);
                        vote.setCallback(result -> {
                                final Game game = new Game(this, result.getBuildWorldWinner(), result.getLocalWorldCopy());
                                game.enable();
                                game.bringPlayersFromLobby();
                                games.add(game);
                            });
                    });
            }
        }
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
                                TrophyCategory.ENDERBALL,
                                TITLE,
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
