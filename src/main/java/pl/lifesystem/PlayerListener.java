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
        Player player = event.getPlayer();

        // Czy wraca po banie? Ustaw życia na lives-after-ban.
        if (lifeManager.isBanPending(player)) {
            lifeManager.clearBanPending(player);
            int afterBan = lifeManager.getLivesAfterBan();
            lifeManager.setLives(player, afterBan);
            player.sendMessage(ChatColor.GREEN + "❤ Witaj z powrotem! Otrzymujesz " + ChatColor.YELLOW + afterBan + ChatColor.GREEN + " żyć po banie.");
            return;
        }

        lifeManager.getLives(player); // upewnij się, że ma wpis
    }

    /** PPM itemem "Życie" → +1 życie. */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!RecipeManager.isLifeItem(item)) return;

        event.setCancelled(true);

        int current = lifeManager.getLives(player);
        int max = lifeManager.getMaxLives();

        if (current >= max) {
            player.sendMessage(ChatColor.YELLOW + "Posiadasz maksymalną ilość ŻYĆ");
            return;
        }

        lifeManager.addLives(player, 1);
        int now = lifeManager.getLives(player);

        if (item.getAmount() > 1) item.setAmount(item.getAmount() - 1);
        else player.getInventory().setItemInMainHand(null);

        player.sendMessage(ChatColor.GREEN + "❤ Dodano 1 życie! Masz teraz " + ChatColor.YELLOW + now + ChatColor.GREEN + " żyć.");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        boolean playerDeathOnly = plugin.getConfig().getBoolean("player-death-only", false);

        // Tryb "tylko śmierć od gracza" i nie ma killera → nic się nie dzieje
        if (playerDeathOnly && killer == null) return;

        // Ofiara traci życie
        int remaining = lifeManager.removeLife(victim);
        victim.sendMessage(ChatColor.RED + "☠ Straciłeś życie! Pozostało: " + ChatColor.YELLOW + remaining + ChatColor.RED + " żyć.");

        if (remaining <= 0) {
            banPlayer(victim);
        }

        // Zabójca dostaje życie (jeśli jest i to nie samobójstwo)
        if (killer != null && !killer.equals(victim)) {
            int killerLives = lifeManager.getLives(killer);
            int max = lifeManager.getMaxLives();

            if (killerLives >= max) {
                killer.sendMessage(ChatColor.YELLOW + "Posiadasz maksymalną ilość ŻYĆ");
            } else {
                lifeManager.addLives(killer, 1);
                killer.sendMessage(ChatColor.GREEN + "❤ Zabójstwo! Zdobyłeś życie. Masz teraz " + ChatColor.YELLOW + (killerLives + 1) + ChatColor.GREEN + " żyć.");
            }
        }
    }

    private void banPlayer(Player player) {
        int banHours = plugin.getConfig().getInt("ban-duration-hours", 48);
        String reason = plugin.getConfig().getString("ban-reason", "Straciłeś wszystkie życia!");
        Date expiry = new Date(System.currentTimeMillis() + (banHours * 60L * 60L * 1000L));

        lifeManager.markBanPending(player);
        Bukkit.getBanList(BanList.Type.NAME).addBan(player.getName(), reason, expiry, "LifeSystem");
        player.kickPlayer(ChatColor.DARK_RED + "Zbanowany na " + banHours + "h!\n" + ChatColor.RED + reason);
        Bukkit.broadcastMessage(ChatColor.DARK_RED + "[LifeSystem] " + ChatColor.RED + player.getName() + " stracił wszystkie życia i dostał bana na " + banHours + "h!");
    }
}
