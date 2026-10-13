package pl.lifesystem;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.*;

public class TrackerManager {

    public static NamespacedKey TRACKER_KEY;
    public static NamespacedKey TARGET_KEY;
    public static final String GUI_TITLE = ChatColor.DARK_PURPLE + "Wybierz gracza";

    public TrackerManager(LifeSystem plugin) {
        TRACKER_KEY = new NamespacedKey(plugin, "player_tracker");
        TARGET_KEY = new NamespacedKey(plugin, "tracker_target");
    }

    public static ItemStack createTracker() {
        ItemStack item = new ItemStack(Material.RECOVERY_COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Tracker");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Kliknij PPM aby wybrac gracza",
                ChatColor.GRAY + "Pozniej nie da sie zmienic"
        ));
        meta.getPersistentDataContainer().set(TRACKER_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isTracker(ItemStack item) {
        if (item == null || item.getType() != Material.RECOVERY_COMPASS) return false;
        if (!item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(TRACKER_KEY, PersistentDataType.BYTE);
    }

    public static UUID getTarget(ItemStack item) {
        if (!isTracker(item)) return null;
        String s = item.getItemMeta().getPersistentDataContainer().get(TARGET_KEY, PersistentDataType.STRING);
        if (s == null) return null;
        try { return UUID.fromString(s); } catch (IllegalArgumentException e) { return null; }
    }

    public static void setTarget(ItemStack item, UUID uuid) {
        if (!isTracker(item)) return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(TARGET_KEY, PersistentDataType.STRING, uuid.toString());
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        if (name == null) name = "???";
        meta.setDisplayName(ChatColor.AQUA + "Tracker: " + ChatColor.WHITE + name);
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Sledzi: " + ChatColor.WHITE + name,
                ChatColor.DARK_RED + "Zablokowany"
        ));
        item.setItemMeta(meta);
    }

    public void openSelectGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, GUI_TITLE);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) continue;
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(online);
            meta.setDisplayName(ChatColor.YELLOW + online.getName());
            meta.setLore(Arrays.asList(ChatColor.GRAY + "Kliknij aby sledzic"));
            head.setItemMeta(meta);
            inv.addItem(head);
        }
        player.openInventory(inv);
    }

    /** Wywoływane co 5 ticków – sprawdza wszystkich graczy trzymających tracker. */
    public void tickAll() {
        Set<UUID> observed = new HashSet<>();

        for (Player holder : Bukkit.getOnlinePlayers()) {
            ItemStack item = holder.getInventory().getItemInMainHand();
            if (!isTracker(item)) continue;

            UUID targetUUID = getTarget(item);
            if (targetUUID == null) continue;

            Player target = Bukkit.getPlayer(targetUUID);
            if (target == null || !target.isOnline()) {
                sendActionBar(holder, ChatColor.RED + "Tracker: gracz offline");
                continue;
            }
            if (!target.getWorld().equals(holder.getWorld())) {
                sendActionBar(holder, ChatColor.RED + "Tracker: inny wymiar");
                continue;
            }

            observed.add(targetUUID);

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
            drawRedBeam(holder, hLoc, tLoc);
        }

        // Śledzeni gracze widzą komunikat
        for (UUID uuid : observed) {
            Player target = Bukkit.getPlayer(uuid);
            if (target != null && target.isOnline()) {
                sendActionBar(target, ChatColor.RED + "Czujesz sie obserwowany...");
            }
        }
    }

    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }

    /** Czerwony promień particles, 10 bloków, płasko w stronę targetu. */
    private void drawRedBeam(Player holder, Location from, Location to) {
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

        Particle.DustOptions red = new Particle.DustOptions(Color.RED, 1.2f);

        for (double d = 0; d <= maxLen; d += step) {
            Location p = start.clone().add(dir.clone().multiply(d));
            world.spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, red);
        }
    }
}
