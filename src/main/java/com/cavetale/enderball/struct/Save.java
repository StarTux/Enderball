package com.cavetale.enderball.struct;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Data;

@Data
public final class Save {
    protected Map<UUID, Integer> score = new HashMap<>();
    protected Map<UUID, Integer> goals = new HashMap<>();
    protected Map<UUID, Integer> assists = new HashMap<>();
    protected boolean pause = false;
    protected boolean event = false;
    protected boolean testing = false;

    public int getGoals(UUID uuid) {
        return goals.getOrDefault(uuid, 0);
    }

    public void addGoals(UUID uuid, int value) {
        goals.put(uuid, Math.max(0, getGoals(uuid) + value));
    }

    public int getScore(UUID uuid) {
        return score.getOrDefault(uuid, 0);
    }

    public void addScore(UUID uuid, int value) {
        score.put(uuid, Math.max(0, getScore(uuid) + value));
    }

    public int getAssists(UUID uuid) {
        return assists.getOrDefault(uuid, 0);
    }

    public void addAssists(UUID uuid, int value) {
        assists.put(uuid, Math.max(0, getAssists(uuid) + value));
    }
}
