package haxidenti.mc.hardmc;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.*;

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
        damageWhenDark(playerWrapper, 5, 2);
        actionForMobs(plugin, playerWrapper);
    }

    public static void damageWhenDark(PlayerWrapper playerWrapper, int minimumLight, double damage) {
        int light = playerWrapper.getLightLevel();
        if (light < minimumLight) {
            playerWrapper.damage(damage);
            playerWrapper.sendMessageRed("Too dark here. Go to Light.", light + "/" + minimumLight);
        }
    }

    public static void actionForMobs(HardMC plugin, PlayerWrapper playerWrapper) {
        playerWrapper.getNearEntities(32).stream()
                .filter(e -> e instanceof Monster)
                .map(e -> (Monster) e)
                .forEach(monster -> nearMonsterAction(plugin, monster, playerWrapper));
    }

    public static void nearMonsterAction(HardMC plugin, Monster monster, PlayerWrapper playerWrapper) {
        monster.setTarget(playerWrapper.player);
        towerBuildOrDigIfNeed(plugin, playerWrapper, monster, 3, true, Material.BIRCH_LEAVES);
        if (monster instanceof Creeper c) creeperAction(plugin, c, playerWrapper);
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
