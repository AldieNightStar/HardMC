package haxidenti.mc.hardmc;

import org.bukkit.*;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Monster;

import java.util.Random;

import static haxidenti.mc.hardmc.HardMCUtil.*;

public class HardMCRules {
    public static void mainRules(HardMC plugin) {
        // Each online players will have some action
        plugin.getServer().getOnlinePlayers().stream()
                .map(PlayerWrapper::new)
                .filter(HardMCRules::isSurvival)
                .forEach(player -> actionToSurvivalPlayer(plugin, player));
        // Time faster when day for each world
        plugin.getServer().getWorlds().forEach(HardMCRules::makeDayFaster);
    }

    public static void makeDayFaster(World world) {
        if (isDay(world)) {
            world.setTime(world.getTime() + 100);
        }
    }

    public static void actionToSurvivalPlayer(HardMC plugin, PlayerWrapper playerWrapper) {
        damageWhenDark(plugin, playerWrapper, 5, 2);
        actionForMobs(plugin, playerWrapper);
        playerWrapper.tryShowAnger(plugin);
    }

    public static void damageWhenDark(HardMC plugin, PlayerWrapper playerWrapper, int minimumLight, double damage) {
        int light = playerWrapper.getLightLevel();
        if (light < minimumLight) {
            playerWrapper.damage(damage);
            playerWrapper.sendMessageRed("Too dark here. Go to Light.", light + "/" + minimumLight);
            makeNearbyMobsAngry(plugin, playerWrapper, 6, 9);
        }
    }

    public static void actionForMobs(HardMC plugin, PlayerWrapper playerWrapper) {
        playerWrapper.getNearEntities(64).stream()
                .filter(e -> e instanceof Monster)
                .map(e -> (Monster) e)
                .forEach(monster -> nearMonsterAction(plugin, monster, playerWrapper));
    }

    public static void nearMonsterAction(HardMC plugin, Monster monster, PlayerWrapper playerWrapper) {
        // If monster is angry (Hears player)
        MobMem.Mem mem = plugin.mobmem.getMemFor(monster);
        Random random = new Random();
        if (mem.isAngryAt(playerWrapper.player.getLocation())) {
            monster.setTarget(playerWrapper.player);
            // May be monster will calm down by himself
            HardMCUtil.tryCalmDown(random, mem, playerWrapper, monster.getLocation(), .1f);
            // Towering and digging
            towerBuildOrDigIfNeed(plugin, playerWrapper, monster, 3, true, Material.BIRCH_LEAVES);
            // Play sound for angry mobs
            monster.getWorld().playSound(
                    monster, Sound.ENTITY_FOX_SCREECH,
                    random.nextFloat(.75f, 1), random.nextFloat(0.01f, .1f)
            );
            if (monster instanceof Creeper c) creeperAction(plugin, c, playerWrapper);
        }
    }

    public static void creeperAction(HardMC plugin, Creeper creeper, PlayerWrapper playerWrapper) {
        Location playerLoc = playerWrapper.player.getLocation();
        Location creeperLoc = creeper.getLocation();
        // Creeper will explode if he is above player or below to 4 blocks
        if (HardMCUtil.isClose(6, 32, playerLoc, creeperLoc)) {
            if (HardMCUtil.isBelow(playerWrapper.player.getLocation().add(0, -6, 0), creeper.getLocation())) {
                creeper.ignite();
            }
        }
    }

    public static boolean isSurvival(PlayerWrapper playerWrapper) {
        GameMode gameMode = playerWrapper.player.getGameMode();
        return gameMode == GameMode.SURVIVAL || gameMode == GameMode.ADVENTURE;
    }
}
