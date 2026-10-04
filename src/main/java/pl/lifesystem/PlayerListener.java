package pl.lifesystem;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Date;

public class PlayerListener implements Listener {

    private final LifeSystem plugin;
    private final LifeManager lifeManager;

    public PlayerListener(LifeSystem plugin) {
        this.plugin = plugin;
        this.lifeManager = plugin.getLifeManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        lifeManager.getLives(event.getPlayer());
    }

    /** PPM itemem "Życie" → +1 życie i zużycie itemu. */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!RecipeManager.isLifeItem(item)) return;

        event.setCancelled(true);

        // Dodaj życie
        lifeManager.addLives(player, 1);
        int now = lifeManager.getLives(player);

        // Zużyj 1 sztukę
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        player.sendMessage(ChatColor.GREEN + "❤ Dodano 1 życie! Masz teraz " + ChatColor.YELLOW + now + ChatColor.GREEN + " żyć.");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        boolean playerDeathOnly = plugin.getConfig().getBoolean("player-death-only", false);

        if (playerDeathOnly && player.getKiller() == null) return;

        int remaining = lifeManager.removeLife(player);
        player.sendMessage(ChatColor.RED + "☠ Straciłeś życie! Pozostało: " + ChatColor.YELLOW + remaining + ChatColor.RED + " żyć.");

        if (remaining <= 0) banPlayer(player);
    }

    private void banPlayer(Player player) {
        int banHours = plugin.getConfig().getInt("ban-duration-hours", 48);
        String reason = plugin.getConfig().getString("ban-reason", "Straciłeś wszystkie życia!");
        Date expiry = new Date(System.currentTimeMillis() + (banHours * 60L * 60L * 1000L));

        Bukkit.getBanList(BanList.Type.NAME).addBan(player.getName(), reason, expiry, "LifeSystem");
        player.kickPlayer(ChatColor.DARK_RED + "Zbanowany na " + banHours + "h!\n" + ChatColor.RED + reason);
        Bukkit.broadcastMessage(ChatColor.DARK_RED + "[LifeSystem] " + ChatColor.RED + player.getName() + " stracił wszystkie życia i dostał bana na " + banHours + "h!");
    }
}
