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
    private final NameTagManager nameTagManager;

    public PlayerListener(LifeSystem plugin) {
        this.plugin = plugin;
        this.lifeManager = plugin.getLifeManager();
        this.nameTagManager = new NameTagManager(plugin);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (lifeManager.isBanPending(player)) {
            lifeManager.clearBanPending(player);
            int after = lifeManager.getLivesAfterBan();
            lifeManager.setLives(player, after);
            player.sendMessage(ChatColor.GREEN + "Witaj z powrotem! Otrzymujesz " + after + " zyc po banie.");
        } else {
            lifeManager.getLives(player);
        }

        // Odswiez suffix po 1 ticku (tablista bywa jeszcze nie gotowa)
        Bukkit.getScheduler().runTaskLater(plugin, () -> nameTagManager.updatePlayer(player), 20L);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!RecipeManager.isLifeItem(item)) return;

        event.setCancelled(true);

        int current = lifeManager.getLives(player);
        int max = lifeManager.getMaxLives();

        if (current >= max) {
            player.sendMessage(ChatColor.YELLOW + "Posiadasz maksymalna ilosc ZYC");
            return;
        }

        lifeManager.addLives(player, 1);
        int now = lifeManager.getLives(player);

        if (item.getAmount() > 1) item.setAmount(item.getAmount() - 1);
        else player.getInventory().setItemInMainHand(null);

        player.sendMessage(ChatColor.GREEN + "Dodano 1 zycie! Masz teraz " + ChatColor.YELLOW + now + ChatColor.GREEN + " zyc.");
        nameTagManager.updatePlayer(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        boolean playerOnly = plugin.getConfig().getBoolean("player-death-only", false);

        if (playerOnly && killer == null) return;

        int remaining = lifeManager.removeLife(victim);
        victim.sendMessage(ChatColor.RED + "Straciles zycie! Pozostalo: " + ChatColor.YELLOW + remaining + ChatColor.RED + " zyc.");
        nameTagManager.updatePlayer(victim);

        if (remaining <= 0) banPlayer(victim);

        if (killer != null && !killer.equals(victim)) {
            int kLives = lifeManager.getLives(killer);
            int max = lifeManager.getMaxLives();

            if (kLives >= max) {
                killer.sendMessage(ChatColor.YELLOW + "Posiadasz maksymalna ilosc ZYC");
            } else {
                lifeManager.addLives(killer, 1);
                killer.sendMessage(ChatColor.GREEN + "Zabojstwo! Zdobyles zycie. Masz teraz " + ChatColor.YELLOW + (kLives + 1) + ChatColor.GREEN + " zyc.");
                nameTagManager.updatePlayer(killer);
            }
        }
    }

    private void banPlayer(Player player) {
        int hours = plugin.getConfig().getInt("ban-duration-hours", 48);
        String reason = plugin.getConfig().getString("ban-reason", "Straciles wszystkie zycia!");
        Date expiry = new Date(System.currentTimeMillis() + (hours * 60L * 60L * 1000L));

        lifeManager.markBanPending(player);
        Bukkit.getBanList(BanList.Type.NAME).addBan(player.getName(), reason, expiry, "LifeSystem");
        player.kickPlayer(ChatColor.DARK_RED + "Zbanowany na " + hours + "h!\n" + ChatColor.RED + reason);
        Bukkit.broadcastMessage(ChatColor.DARK_RED + "[LifeSystem] " + ChatColor.RED + player.getName() + " stracil wszystkie zycia i dostal bana na " + hours + "h!");
    }
}
