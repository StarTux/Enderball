package com.cavetale.enderball;

import lombok.Value;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.util.Vector;

@Value
public class Kick {
    protected final Player player;
    protected final GameBall ball;
    protected final Strength strength;
    protected final Height height;
    protected final Vector vector;

    public enum Strength {
        SHORT(0.5),
        LONG(0.9);

        public final double strength;

        Strength(final double strength) {
            this.strength = strength;
        }
    }

    public enum Height {
        LOW(0.5),
        HIGH(1.0);

        public final double height;

        Height(final double height) {
            this.height = height;
        }

        public static Height of(Action action) {
            switch (action) {
            case LEFT_CLICK_AIR:
            case LEFT_CLICK_BLOCK:
                return Kick.Height.HIGH;
            case RIGHT_CLICK_AIR:
            case RIGHT_CLICK_BLOCK:
            default:
                return Kick.Height.LOW;
            }
        }
    }
}
