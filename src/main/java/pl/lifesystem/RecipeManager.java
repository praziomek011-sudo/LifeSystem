package pl.lifesystem;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;

public class RecipeManager {

    private final LifeSystem plugin;
    public static NamespacedKey LIFE_KEY;

    public RecipeManager(LifeSystem plugin) {
        this.plugin = plugin;
        LIFE_KEY = new NamespacedKey(plugin, "life_item");
    }

    public static ItemStack createLifeItem(int amount) {
        ItemStack item = new ItemStack(Material.NETHER_STAR, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00A7c\u2764 \u00A7f\u017Bycie");
        meta.setLore(Arrays.asList(
                "\u00A77Kliknij \u00A7ePPM\u00A77, aby dodac sobie 1 zycie.",
                "\u00A77Craftuj lub otrzymaj od admina."
        ));
        meta.getPersistentDataContainer().set(LIFE_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isLifeItem(ItemStack item) {
        if (item == null || item.getType() != Material.NETHER_STAR) return false;
        if (!item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(LIFE_KEY, PersistentDataType.BYTE);
    }

    public void registerRecipes() {
        ItemStack result = createLifeItem(1);

        NamespacedKey key = new NamespacedKey(plugin, "life_recipe");
        plugin.getServer().removeRecipe(key);

        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(" G ", "NON", " G ");
        recipe.setIngredient('G', Material.GHAST_TEAR);
        recipe.setIngredient('N', Material.NETHER_STAR);
        recipe.setIngredient('O', Material.OMINOUS_TRIAL_KEY);

        plugin.getServer().addRecipe(recipe);
    }
}
