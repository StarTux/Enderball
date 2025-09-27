package com.cavetale.enderball;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import static net.kyori.adventure.text.Component.text;

@Getter
public enum GameTeam {
    RED(NamedTextColor.RED, DyeColor.RED, Color.RED),
    BLUE(NamedTextColor.BLUE, DyeColor.LIGHT_BLUE, Color.BLUE);

    public final NamedTextColor textColor;
    public final DyeColor dyeColor;
    public final Color color;
    public final String humanName;
    public final Component chatBlock;

    GameTeam(final NamedTextColor textColor, final DyeColor dyeColor, final Color color) {
        this.textColor = textColor;
        this.dyeColor = dyeColor;
        this.color = color;
        this.humanName = name().substring(0, 1) + name().substring(1).toLowerCase();
        this.chatBlock = text("\u2588", textColor);
    }

    public GameTeam other() {
        switch (this) {
        case RED: return BLUE;
        case BLUE: return RED;
        default: return null;
        }
    }
}
