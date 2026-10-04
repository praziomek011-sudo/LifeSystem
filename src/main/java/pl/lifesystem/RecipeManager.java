package pl.lifesystem;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class RecipeManager {

    private final LifeSystem plugin;

    public RecipeManager(LifeSystem plugin) {
        this.plugin = plugin;
    }

    /** Rejestruje crafting „Życia”. */
    public void registerRecipes() {
        // Utwórz item wynikowy – „Życie”.
        ItemStack lifeItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = lifeItem.getItemMeta();
        meta.setDisplayName("§c❤ Życie");
        meta.setLore(Arrays.asList(
                "§7Kliknij PPM, aby dodać sobie życie.",
                "§7Pozwala przetrwać kolejną śmierć."
        ));
        lifeItem.setItemMeta(meta);

        // Utwórz przepis 3x3.
        NamespacedKey key = new NamespacedKey(plugin, "life_recipe");
        ShapedRecipe recipe = new ShapedRecipe(key, lifeItem);

        // Wzór:
        // nic/GHAST_TEAR/nic
        // NETHER_STAR/OMINOUS_TRIAL_KEY/NETHER_STAR
        // nic/GHAST_TEAR/nic
        recipe.shape(" G ", "NON", " G ");

        recipe.setIngredient('G', Material.GHAST_TEAR);
        recipe.setIngredient('N', Material.NETHER_STAR);
        recipe.setIngredient('O', Material.OMINOUS_TRIAL_KEY);

        // Zarejestruj przepis.
        plugin.getServer().addRecipe(recipe);
    }
}