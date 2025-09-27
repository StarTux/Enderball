package com.cavetale.enderball;

import com.cavetale.area.struct.Area;
import com.cavetale.area.struct.AreasFile;
import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec3i;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.bukkit.World;

@Data
public final class GameBoard implements Serializable {
    private Cuboid area = Cuboid.ZERO;
    private Cuboid field = Cuboid.ZERO;
    private Vec3i kickoff = Vec3i.ZERO;
    private List<Cuboid> goals = new ArrayList<>();
    private List<Cuboid> spawns = new ArrayList<>();
    private List<Cuboid> viewers = new ArrayList<>();

    public void load(World world) {
        final AreasFile areasFile = AreasFile.load(world, "Enderball");
        for (Area a : areasFile.find("area")) {
            area = a.toCuboid();
            break;
        }
        for (Area a : areasFile.find("field")) {
            field = a.toCuboid();
            break;
        }
        for (Area a : areasFile.find("kickoff")) {
            kickoff = a.getMin();
            break;
        }
        for (Area a : areasFile.find("goal", "red")) {
            goals.add(a.toCuboid());
            break;
        }
        for (Area a : areasFile.find("goal", "blue")) {
            goals.add(a.toCuboid());
            break;
        }
        for (Area a : areasFile.find("spawn", "red")) {
            spawns.add(a.toCuboid());
            break;
        }
        for (Area a : areasFile.find("spawn", "blue")) {
            spawns.add(a.toCuboid());
            break;
        }
        for (Area a : areasFile.find("viewer")) {
            viewers.add(a.toCuboid());
        }
        if (area.isZero()) throw new IllegalStateException("Missing area: 'area'");
        if (field.isZero()) throw new IllegalStateException("Missing area: 'field'");
        if (kickoff.isZero()) throw new IllegalStateException("Missing area: 'kickoff'");
        if (goals.size() != 2) throw new IllegalStateException("Missing areas: 'goal'" + goals);
        if (spawns.size() != 2) throw new IllegalStateException("Missing areas: 'spawn'" + goals);
        if (viewers.isEmpty()) throw new IllegalStateException("Missing areas: 'viewer'");
    }
}
