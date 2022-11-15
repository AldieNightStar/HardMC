package haxidenti.mc.hardmc;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import it.unimi.dsi.fastutil.Hash;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

import static haxidenti.mc.hardmc.HardMCUtil.*;

public final class HardMC extends JavaPlugin implements Listener {
    public Random random;

    public MobMem mobmem;

    public HashMap<UUID, PlayerMem> playerMemMap;

    @Override
    public void onEnable() {
        random = new Random();
        mobmem = new MobMem();
        playerMemMap = new HashMap<>();
        registerScheduler(this, 100);
        getServer().getPluginManager().registerEvents(this, this);
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
        makeNearbyMobsAngry(this, new PlayerWrapper(event.getPlayer()), 12, random.nextInt(10, 50));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }
        if (command.getName().equalsIgnoreCase("anger")) {
            PlayerWrapper playerWrapper = new PlayerWrapper((Player) sender);
            PlayerMem playerMem = playerWrapper.getMem(this);
            playerMem.showingAnger = !playerMem.showingAnger;
            String onOff = (playerMem.showingAnger) ? "on" : "off";
            playerWrapper.sendMessageRed("Showing anger mode is", onOff);
            return true;
        }
        return false;
    }

    @EventHandler
    void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (isDaylight(player.getLocation())) return;
        makeNearbyMobsAngry(this, new PlayerWrapper(player), 12, random.nextInt(10, 30));
    }

    @EventHandler
    void onBuild(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (isDaylight(player.getLocation())) return;
        if (random.nextFloat() <= .5f) {
            makeNearbyMobsAngry(this, new PlayerWrapper(player), 12, random.nextInt(6, 50));
        }
    }

    @EventHandler
    void onJump(PlayerJumpEvent event) {
        Player player = event.getPlayer();
        if (event.getPlayer().isSneaking()) return;
        if (isDaylight(player.getLocation())) return;
        if (random.nextFloat() <= .5f) {
            makeNearbyMobsAngry(this, new PlayerWrapper(player), 12, random.nextInt(25, 50));
        }
    }

    @EventHandler
    void mobKill(EntityDeathEvent event) {
        if (event.getEntity() instanceof Monster) {
            Player player = event.getEntity().getKiller();
            if (player != null) makeNearbyMobsAngry(this, new PlayerWrapper(player), 128, random.nextInt(60, 120));
        }
    }

    @EventHandler
    void onHarvest(PlayerHarvestBlockEvent event) {
        Player player = event.getPlayer();
        if (isDaylight(player.getLocation())) return;
        if (random.nextFloat() <= .1f) {
            makeNearbyMobsAngry(this, new PlayerWrapper(player), 6, random.nextInt(10, 20));
        }
    }

    @EventHandler
    void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (isDaylight(player.getLocation())) return;
            if (random.nextFloat() <= .5f) {
                makeNearbyMobsAngry(this, new PlayerWrapper(player), 32, random.nextInt(32, 64));
            }
        }
    }

    @EventHandler
    void playerSpotted(EntityTargetEvent event) {
        if (event.getReason() != EntityTargetEvent.TargetReason.CLOSEST_PLAYER && event.getReason() != EntityTargetEvent.TargetReason.CLOSEST_ENTITY)
            return;
        if (event.getEntity() instanceof Monster monster) {
            Entity target = event.getTarget();
            if (target instanceof Player player && random.nextFloat() <= .5f) {
                PlayerWrapper playerWrapper = new PlayerWrapper(player);
                makeNearbyMobsAngry(this, playerWrapper, 64, random.nextInt(32, 64));
            }
        }
    }

    @EventHandler
    void playerRespawn(PlayerRespawnEvent event) {
        PlayerWrapper playerWrapper = new PlayerWrapper(event.getPlayer());
        if (playerWrapper.player.getBedSpawnLocation() != null) return;
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
