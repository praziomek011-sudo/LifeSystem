package pl.lifesystem;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.UUID;

public class TrackerManager {

    private final LifeManager lifeManager;
    public static NamespacedKey TRACKER_KEY;

    public TrackerManager(LifeSystem plugin) {
        this.lifeManager = plugin.getLifeManager();
        TRACKER_KEY = new NamespacedKey(plugin, "player_tracker");
    }

    public ItemStack createTracker(String targetName, UUID targetUUID) {
        ItemStack item = new ItemStack(Material.RECOVERY_COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Tracker: " + ChatColor.WHITE + targetName);
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Sledzi gracza " + ChatColor.WHITE + targetName,
                ChatColor.GRAY + "Koordynaty nad hotbarem",
                ChatColor.GRAY + "Sciezka z czasteczek: 10 blokow"
        ));
        meta.getPersistentDataContainer().set(TRACKER_KEY, PersistentDataType.STRING, targetUUID.toString());
        item.setItemMeta(meta);
        return item;
    }

    public static UUID getTrackedUUID(ItemStack item) {
        if (item == null || item.getType() != Material.RECOVERY_COMPASS) return null;
        if (!item.hasItemMeta()) return null;
        String s = item.getItemMeta().getPersistentDataContainer().get(TRACKER_KEY, PersistentDataType.STRING);
        if (s == null) return null;
        try { return UUID.fromString(s); } catch (IllegalArgumentException e) { return null; }
    }

    public void tick(Player holder) {
        ItemStack item = holder.getInventory().getItemInMainHand();
        UUID targetUUID = getTrackedUUID(item);
        if (targetUUID == null) return;

        Player target = Bukkit.getPlayer(targetUUID);
        if (target == null || !target.isOnline()) {
            sendActionBar(holder, ChatColor.RED + "Tracker: gracz offline");
            return;
        }
        if (!target.getWorld().equals(holder.getWorld())) {
            sendActionBar(holder, ChatColor.RED + "Tracker: inny wymiar");
            return;
        }

        Location tLoc = target.getLocation();
        Location hLoc = holder.getLocation();

        String bar = ChatColor.AQUA + "Tracker " + ChatColor.WHITE + target.getName()
                + ChatColor.GRAY + " | "
                + ChatColor.YELLOW + "X:" + tLoc.getBlockX()
                + " Y:" + tLoc.getBlockY()
                + " Z:" + tLoc.getBlockZ()
                + ChatColor.GRAY + " | "
                + ChatColor.GREEN + (int) hLoc.distance(tLoc) + "m";
        sendActionBar(holder, bar);

        holder.setCompassTarget(tLoc);
        drawParticleLine(holder, hLoc, tLoc);
    }

    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }

    private void drawParticleLine(Player holder, Location from, Location to) {
        World world = from.getWorld();
        if (world == null) return;

        Vector dir = to.toVector().subtract(from.toVector());
        dir.setY(0);
        double dist = dir.length();
        if (dist < 0.5) return;
        dir.normalize();

        Location start = from.clone().add(0, 1.2, 0);
        double maxLen = Math.min(10.0, dist);
        double step = 0.5;

        for (double d = 0; d <= maxLen; d += step) {
            Location p = start.clone().add(dir.clone().multiply(d));
            world.spawnParticle(Particle.END_ROD, p, 1, 0, 0, 0, 0);
        }
    }
}
