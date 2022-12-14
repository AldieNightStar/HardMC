package haxidenti.mc.hardmc;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Consumer;
import org.bukkit.util.Vector;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class HardMCUtil {
    public static void registerScheduler(HardMC plugin, int delay) {
        BukkitScheduler scheduler = plugin.getServer().getScheduler();
        scheduler.scheduleSyncRepeatingTask(plugin, getScheduledTask(plugin), delay, delay);
        cleanTaskForMobs(plugin, scheduler);
    }

    public static void cleanTaskForMobs(HardMC plugin, BukkitScheduler scheduler) {
        scheduler.scheduleSyncRepeatingTask(plugin, () -> {
            plugin.mobmem.cleanProcess();
        }, 200, 200);
    }

    public static Runnable getScheduledTask(HardMC plugin) {
        AtomicInteger repeats = new AtomicInteger();
        return () -> {
            HardMCRules.mainRules(plugin);
            repeats.incrementAndGet();
        };
    }

    public static boolean isClose(int horizontalDistance, int verticalDistance, Location loc1, Location loc2) {
        double x = Math.abs(loc1.getX() - loc2.getX());
        double y = Math.abs(loc1.getY() - loc2.getY());
        double z = Math.abs(loc1.getZ() - loc2.getZ());
        return (x <= horizontalDistance || z <= horizontalDistance) && y <= verticalDistance;
    }

    public static Block getBlockLooking(int maxDistance, LivingEntity ent) {
        Location loc = ent.getEyeLocation();
        Vector dir = loc.getDirection();
        Block block = loc.getBlock();
        for (int i = 0; i < maxDistance; i++) {
            if (block.getType().isSolid()) {
                return block;
            }
            loc.add(dir);
            block = loc.getBlock();
        }
        return block;
    }

    public static void mobDigging(HardMC plugin, LivingEntity ent, int distance) {
        Block blockLooking = HardMCUtil.getBlockLooking(distance, ent);
        // Break block and block below
        digByHardness(plugin, ent, blockLooking.getLocation(), () -> {
            digByHardness(plugin, ent, blockLooking.getLocation().add(0, -1, 0), null);
        });
    }

    public static void mobDigDown(HardMC plugin, LivingEntity ent, Runnable callback) {
        digByHardness(plugin, ent, ent.getLocation().add(0, -1, 0), callback);
    }

    public static boolean mobDigUp(HardMC plugin, LivingEntity ent, Runnable callback) {
        return (digByHardness(plugin, ent, ent.getLocation().add(0, 1, 0), callback) || digByHardness(plugin, ent, ent.getLocation().add(0, 2, 0), callback));
    }

    public static boolean digByHardness(HardMC plugin, LivingEntity ent, Location loc, Runnable callback) {
        Block block = loc.getBlock();
        if (!block.isSolid()) return false;
        if (block.getType().getHardness() > 100) return false;
        float hardness = block.getType().getHardness();
        // Don't allow to dig again while limit
        MobMem.Mem mobMem = plugin.mobmem.getMemFor(ent.getUniqueId());
        if (mobMem.allowedDigs < 1) return false;
        // Add limit for digs
        mobMem.allowedDigs -= 1;
        // Scheduler
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            // Cancel if mob dead, block is already broken or when position changed already
            if (ent.isDead()) {
                mobMem.allowedDigs += 1;
                return;
            }
            if (!block.isSolid()) {
                mobMem.allowedDigs += 1;
                return;
            }
            if (block.getLocation().distance(ent.getLocation()) > 3) {
                mobMem.allowedDigs += 1;
                return;
            }
            // Breaking
            block.breakNaturally(new ItemStack(Material.WOODEN_PICKAXE), true);
            // Allow to dig again
            mobMem.allowedDigs += 1;
            // Callback
            if (callback != null) callback.run();
        }, (long) hardness * 20);
        return true;
    }

    public static boolean towerUp(LivingEntity ent, Material material) {
        if (!ent.getLocation().add(0, 1, 0).getBlock().isSolid() && !ent.getLocation().add(0, 2, 0).getBlock().isSolid()) {
            ent.getLocation().getBlock().setType(material);
            ent.teleport(ent.getLocation().add(0, 1, 0));
            return true;
        }
        return false;
    }

    public static void towerUpIfNeed(HardMC plugin, LivingEntity ent, PlayerWrapper playerWrapper, boolean digging) {
        if (isBelowAndHorizontallyClose(ent, playerWrapper.player, 6)) {
            MobMem.Mem mem = plugin.mobmem.getMemFor(ent);
            asyncWhile(plugin, 10,
                    () -> ent.getLocation().getY() <= playerWrapper.player.getLocation().getY() && mem.isAngryAt(playerWrapper.player.getLocation()),
                    () -> {
                        mobDigUp(plugin, ent, null);
                        towerUp(ent, Material.BIRCH_LEAVES);
                    });
        }
    }

    public static boolean isBelow(Entity entity1, Entity entity2) {
        return entity1.getLocation().getBlockY() < entity2.getLocation().getBlockY();
    }

    public static boolean isBelow(Location loc1, Location loc2) {
        return loc1.getBlockY() < loc2.getBlockY();
    }

    public static boolean isBelowAndHorizontallyClose(Entity entity1, Entity entity2, int horDistance) {
        return isBelow(entity1, entity2) && horizontalDistance(entity1.getLocation(), entity2.getLocation()) <= horDistance;
    }

    public static double horizontalDistance(Location loc, Location loc2) {
        return Math.min(Math.abs(loc.getX() - loc2.getX()), Math.abs(loc.getZ() - loc2.getZ()));
    }

    public static void asyncRepeat(Plugin plugin, int delay, int max, Consumer<Integer> c) {
        AtomicInteger counter = new AtomicInteger(0);
        asyncWhile(plugin, delay, () -> counter.getAndIncrement() < max, () -> c.accept(counter.get()));
    }

    public static void asyncWhile(Plugin plugin, int delay, Supplier<Boolean> bool, Runnable runnable) {
        Runnable[] _r = new Runnable[1];
        _r[0] = () -> {
            if (bool.get()) {
                runnable.run();
                plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, _r[0], delay);
            }
        };
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, _r[0], delay);
    }

    public static void buildBridge(LivingEntity ent, int distance, Material material) {
        Vector dir = ent.getEyeLocation().getDirection();
        Location loc = ent.getLocation().add(0, -1, 0);
        dir.setY(0);
        for (int i = 0; i < distance; i++) {
            loc.add(dir);
            if (loc.getBlock().isSolid()) break;
            loc.getBlock().setType(material);
        }
    }

    public static void buildBridgeIfNeed(PlayerWrapper playerWrapper, LivingEntity entity, int distance, Material material) {
        int playerY = (int) playerWrapper.player.getLocation().getY();
        int entY = (int) entity.getLocation().getY();
        if (entY >= playerY - 1 && entY <= playerY + 3) {
            buildBridge(entity, 3, material);
        }
    }

    public static void towerBuildOrDigIfNeed(HardMC plugin, PlayerWrapper playerWrapper, LivingEntity entity, int distance, boolean dig, Material material) {
        buildBridgeIfNeed(playerWrapper, entity, distance, material);
        mobDigging(plugin, entity, distance);
        towerUpIfNeed(plugin, entity, playerWrapper, dig);
        if (isBelow(playerWrapper.player, entity)) {
            HardMCUtil.asyncWhile(plugin, 20, () -> isBelow(playerWrapper.player, entity), () -> mobDigDown(plugin, entity, null));
        }
    }

    public static boolean isDay(World world) {
        long time = world.getTime();
        return time < 12300 || time > 23850;
    }

    public static void teleportRandom(HardMC plugin, PlayerWrapper playerWrapper, int distance, Runnable callback) {
        AtomicReference<Location> locRef = new AtomicReference<>(playerWrapper.player.getLocation());
        Random random = new Random();
        for (int i = 0; i < 9; i++) {
            // Pick random position
            int x = random.nextInt(distance);
            int z = random.nextInt(distance);
            locRef.set(playerWrapper.getTopOfXZ(x, z));
            // Do not allow to spawn in the Ocean
            if (locRef.get().getWorld().getBiome(locRef.get()).name().endsWith("OCEAN")) continue;

            break;
        }
        // Teleport
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            playerWrapper.player.teleport(locRef.get());
            if (callback != null) callback.run();
        }, 0);
    }

    public static void giveRequiredItems(PlayerWrapper playerWrapper) {
        playerWrapper.give(new ItemStack(Material.STONE_SWORD), new ItemStack(Material.STONE_AXE), new ItemStack(Material.STONE_PICKAXE), new ItemStack(Material.DARK_OAK_WOOD, 8), new ItemStack(Material.TORCH, 4), new ItemStack(Material.APPLE, 32), new ItemStack(Material.COBBLESTONE, 12), new ItemStack(Material.CRAFTING_TABLE), new ItemStack(Material.FURNACE));
    }

    public static void makeNearbyMobsAngry(HardMC plugin, PlayerWrapper wrapper, int distance, int seconds) {
        AtomicInteger count = new AtomicInteger();
        Random random = new Random();
        wrapper.getNearEntities(distance).stream()
                .filter(m -> m instanceof Monster)
                .map(m -> (Monster) m)
                .filter(m -> !plugin.mobmem.getMemFor(m).isAngryAt(wrapper.player.getLocation()))
                .forEach(mob -> {
                    count.getAndIncrement();
                    MobMem.Mem mem = plugin.mobmem.getMemFor(mob);
                    plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                        mob.getWorld().playSound(mob, Sound.ENTITY_FOX_SCREECH, random.nextFloat(1, 3), .1f);
                    }, random.nextInt(1, 300));
                    mem.makeAngry(seconds, wrapper.player.getLocation());
                });
        if (count.get() > 5) {
            wrapper.sendMessageRed("A lot of Monsters getting angry:", "" + count.get());
        }
    }

    public static boolean isAngryAt(HardMC plugin, Monster monster, Location location) {
        return plugin.mobmem.getMemFor(monster).isAngryAt(location);
    }

    public static boolean isDaylight(Location location) {
        return location.getBlock().getLightFromSky() > 6;
    }

    public static long getAngryMobsCount(HardMC plugin, PlayerWrapper playerWrapper) {
        return playerWrapper.getNearEntities(128)
                .stream()
                .filter(e -> e instanceof Monster)
                .map(e -> (Monster) e)
                .filter(m -> isAngryAt(plugin, m, playerWrapper.player.getLocation()))
                .count();
    }

    public static void tryCalmDown(Random random, MobMem.Mem mem, PlayerWrapper playerWrapper, Location monsterLoc, float chance) {
        if (playerWrapper.player.getLocation().distance(monsterLoc) < 12) return;
        if (random.nextFloat() > chance) return;
        if (!playerWrapper.isSneaking()) return;
        mem.makePeaceful();
    }
}
