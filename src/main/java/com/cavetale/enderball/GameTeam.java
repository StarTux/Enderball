package com.cavetale.enderball;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import static net.kyori.adventure.text.Component.text;

public enum GameTeam {
    RED(ChatColor.RED, NamedTextColor.RED, DyeColor.RED),
    BLUE(ChatColor.BLUE, NamedTextColor.BLUE, DyeColor.LIGHT_BLUE);

    public final ChatColor chatColor;
    public final NamedTextColor textColor;
    public final DyeColor dyeColor;
    public final String humanName;
    public final Component chatBlock;

    GameTeam(final ChatColor chatColor, final NamedTextColor textColor, final DyeColor dyeColor) {
        this.chatColor = chatColor;
        this.textColor = textColor;
        this.dyeColor = dyeColor;
        this.humanName = name().substring(0, 1) + name().substring(1).toLowerCase();
        this.chatBlock = text("\u2588", textColor);
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
