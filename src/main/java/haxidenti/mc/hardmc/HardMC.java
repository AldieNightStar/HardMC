package haxidenti.mc.hardmc;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Random;

import static haxidenti.mc.hardmc.HardMCUtil.*;

public final class HardMC extends JavaPlugin implements Listener {

    public HardMCConfig config;
    public Random random;

    public MobMem mobmem;

    @Override
    public void onEnable() {
        random = new Random();
        mobmem = new MobMem();
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
    void onBreak(PlayerItemBreakEvent event) {
        Player player = event.getPlayer();
        if (isDaylight(player.getLocation())) return;
        makeNearbyMobsAngry(this, new PlayerWrapper(event.getPlayer()), 32);
    }

    @EventHandler
    void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (isDaylight(player.getLocation())) return;
        makeNearbyMobsAngry(this, new PlayerWrapper(player), 12);
    }

    @EventHandler
    void onBuild(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (isDaylight(player.getLocation())) return;
        makeNearbyMobsAngry(this, new PlayerWrapper(player), 12);
    }

    @EventHandler
    void onJump(PlayerJumpEvent event) {
        Player player = event.getPlayer();
        if (event.getPlayer().isSneaking()) return;
        if (isDaylight(player.getLocation())) return;
        makeNearbyMobsAngry(this, new PlayerWrapper(player), 12);
    }

    @EventHandler
    void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (isDaylight(player.getLocation())) return;
        if (event.getAction() == Action.LEFT_CLICK_BLOCK)
            makeNearbyMobsAngry(this, new PlayerWrapper(player), 32);
    }

    @EventHandler
    void mobKill(EntityDeathEvent event) {
        if (event.getEntity() instanceof Monster) {
            Player player = event.getEntity().getKiller();
            if (player != null) makeNearbyMobsAngry(this, new PlayerWrapper(player), 64);
        }
    }

    @EventHandler
    void onHarvest(PlayerHarvestBlockEvent event) {
        Player player = event.getPlayer();
        if (isDaylight(player.getLocation())) return;
        makeNearbyMobsAngry(this, new PlayerWrapper(player), 32);
    }

    @EventHandler
    void playerChest(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            if (isDaylight(player.getLocation())) return;
            if (event.getPlayer() instanceof Player p)
                makeNearbyMobsAngry(this, new PlayerWrapper(p), 32);
        }
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
