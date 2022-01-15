package com.cavetale.enderball;

public enum GamePhase {
    IDLE,
    WAIT_FOR_PLAYERS,
    TEAMS, // make teams
    PICK_FLAG,
    PLAYERS, // prepare
    KICKOFF,
    PLAY,
    GOAL,
    END;

    public boolean isPlaying() {
        switch (this) {
        case KICKOFF:
        case PLAY:
        case GOAL:
            return true;
        default: return false;
        }
    }
}
