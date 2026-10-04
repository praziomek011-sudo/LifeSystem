package pl.lifesystem;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class LifeManager {

    private final LifeSystem plugin;
    private final File livesFile;
    private FileConfiguration livesConfig;

    public LifeManager(LifeSystem plugin) {
        this.plugin = plugin;
        this.livesFile = new File(plugin.getDataFolder(), "lives.yml");

        if (!livesFile.exists()) {
            try {
                livesFile.getParentFile().mkdirs();
                livesFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Nie można utworzyć lives.yml!");
                e.printStackTrace();
            }
        }
        this.livesConfig = YamlConfiguration.loadConfiguration(livesFile);
    }

    public int getLives(OfflinePlayer player) {
        String path = "players." + player.getUniqueId() + ".lives";
        if (!livesConfig.contains(path)) {
            int def = plugin.getConfig().getInt("default-lives", 5);
            livesConfig.set(path, def);
            saveData();
            return def;
        }
        return livesConfig.getInt(path);
    }

    public void setLives(OfflinePlayer player, int amount) {
        String path = "players." + player.getUniqueId() + ".lives";
        livesConfig.set(path, Math.max(0, amount));
        saveData();
    }

    /** Dodaje życia, ale NIE przekracza max-lives. Zwraca ile faktycznie dodano. */
    public int addLives(OfflinePlayer player, int amount) {
        int current = getLives(player);
        int max = getMaxLives();
        int newAmount = Math.min(current + amount, max);
        setLives(player, newAmount);
        return newAmount - current;
    }

    public int removeLife(OfflinePlayer player) {
        int current = getLives(player);
        int newAmount = Math.max(0, current - 1);
        setLives(player, newAmount);
        return newAmount;
    }

    public int getMaxLives() {
        return plugin.getConfig().getInt("max-lives", 7);
    }

    public int getLivesAfterBan() {
        return plugin.getConfig().getInt("lives-after-ban", 3);
    }

    /** Oznacza gracza jako "czeka na przywrócenie żyć po banie". */
    public void markBanPending(OfflinePlayer player) {
        livesConfig.set("players." + player.getUniqueId() + ".ban-pending", true);
        saveData();
    }

    public boolean isBanPending(OfflinePlayer player) {
        return livesConfig.getBoolean("players." + player.getUniqueId() + ".ban-pending", false);
    }

    public void clearBanPending(OfflinePlayer player) {
        livesConfig.set("players." + player.getUniqueId() + ".ban-pending", null);
        saveData();
    }

    public void saveData() {
        try {
            livesConfig.save(livesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Nie można zapisać lives.yml!");
            e.printStackTrace();
        }
    }
}
