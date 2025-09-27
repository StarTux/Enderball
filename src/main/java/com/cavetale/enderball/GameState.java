package com.cavetale.enderball;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Data;

@Data
public final class GameState implements Serializable {
    private GamePhase phase = GamePhase.IDLE;
    private List<GameBall> balls = new ArrayList<>();
    private GameTeam kickoffTeam;
    private List<Integer> scores = new ArrayList<>(List.of(0, 0));
    private Map<UUID, GameTeam> teams = new HashMap<>();
    private List<Nation> nations = new ArrayList<>();
    private long waitForPlayersStarted;
    private long pickFlagStarted;
    private long gameStarted;
    private long kickoffStarted;
    private long goalStarted;
    private long endStarted;
    private boolean manual;
    private Map<UUID, Integer> ballContacts = new HashMap<>();

    public void addBallContact(UUID uuid) {
        ballContacts.put(uuid, ballContacts.getOrDefault(uuid, 0) + 1);
    }

    public void addGoal(GameTeam team) {
        scores.set(team.ordinal(), getGoals(team) + 1);
    }

    public int getGoals(GameTeam team) {
        return scores.get(team.ordinal());
    }
}
