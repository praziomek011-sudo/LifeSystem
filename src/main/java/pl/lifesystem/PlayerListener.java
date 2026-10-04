package pl.lifesystem;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Date;

public class PlayerListener implements Listener {

    private final LifeSystem plugin;
    private final LifeManager lifeManager;

    public PlayerListener(LifeSystem plugin) {
        this.plugin = plugin;
        this.lifeManager = plugin.getLifeManager();
    }

    /** Gdy gracz dołącza – upewniamy się, że ma przypisaną liczbę żyć. */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Wywołanie getLives() automatycznie ustawi domyślną wartość, jeśli brak.
        lifeManager.getLives(player);
    }

    /** Obsługa śmierci gracza. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Sprawdź, czy życie ma być odebrane tylko za śmierć z rąk gracza.
        boolean playerDeathOnly = plugin.getConfig().getBoolean("player-death-only", false);

        if (playerDeathOnly) {
            Player killer = player.getKiller();
            // Jeśli gracz nie został zabity przez innego gracza – nie zabieramy życia.
            if (killer == null) {
                return;
            }
        }

        // Odejmij życie.
        int remainingLives = lifeManager.removeLife(player);

        // Informuj gracza.
        player.sendMessage(ChatColor.RED + "☠ Straciłeś życie! Pozostało Ci: " + ChatColor.YELLOW + remainingLives + ChatColor.RED + " żyć.");

        // Jeśli gracz stracił wszystkie życia – ban.
        if (remainingLives <= 0) {
            banPlayer(player);
        }
    }

    /** Banuje gracza na 48 godzin. */
    private void banPlayer(Player player) {
        int banHours = plugin.getConfig().getInt("ban-duration-hours", 48);
        String reason = plugin.getConfig().getString("ban-reason", "Straciłeś wszystkie życia!");

        // Oblicz datę wygaśnięcia bana.
        Date expiry = new Date(System.currentTimeMillis() + (banHours * 60L * 60L * 1000L));

        // Dodaj bana.
        Bukkit.getBanList(BanList.Type.NAME).addBan(
                player.getName(),
                reason,
                expiry,
                "LifeSystem"
        );

        // Wyrzuć gracza z serwera.
        player.kickPlayer(ChatColor.DARK_RED + "Zostałeś zbanowany na " + banHours + " godzin!\n" + ChatColor.RED + reason);

        // Wiadomość na serwerze.
        Bukkit.broadcastMessage(ChatColor.DARK_RED + "[LifeSystem] " + ChatColor.RED + player.getName() + " stracił wszystkie życia i został zbanowany na " + banHours + " godzin!");
    }
}