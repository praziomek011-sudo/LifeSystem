package pl.lifesystem;

import org.bukkit.plugin.java.JavaPlugin;

public class LifeSystem extends JavaPlugin {

    private LifeManager lifeManager;
    private RecipeManager recipeManager;

    @Override
    public void onEnable() {
        // Zapisz domyślny config, jeśli nie istnieje
        saveDefaultConfig();

        // Inicjalizacja menedżera żyć
        lifeManager = new LifeManager(this);

        // Rejestracja listenerów
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // Rejestracja komend
        CommandHandler commandHandler = new CommandHandler(this);
        getCommand("lifesystem").setExecutor(commandHandler);
        getCommand("lifesystem").setTabCompleter(commandHandler);

        // Rejestracja craftingu
        recipeManager = new RecipeManager(this);
        recipeManager.registerRecipes();

        getLogger().info("LifeSystem został włączony!");
    }

    @Override
    public void onDisable() {
        if (lifeManager != null) {
            lifeManager.saveData();
        }
        getLogger().info("LifeSystem został wyłączony.");
    }

    public LifeManager getLifeManager() {
        return lifeManager;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }
}