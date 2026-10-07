package pl.lifesystem;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class NameTagManager {

    private final LifeSystem plugin;
    private final LifeManager lifeManager;

    public NameTagManager(LifeSystem plugin) {
        this.plugin = plugin;
        this.lifeManager = plugin.getLifeManager();
    }

    /** Ustawia suffix (za nickiem) i nazwę w tabliście na podstawie żyć. */
    public void updatePlayer(Player player) {
        if (player == null) return;

        int lives = lifeManager.getLives(player);
        int max = lifeManager.getMaxLives();

        String suffix = buildHeartSuffix(lives, max);

        // Nazwa w tabliście: Nick + spacja + symbole
        player.setPlayerListName(ChatColor.WHITE + player.getName() + " " + suffix);

        // Team suffix → wyświetla się nad głową i na czacie ZA nickiem
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = board.getTeam(player.getName());

        if (team == null) {
            team = board.registerNewTeam(player.getName());
        }

        team.setSuffix(" " + suffix); // ⬅ SUFFIX zamiast prefix
        team.addEntry(player.getName());
    }

    /** Buduje string z sercami i czaszkami. */
    public String buildHeartSuffix(int lives, int max) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < max; i++) {
            if (i < lives) {
                sb.append(ChatColor.RED).append("❤");
            } else {
                sb.append(ChatColor.DARK_GRAY).append("☠");
            }
        }

        return sb.toString();
    }
}
