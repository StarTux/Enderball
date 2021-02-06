package com.cavetale.enderball;

import org.bukkit.event.block.Action;

public class Kick {
    public enum Strength {
        SHORT(0.7),
        LONG(1.0);

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
