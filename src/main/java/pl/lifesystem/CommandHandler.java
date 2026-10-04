package pl.lifesystem;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CommandHandler implements CommandExecutor, TabCompleter {

    private final LifeSystem plugin;
    private final LifeManager lifeManager;

    public CommandHandler(LifeSystem plugin) {
        this.plugin = plugin;
        this.lifeManager = plugin.getLifeManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "check":           return handleCheck(sender, args);
            case "give":            return handleGive(sender, args);
            case "life":            return handleLife(sender, args);
            case "playerdeathonly": return handlePlayerDeathOnly(sender, args);
            default:                sendHelp(sender); return true;
        }
    }

    /** /lifesystem Check  |  /lifesystem Check [nick] (OP) */
    private boolean handleCheck(CommandSender sender, String[] args) {
        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Ta komenda tylko dla graczy!");
                return true;
            }
            Player p = (Player) sender;
            p.sendMessage(ChatColor.GOLD + "Masz " + ChatColor.YELLOW + lifeManager.getLives(p) + ChatColor.GOLD + " żyć.");
            return true;
        }

        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Brak uprawnień!");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Nie znaleziono gracza: " + args[1]);
            return true;
        }
        sender.sendMessage(ChatColor.GOLD + "Gracz " + ChatColor.YELLOW + target.getName() + ChatColor.GOLD + " ma " + ChatColor.YELLOW + lifeManager.getLives(target) + ChatColor.GOLD + " żyć.");
        return true;
    }

    /** /lifesystem Give Life [nick]  → daje ITEM do ekwipunku */
    private boolean handleGive(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Ta komenda tylko dla graczy!");
            return true;
        }
        if (args.length < 2 || !args[1].equalsIgnoreCase("life")) {
            sender.sendMessage(ChatColor.RED + "Użycie: /lifesystem Give Life [nick]");
            return true;
        }

        Player giver = (Player) sender;
        Player target = giver;

        if (args.length >= 3) {
            if (!giver.isOp()) {
                giver.sendMessage(ChatColor.RED + "Brak uprawnień do dawania innym!");
                return true;
            }
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                giver.sendMessage(ChatColor.RED + "Gracz musi być online: " + args[2]);
                return true;
            }
        }

        ItemStack lifeItem = RecipeManager.createLifeItem(1);
        target.getInventory().addItem(lifeItem);
        target.sendMessage(ChatColor.GREEN + "❤ Otrzymałeś item Życie! Kliknij PPM aby użyć.");
        giver.sendMessage(ChatColor.GREEN + "❤ Dano item Życie graczowi " + target.getName());
        return true;
    }

    /** /lifesystem Life set [nick] [ilość] (OP) */
    private boolean handleLife(CommandSender sender, String[] args) {
        if (args.length < 2 || !args[1].equalsIgnoreCase("set")) {
            sender.sendMessage(ChatColor.RED + "Użycie: /lifesystem Life set [nick] [ilość]");
            return true;
        }
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Brak uprawnień!");
            return true;
        }
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Użycie: /lifesystem Life set [nick] [ilość]");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(ChatColor.RED + "Nie znaleziono gracza: " + args[2]);
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Nieprawidłowa ilość!");
            return true;
        }

        lifeManager.setLives(target, amount);
        sender.sendMessage(ChatColor.GREEN + "Ustawiono " + amount + " żyć graczowi " + target.getName() + ".");
        return true;
    }

    /** /lifesystem PlayerDeathOnly on/off (OP) */
    private boolean handlePlayerDeathOnly(CommandSender sender, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Brak uprawnień!");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Użycie: /lifesystem PlayerDeathOnly on/off");
            return true;
        }

        boolean value;
        if (args[1].equalsIgnoreCase("on")) value = true;
        else if (args[1].equalsIgnoreCase("off")) value = false;
        else {
            sender.sendMessage(ChatColor.RED + "Użyj on lub off!");
            return true;
        }

        plugin.getConfig().set("player-death-only", value);
        plugin.saveConfig();
        sender.sendMessage(ChatColor.GREEN + "PlayerDeathOnly: " + (value ? "ON" : "OFF"));
        return true;
    }

    private void sendHelp(CommandSender s) {
        s.sendMessage(ChatColor.GOLD + "===== LifeSystem =====");
        s.sendMessage(ChatColor.YELLOW + "/lifesystem Check" + ChatColor.GRAY + " – swoje życia");
        if (s.isOp()) {
            s.sendMessage(ChatColor.YELLOW + "/lifesystem Check [nick]");
            s.sendMessage(ChatColor.YELLOW + "/lifesystem Give Life [nick]");
            s.sendMessage(ChatColor.YELLOW + "/lifesystem Life set [nick] [ilość]");
            s.sendMessage(ChatColor.YELLOW + "/lifesystem PlayerDeathOnly on/off");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> c = new ArrayList<>();
        if (args.length == 1) {
            c.add("check");
            c.add("give");
            if (sender.isOp()) { c.add("life"); c.add("playerdeathonly"); }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give")) c.add("life");
            if (args[0].equalsIgnoreCase("life")) c.add("set");
            if (args[0].equalsIgnoreCase("playerdeathonly")) { c.add("on"); c.add("off"); }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("life") || args[0].equalsIgnoreCase("check")) {
                for (Player p : Bukkit.getOnlinePlayers()) c.add(p.getName());
            }
        }

        String last = args[args.length - 1].toLowerCase();
        c.removeIf(s -> !s.toLowerCase().startsWith(last));
        return c;
    }
}
