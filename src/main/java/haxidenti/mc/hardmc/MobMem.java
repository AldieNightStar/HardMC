package haxidenti.mc.hardmc;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class MobMem {
    public HashMap<UUID, Mem> memory = new HashMap<>(128);

    public static class Mem {
        public int allowedDigs = 2;
        public long angerLastTime = 0;

        public boolean isAngry() {
            return Instant.now().toEpochMilli() <= angerLastTime;
        }

        public void makeAngry(int sec) {
            long millis = Instant.now().toEpochMilli() + (1000L * sec);
            if (angerLastTime < millis) {
                angerLastTime = millis;
            }
        }
    }

    public Mem getMemFor(UUID uid) {
        return memory.computeIfAbsent(uid, k -> new Mem());
    }

    public Mem getMemFor(Entity ent) {
        return getMemFor(ent.getUniqueId());
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
