package haxidenti.mc.hardmc;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

import static haxidenti.mc.hardmc.HardMCUtil.getAngryMobsCount;

public class PlayerWrapper {
    public Player player;

    public PlayerWrapper(Player player) {
        this.player = player;
    }

    public static PlayerWrapper getPlayer(HardMC plugin, String uid) {
        Player player = plugin.getServer().getPlayer(UUID.fromString(uid));
        if (player == null || !player.isOnline()) return null;
        return new PlayerWrapper(player);
    }

    public int getLightLevel() {
        return player.getLocation().getBlock().getLightLevel();
    }

    public void damage(double val) {
        player.damage(val);
    }

    public void sendMessage(String message) {
        player.sendMessage(ChatColor.RED + "HardMC: " + ChatColor.WHITE + message);
    }

    public void sendMessageRed(String message, String redMessage) {
        sendMessage(message + " " + ChatColor.RED + redMessage);
    }

    public List<Entity> getNearEntities(int distance) {
        return player.getNearbyEntities(distance, (double) distance, distance);
    }

    public Location getTopOfXZ(int x, int z) {
        Location loc = player.getLocation();
        loc.setX(x);
        loc.setZ(z);
        loc.setY(loc.getWorld().getHighestBlockYAt(x, z) + 1);
        return loc;
    }

    public void give(ItemStack item) {
        Location loc = player.getLocation().add(0, 1, 0);
        loc.getWorld().dropItem(loc, item);
    }

    public void give(ItemStack... items) {
        Location loc = player.getLocation().add(0, 1, 0);
        for (ItemStack item : items) {
            loc.getWorld().dropItem(loc, item);
        }
    }

    public boolean isSneaking() {
        return player.isSneaking();
    }

    public PlayerMem getMem(HardMC plugin) {
        return plugin.playerMemMap.computeIfAbsent(player.getUniqueId(), k -> new PlayerMem());
    }

    public void showAnger(HardMC plugin) {
        sendMessageRed("/anger", "Angry: " + getAngryMobsCount(plugin, this));
    }

    public void tryShowAnger(HardMC plugin) {
        if (getMem(plugin).showingAnger) {
            showAnger(plugin);
        }
    }
}
