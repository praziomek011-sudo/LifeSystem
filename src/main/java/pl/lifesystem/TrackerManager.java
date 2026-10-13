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
    public static NamespacedKey EXPIRY_KEY;
    public static final String GUI_TITLE = ChatColor.DARK_PURPLE + "Wybierz gracza";
    public static final long TRACKER_DURATION_MS = 10L * 60L * 1000L; // 10 minut

    public TrackerManager(LifeSystem plugin) {
        TRACKER_KEY = new NamespacedKey(plugin, "player_tracker");
        TARGET_KEY = new NamespacedKey(plugin, "tracker_target");
        EXPIRY_KEY = new NamespacedKey(plugin, "tracker_expiry");
    }

    public static ItemStack createTracker() {
        ItemStack item = new ItemStack(Material.RECOVERY_COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Tracker");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Kliknij PPM aby wybrac gracza",
                ChatColor.GRAY + "Pozniej nie da sie zmienic",
                ChatColor.DARK_GRAY + "Tracker dziala 10 minut"
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

    public static long getExpiry(ItemStack item) {
        if (!isTracker(item)) return 0L;
        Long l = item.getItemMeta().getPersistentDataContainer().get(EXPIRY_KEY, PersistentDataType.LONG);
        return l == null ? 0L : l;
    }

    public static boolean isExpired(ItemStack item) {
        long exp = getExpiry(item);
        return exp > 0 && System.currentTimeMillis() >= exp;
    }

    public static void setTarget(ItemStack item, UUID uuid) {
        if (!isTracker(item)) return;
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(TARGET_KEY, PersistentDataType.STRING, uuid.toString());
        meta.getPersistentDataContainer().set(EXPIRY_KEY, PersistentDataType.LONG,
                System.currentTimeMillis() + TRACKER_DURATION_MS);

        String name = Bukkit.getOfflinePlayer(uuid).getName();
        if (name == null) name = "???";
        meta.setDisplayName(ChatColor.AQUA + "Tracker: " + ChatColor.WHITE + name);
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Sledzi: " + ChatColor.WHITE + name,
                ChatColor.DARK_RED + "Zablokowany",
                ChatColor.DARK_GRAY + "Wygasa po 10 minutach"
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

    public void tickAll() {
        Set<UUID> observed = new HashSet<>();

        for (Player holder : Bukkit.getOnlinePlayers()) {
            ItemStack item = holder.getInventory().getItemInMainHand();
            if (!isTracker(item)) continue;

            UUID targetUUID = getTarget(item);
            if (targetUUID == null) continue;

            // Sprawdź timer
            if (isExpired(item)) {
                sendActionBar(holder, ChatColor.DARK_RED + "Tracker wygasl!");
                continue;
            }

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

            // Pozostały czas
            long remainingMs = getExpiry(item) - System.currentTimeMillis();
            long totalSec = remainingMs / 1000L;
            long min = totalSec / 60L;
            long sec = totalSec % 60L;
            String timeStr = String.format("%d:%02d", min, sec);

            String bar = ChatColor.AQUA + "Tracker " + ChatColor.WHITE + target.getName()
                    + ChatColor.GRAY + " | "
                    + ChatColor.YELLOW + "X:" + tLoc.getBlockX()
                    + " Y:" + tLoc.getBlockY()
                    + " Z:" + tLoc.getBlockZ()
                    + ChatColor.GRAY + " | "
                    + ChatColor.GREEN + (int) hLoc.distance(tLoc) + "m"
                    + ChatColor.GRAY + " | "
                    + ChatColor.GOLD + timeStr;
            sendActionBar(holder, bar);

            holder.setCompassTarget(tLoc);
            drawRedBeam(holder, hLoc, tLoc);
        }

        // Śledzeni gracze widzą animowany komunikat
        if (!observed.isEmpty()) {
            String dots = animatedDots();
            String msg = ChatColor.RED + "Czujesz sie obserwowany" + ChatColor.DARK_RED + dots;
            for (UUID uuid : observed) {
                Player target = Bukkit.getPlayer(uuid);
                if (target != null && target.isOnline()) {
                    sendActionBar(target, msg);
                }
            }
        }
    }

    /** Animowane kropki: "", ".", "..", "..." – zmieniają się co 0.5 sekundy. */
    private String animatedDots() {
        int phase = (int) ((System.currentTimeMillis() / 500L) % 4);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < phase; i++) sb.append(".");
        return sb.toString();
    }

    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }

    /**
     * Czerwony promień 10 bloków – widoczny TYLKO dla holdera.
     */
    @SuppressWarnings("deprecation")
    private void drawRedBeam(Player holder, Location from, Location to) {
        Vector dir = to.toVector().subtract(from.toVector());
        dir.setY(0);
        double dist = dir.length();
        if (dist < 0.5) return;
        dir.normalize();

        Location start = from.clone().add(0, 1.2, 0);
        double maxLen = Math.min(10.0, dist);
        double step = 0.4;

        Particle.DustOptions red = new Particle.DustOptions(Color.RED, 1.0f);

        for (double d = 0; d <= maxLen; d += step) {
            Location p = start.clone().add(dir.clone().multiply(d));
            holder.spawnParticle(Particle.DUST, p, 1, 0, 0, 0, 0, red);
        }
    }
}
