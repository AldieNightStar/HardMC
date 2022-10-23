package haxidenti.mc.hardmc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class MobMem {
    public static HashMap<UUID, Mem> memory = new HashMap<>(128);

    public static class Mem {
        public int allowedDigs = 2;
    }

    public Mem getMemFor(UUID uid) {
        return memory.computeIfAbsent(uid, k -> new Mem());
    }

    public void cleanProcess() {
        List<UUID> toRemove = new ArrayList<>(32);
        memory.forEach((uid, v) -> {
            Entity ent = Bukkit.getEntity(uid);
            if (ent == null || ent.isDead()) {
                toRemove.add(uid);
            }
        });
        toRemove.forEach(uid -> memory.remove(uid));
    }
}
