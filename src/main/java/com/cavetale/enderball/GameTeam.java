package com.cavetale.enderball;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;

public enum GameTeam {
    RED(ChatColor.RED, DyeColor.RED),
    BLUE(ChatColor.BLUE, DyeColor.LIGHT_BLUE);

    public final ChatColor chatColor;
    public final DyeColor dyeColor;
    public final String humanName;

    GameTeam(final ChatColor chatColor, final DyeColor dyeColor) {
        this.chatColor = chatColor;
        this.dyeColor = dyeColor;
        this.humanName = name().substring(0, 1) + name().substring(1).toLowerCase();
    }

    public static GameTeam of(int i) {
        return i == 0
            ? RED
            : i == 1
            ? BLUE
            : null;
    }

    public int toIndex() {
        return ordinal();
    }

    public GameTeam other() {
        switch (this) {
        case RED: return BLUE;
        case BLUE: return RED;
        default: return null;
        }
    }
}
