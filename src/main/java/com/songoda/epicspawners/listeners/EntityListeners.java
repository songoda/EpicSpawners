package com.songoda.epicspawners.listeners;

import com.songoda.epicspawners.EpicSpawners;
import com.songoda.epicspawners.spawners.spawner.Spawner;
import com.songoda.epicspawners.spawners.spawner.SpawnerData;
import com.songoda.epicspawners.spawners.spawner.SpawnerStack;
import com.songoda.epicspawners.utils.ServerVersion;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by songoda on 2/25/2017.
 */
public class EntityListeners implements Listener {

    private final EpicSpawners plugin;

    public EntityListeners(EpicSpawners plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlow(EntityExplodeEvent event) {
        List<Block> destroyed = event.blockList();
        Iterator<Block> it = destroyed.iterator();
        List<Block> toCancel = new ArrayList<>();
        while (it.hasNext()) {
            Block block = it.next();
            if (block.getType() != (plugin.isServerVersionAtLeast(ServerVersion.V1_13) ? Material.SPAWNER : Material.valueOf("MOB_SPAWNER")))
                continue;

            Location spawnLocation = block.getLocation();

            Spawner spawner = plugin.getSpawnerManager().getSpawnerFromWorld(block.getLocation());

            if (plugin.getConfig().getBoolean("Main.Prevent Spawners From Exploding"))
                toCancel.add(block);
            else if (event.getEntity() instanceof Creeper && plugin.getConfig().getBoolean("Spawner Drops.Drop On Creeper Explosion")
                    || event.getEntity() instanceof TNTPrimed && plugin.getConfig().getBoolean("Spawner Drops.Drop On TNT Explosion")) {

                String chance = "";
                if (event.getEntity() instanceof Creeper && plugin.getConfig().getBoolean("Spawner Drops.Drop On Creeper Explosion"))
                    chance = plugin.getConfig().getString("Spawner Drops.Chance On TNT Explosion");
                else if (event.getEntity() instanceof TNTPrimed && plugin.getConfig().getBoolean("Spawner Drops.Drop On TNT Explosion"))
                    chance = plugin.getConfig().getString("Spawner Drops.Chance On Creeper Explosion");
                int ch = Integer.parseInt(chance.replace("%", ""));
                double rand = Math.random() * 100;
                if (rand - ch < 0 || ch == 100) {
                    for (SpawnerStack stack : spawner.getSpawnerStacks()) {
                        ItemStack item = stack.getSpawnerData().toItemStack(1, stack.getStackSize());
                        spawnLocation.getWorld().dropItemNaturally(spawnLocation.clone().add(.5, 0, .5), item);
                    }
                }
            }
            plugin.getSpawnerManager().removeSpawnerFromWorld(spawnLocation);
            if (plugin.getHologram() != null)
                plugin.getHologram().remove(spawner);
            if (spawner != null) plugin.getAppearanceHandler().removeDisplayItem(spawner);

            Location nloc = spawnLocation.clone();
            nloc.add(.5, -.4, .5);
            List<Entity> near = (List<Entity>) nloc.getWorld().getNearbyEntities(nloc, 8, 8, 8);
            for (Entity ee : near) {
                if (ee.getLocation().getX() == nloc.getX() && ee.getLocation().getY() == nloc.getY() && ee.getLocation().getZ() == nloc.getZ()) {
                    ee.remove();
                }
            }

        }

        for (Block block : toCancel) {
            event.blockList().remove(block);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (event.getEntity().hasMetadata("ES")) {
            SpawnerData spawnerData = plugin.getSpawnerManager().getSpawnerData(event.getEntity().getMetadata("ES").get(0).asString());
            if (!spawnerData.getEntityDroppedItems().isEmpty()) {
                event.getDrops().clear();
            }
            for (ItemStack itemStack : spawnerData.getEntityDroppedItems()) {
                event.getDrops().add(itemStack);
            }
        }
        if (event.getEntity().getKiller() == null) return;
        Player player = event.getEntity().getKiller();
        if (!player.hasPermission("epicspawners.Killcounter") || !plugin.getConfig().getBoolean("Spawner Drops.Allow Killing Mobs To Drop Spawners"))
            return;

        if (!plugin.getSpawnManager().isNaturalSpawn(event.getEntity().getUniqueId()) && !plugin.getConfig().getBoolean("Spawner Drops.Count Unnatural Kills Towards Spawner Drop"))
            return;


        if (!plugin.getSpawnerManager().getSpawnerData(event.getEntityType()).isActive()) return;

        int amt = plugin.getPlayerActionManager().getPlayerAction(player).addKilledEntity(event.getEntityType());
        int goal = plugin.getConfig().getInt("Spawner Drops.Kills Needed for Drop");

        SpawnerData spawnerData = plugin.getSpawnerManager().getSpawnerData(event.getEntityType());

        if (!spawnerData.isActive()) return;

        int customGoal = spawnerData.getKillGoal();
        if (customGoal != 0) goal = customGoal;

        if (plugin.getConfig().getInt("Spawner Drops.Alert Every X Before Drop") != 0
                && amt % plugin.getConfig().getInt("Spawner Drops.Alert Every X Before Drop") == 0
                && amt != goal) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(plugin.getLocale().getMessage("event.goal.alert", goal - amt, spawnerData.getIdentifyingName())));
        }

        if (amt >= goal) {
            ItemStack item = spawnerData.toItemStack();
            event.getEntity().getLocation().getWorld().dropItemNaturally(event.getEntity().getLocation(), item);
            plugin.getPlayerActionManager().getPlayerAction(player).removeEntity(event.getEntityType());
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(plugin.getLocale().getMessage("event.goal.reached", spawnerData.getIdentifyingName())));
        }
    }
}