package com.cavetale.enderball.util;

import com.cavetale.enderball.struct.Cuboid;
import com.cavetale.enderball.struct.Vec3i;
import org.bukkit.entity.Player;

public final class WorldEdit {
    private WorldEdit() { }

    public static Cuboid getSelection(Player player) {
        com.cavetale.core.struct.Cuboid tmp = com.cavetale.core.struct.Cuboid.selectionOf(player);
        if (tmp == null) return null;
        return new Cuboid(new Vec3i(tmp.ax, tmp.ay, tmp.az),
                          new Vec3i(tmp.bx, tmp.by, tmp.bz));
    }
}
