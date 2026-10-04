package pl.lifesystem;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public class RecipeManager {

    private final LifeSystem plugin;
    public static NamespacedKey LIFE_KEY;

    public RecipeManager(LifeSystem plugin) {
        this.plugin = plugin;
        LIFE_KEY = new NamespacedKey(plugin, "life_item");
    }

    /** Tworzy item "Życie" z tagiem PDC, żeby plugin wiedział że to nasz item. */
    public static ItemStack createLifeItem(int amount) {
        ItemStack item = new ItemStack(Material.NETHER_STAR, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§c❤ Życie");
        meta.setLore(Arrays.asList(
                "§7Kliknij §ePPM§7, aby dodać sobie 1 życie.",
                "§7Można craftować lub dostać od admina."
        ));
        meta.getPersistentDataContainer().set(LIFE_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    /** Sprawdza czy item to nasze Życie. */
    public static boolean isLifeItem(ItemStack item) {
        if (item == null || item.getType() != Material.NETHER_STAR) return false;
        if (!item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(LIFE_KEY, PersistentDataType.BYTE);
    }

    public void registerRecipes() {
        ItemStack lifeItem = createLifeItem(1);

        NamespacedKey key = new NamespacedKey(plugin, "life_recipe");
        ShapedRecipe recipe = new ShapedRecipe(key, lifeItem);
        recipe.shape(" G ", "NON", " G ");
        recipe.setIngredient('G', Material.GHAST_TEAR);
        recipe.setIngredient('N', Material.NETHER_STAR);
        recipe.setIngredient('O', Material.OMINOUS_TRIAL_KEY);

        // Usuń stary przepis jeśli istnieje (przy /reload)
        plugin.getServer().removeRecipe(key);
        plugin.getServer().addRecipe(recipe);
    }
}
