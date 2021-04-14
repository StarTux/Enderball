package com.cavetale.enderball;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Data;

@Data
public final class GameState implements Serializable {
    private GamePhase phase = GamePhase.IDLE;
    private List<GameBall> balls = Collections.emptyList();
    private int kickoffTeam;
    private List<Integer> scores = Arrays.asList(0, 0);
    private Map<UUID, GameTeam> teams = new HashMap<>();
    private List<Nation> nations = new ArrayList<>();
    private long pickFlagStarted;
    private long gameStarted;
    private long kickoffStarted;
    private long goalStarted;
    private long endStarted;

    public void prep() {
        if (!(balls instanceof ArrayList)) {
            balls = new ArrayList<>(balls);
        }
        if (!(scores instanceof ArrayList)) {
            scores = new ArrayList<>(scores);
        }
        if (!(teams instanceof HashMap)) {
            teams = new HashMap<>(teams);
        }
    }
}
