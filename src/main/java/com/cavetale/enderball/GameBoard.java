package com.cavetale.enderball;

import com.cavetale.enderball.struct.Cuboid;
import com.cavetale.enderball.struct.Vec3i;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Data;

@Data
public final class GameBoard implements Serializable {
    private String world = "";
    private Cuboid area = Cuboid.ZERO;
    private Cuboid field = Cuboid.ZERO;
    private Vec3i kickoff = Vec3i.ZERO;
    private List<Cuboid> goals = Arrays.asList(Cuboid.ZERO, Cuboid.ZERO);
    private List<Cuboid> spawns = Arrays.asList(Cuboid.ZERO, Cuboid.ZERO);
    private Vec3i outside = Vec3i.ZERO;

    public void prep() {
        if (!(goals instanceof ArrayList)) {
            goals = new ArrayList<>(goals);
        }
        if (!(spawns instanceof ArrayList)) {
            spawns = new ArrayList<>(spawns);
        }
    }

    public List<Cuboid> getAllAreas() {
        return List.of(area, field, new Cuboid(kickoff, kickoff), goals.get(0), goals.get(1), spawns.get(0), spawns.get(1), new Cuboid(outside, outside));
    }
}
