package haxidenti.mc.hardmc;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Consumer;
import org.bukkit.util.Vector;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

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
            saveConfigEveryNRepeats(plugin, 10, repeats);
            HardMCRules.mainRules(plugin);
        };
    }

    public static void loadConfigFor(HardMC plugin) {
        try {
            plugin.config = HardMCConfig.loadConfig(plugin);
            plugin.getLogger().log(Level.INFO, "Config is loaded successfully");
        } catch (FileNotFoundException e) {
            plugin.config = new HardMCConfig();
            plugin.getLogger().log(Level.INFO, "No Config found so i create new one");
        }
    }

    public static void saveConfigFor(HardMC plugin) {
        try {
            plugin.config.saveConfig(plugin);
            plugin.getLogger().log(Level.INFO, "Config saved!");
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Wasn't able to save the config");
        }
    }

    public static void saveConfigEveryNRepeats(HardMC plugin, int n, AtomicInteger repeats) {
        repeats.incrementAndGet();
        if (repeats.get() % n == 0) saveConfigFor(plugin);
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
        return (digByHardness(plugin, ent, ent.getLocation().add(0, 1, 0), callback) ||
                digByHardness(plugin, ent, ent.getLocation().add(0, 2, 0), callback));
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
            if (digging) mobDigUp(plugin, ent, null);
            asyncRepeat(plugin, 10, 5, i -> towerUp(ent, Material.BIRCH_LEAVES));
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
        return Math.min(
                Math.abs(loc.getX() - loc2.getX()),
                Math.abs(loc.getZ() - loc2.getZ())
        );
    }

    public static void asyncRepeat(Plugin plugin, int delay, int max, Consumer<Integer> c) {
        AtomicInteger i = new AtomicInteger(0);
        Runnable[] _r = new Runnable[1];
        _r[0] = () -> {
            if (i.getAndIncrement() < max) {
                c.accept(i.get());
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
            if (loc.getBlock().isSolid()) continue;
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
        if (isBelow(playerWrapper.player, entity)) mobDigDown(plugin, entity, null);
    }

    public static boolean isDay(World world) {
        long time = world.getTime();
        return time < 12300 || time > 23850;
    }

    public static void teleportRandom(HardMC plugin, PlayerWrapper playerWrapper, int distance, Runnable callback) {
        int x = new Random().nextInt(distance);
        int z = new Random().nextInt(distance);
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            playerWrapper.teleportToTop(x, z);
            if (callback != null) callback.run();
        }, 0);
    }

    public static void giveRequiredItems(PlayerWrapper playerWrapper) {
        playerWrapper.give(
                new ItemStack(Material.STONE_SWORD),
                new ItemStack(Material.STONE_AXE),
                new ItemStack(Material.STONE_PICKAXE),
                new ItemStack(Material.DARK_OAK_WOOD, 8),
                new ItemStack(Material.TORCH, 4),
                new ItemStack(Material.APPLE, 32),
                new ItemStack(Material.COBBLESTONE, 12),
                new ItemStack(Material.CRAFTING_TABLE),
                new ItemStack(Material.FURNACE)
        );
    }
}
