package pl.lifesystem;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

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
                plugin.getLogger().severe("Nie można utworzyć pliku lives.yml!");
                e.printStackTrace();
            }
        }

        this.livesConfig = YamlConfiguration.loadConfiguration(livesFile);
    }

    /** Pobiera liczbę żyć gracza. Jeśli nie ma danych, ustawia domyślną wartość. */
    public int getLives(OfflinePlayer player) {
        UUID uuid = player.getUniqueId();
        String path = "players." + uuid + ".lives";

        if (!livesConfig.contains(path)) {
            int defaultLives = plugin.getConfig().getInt("default-lives", 5);
            livesConfig.set(path, defaultLives);
            saveData();
            return defaultLives;
        }

        return livesConfig.getInt(path);
    }

    /** Ustawia liczbę żyć gracza. */
    public void setLives(OfflinePlayer player, int amount) {
        UUID uuid = player.getUniqueId();
        String path = "players." + uuid + ".lives";
        livesConfig.set(path, Math.max(0, amount));
        saveData();
    }

    /** Dodaje życia graczowi. */
    public void addLives(OfflinePlayer player, int amount) {
        int current = getLives(player);
        setLives(player, current + amount);
    }

    /** Odejmuje życie graczowi. Zwraca nową liczbę żyć. */
    public int removeLife(OfflinePlayer player) {
        int current = getLives(player);
        int newAmount = Math.max(0, current - 1);
        setLives(player, newAmount);
        return newAmount;
    }

    /** Zapisuje dane do pliku. */
    public void saveData() {
        try {
            livesConfig.save(livesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Nie można zapisać lives.yml!");
            e.printStackTrace();
        }
    }

    /** Ładuje dane z pliku. */
    public void reloadData() {
        livesConfig = YamlConfiguration.loadConfiguration(livesFile);
    }
}