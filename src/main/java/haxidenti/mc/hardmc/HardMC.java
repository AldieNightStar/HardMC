package haxidenti.mc.hardmc;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static haxidenti.mc.hardmc.HardMCUtil.*;

public final class HardMC extends JavaPlugin implements Listener {

    public HardMCConfig config;
    public Random random;

    public MobMem mobmem;

    @Override
    public void onEnable() {
        random = new Random();
        loadConfigFor(this);
        registerScheduler(this, 100);
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        saveConfigFor(this);
    }

    @EventHandler
    void playerBed(PlayerInteractEvent evt) {
        if (!evt.getAction().isRightClick()) return;
        Block clickedBlock = evt.getClickedBlock();
        if (clickedBlock == null) return;
        if (!clickedBlock.getType().toString().endsWith("BED")) return;
        evt.getPlayer().setBedSpawnLocation(evt.getClickedBlock().getLocation());
        evt.setCancelled(true);
        evt.getPlayer().sendMessage("Bed Location set");
    }

    @EventHandler
    void playerRespawn(PlayerRespawnEvent event) {
        // Set time to day if only single player on the server
        getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
            Collection<? extends Player> players = getServer().getOnlinePlayers();
            if (players.size() < 2) {
                players.stream().findFirst().ifPresent(p -> {
                    p.getWorld().setTime(1000);
                });
            }
        });

        // Teleport randomly
        // Exception is when player has a bed spawn location
        PlayerWrapper playerWrapper = new PlayerWrapper(event.getPlayer());
        if (playerWrapper.player.getBedSpawnLocation() != null) return;
        teleportRandom(this, playerWrapper, 320000, () -> {
            playerWrapper.getNearEntities(12).stream()
                    // Kill all nearby monsters
                    .filter(e -> e instanceof Monster)
                    .map(e -> (Monster) e)
                    .forEach(m -> m.damage(1000));
            HardMCUtil.giveRequiredItems(playerWrapper);
        });
    }
}
