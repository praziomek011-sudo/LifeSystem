package pl.lifesystem;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class NameTagManager {

    private final LifeManager lifeManager;

    public NameTagManager(LifeSystem plugin) {
        this.lifeManager = plugin.getLifeManager();
    }

    /** Ustawia suffix za nickiem: ❤ za każde życie, ☠ za każde stracone (białe). */
    public void updatePlayer(Player player) {
        if (player == null) return;

        int lives = lifeManager.getLives(player);
        int max = lifeManager.getMaxLives();
        String suffix = buildSuffix(lives, max);

        player.setPlayerListName(ChatColor.WHITE + player.getName() + " " + suffix);

        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = board.getTeam(player.getName());
        if (team == null) {
            team = board.registerNewTeam(player.getName());
        }
        team.setSuffix(" " + suffix);
        team.addEntry(player.getName());
    }

    public String buildSuffix(int lives, int max) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < max; i++) {
            if (i < lives) sb.append(ChatColor.WHITE).append("\u2764");   // ❤
            else sb.append(ChatColor.WHITE).append("\u2620");              // ☠
        }
        return sb.toString();
    }
}
