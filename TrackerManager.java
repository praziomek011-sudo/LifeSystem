package pl.lifesystem;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.UUID;

public class TrackerManager {

    private final LifeSystem plugin;
    private final LifeManager lifeManager;
    public static NamespacedKey TRACKER_KEY;

    public TrackerManager(LifeSystem plugin) {
        this.plugin = plugin;
        this.lifeManager = plugin.getLifeManager();
        TRACKER_KEY = new NamespacedKey(plugin, "player_tracker");
    }

    /** Tworzy item "Tracker" śledzący konkretnego gracza. */
    public ItemStack createTracker(String targetName, UUID targetUUID) {
        ItemStack item = new ItemStack(Material.RECOVERY_COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Tracker: " + ChatColor.WHITE + targetName);
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Śledzi gracza " + ChatColor.WHITE + targetName,
                ChatColor.GRAY + "Koordynaty: " + ChatColor.YELLOW + "nad hotbarem",
                ChatColor.GRAY + "Ścieżka: " + ChatColor.AQUA + "cząsteczki 10 bloków"
        ));
        meta.getPersistentDataContainer().set(TRACKER_KEY, PersistentDataType.STRING, targetUUID.toString());
        item.setItemMeta(meta);
        return item;
    }

    /** Sprawdza czy item to tracker i zwraca UUID śledzonego gracza (lub null). */
    public static UUID getTrackedUUID(ItemStack item) {
        if (item == null || item.getType() != Material.RECOVERY_COMPASS) return null;
        if (!item.hasItemMeta()) return null;
        String s = item.getItemMeta().getPersistentDataContainer().get(TRACKER_KEY, PersistentDataType.STRING);
        if (s == null) return null;
        try { return UUID.fromString(s); } catch (IllegalArgumentException e) { return null; }
    }

    /** Główna pętla – wywoływana co 5 ticków dla każdego gracza trzymającego tracker. */
    public void tick(Player holder) {
        ItemStack item = holder.getInventory().getItemInMainHand();
        UUID targetUUID = getTrackedUUID(item);
        if (targetUUID == null) return;

        Player target = Bukkit.getPlayer(targetUUID);
        if (target == null || !target.isOnline()) {
            holder.sendActionBar(ChatColor.RED + "Tracker: gracz offline");
            return;
        }
        if (!target.getWorld().equals(holder.getWorld())) {
            holder.sendActionBar(ChatColor.RED + "Tracker: inny wymiar");
            return;
        }

        Location tLoc = target.getLocation();
        Location hLoc = holder.getLocation();

        // 1) Action Bar z koordynatami
        String bar = ChatColor.AQUA + "Tracker " + ChatColor.WHITE + target.getName()
                + ChatColor.GRAY + " | "
                + ChatColor.YELLOW + "X:" + tLoc.getBlockX()
                + " Y:" + tLoc.getBlockY()
                + " Z:" + tLoc.getBlockZ()
                + ChatColor.GRAY + " | "
                + ChatColor.GREEN + (int) hLoc.distance(tLoc) + "m";
        holder.sendActionBar(bar);

        // 2) Kompas wskazuje na gracza
        holder.setCompassTarget(tLoc);

        // 3) Particles – linia 10 bloków w stronę gracza
        drawParticleLine(holder, hLoc, tLoc);
    }

    /** Rysuje linię cząsteczek od gracza w stronę targetu (max 10 bloków). */
    private void drawParticleLine(Player holder, Location from, Location to) {
        World world = from.getWorld();
        if (world == null) return;

        // Wektor od gracza do targetu
        org.bukkit.util.Vector dir = to.toVector().subtract(from.toVector());
        dir.setY(0); // płaska linia – pomijamy pion
        double dist = dir.length();
        if (dist < 0.5) return;
        dir.normalize();

        // Start lekko nad graczem
        Location start = from.clone().add(0, 1.2, 0);

        double maxLen = Math.min(10.0, dist);
        double step = 0.5; // co 0.5 bloku

        for (double d = 0; d <= maxLen; d += step) {
            Location p = start.clone().add(dir.clone().multiply(d));
            world.spawnParticle(
                    Particle.END_ROD,
                    p,
                    1,
                    0, 0, 0,
                    0
            );
        }
    }
}
