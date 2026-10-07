package pl.lifesystem;

import org.bukkit.plugin.java.JavaPlugin;

public class LifeSystem extends JavaPlugin {

    private LifeManager lifeManager;
    private RecipeManager recipeManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        lifeManager = new LifeManager(this);

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        CommandHandler commandHandler = new CommandHandler(this);
        getCommand("lifesystem").setExecutor(commandHandler);
        getCommand("lifesystem").setTabCompleter(commandHandler);

        recipeManager = new RecipeManager(this);
        recipeManager.registerRecipes();

        getLogger().info("LifeSystem wlaczony!");
    }

    @Override
    public void onDisable() {
        if (lifeManager != null) lifeManager.saveData();
        getLogger().info("LifeSystem wylaczony.");
    }

    public LifeManager getLifeManager() {
        return lifeManager;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }
}
