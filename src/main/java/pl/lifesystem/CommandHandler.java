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
            case "withdraw":        return handleWithdraw(sender, args);
            default:                sendHelp(sender); return true;
        }
    }

    private boolean handleCheck(CommandSender sender, String[] args) {
        if (args.length == 1) {
            if (!(sender instanceof Player)) { sender.sendMessage(ChatColor.RED + "Tylko dla graczy!"); return true; }
            Player p = (Player) sender;
            p.sendMessage(ChatColor.GOLD + "Masz " + ChatColor.YELLOW + lifeManager.getLives(p) + ChatColor.GOLD + " / " + lifeManager.getMaxLives() + " żyć.");
            return true;
        }
        if (!sender.isOp()) { sender.sendMessage(ChatColor.RED + "Brak uprawnień!"); return true; }

        OfflinePlayer t = Bukkit.getOfflinePlayer(args[1]);
        if (!t.hasPlayedBefore() && !t.isOnline()) { sender.sendMessage(ChatColor.RED + "Nie znaleziono gracza: " + args[1]); return true; }
        sender.sendMessage(ChatColor.GOLD + "Gracz " + ChatColor.YELLOW + t.getName() + ChatColor.GOLD + " ma " + ChatColor.YELLOW + lifeManager.getLives(t) + ChatColor.GOLD + " / " + lifeManager.getMaxLives() + " żyć.");
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage(ChatColor.RED + "Tylko dla graczy!"); return true; }
        if (args.length < 2 || !args[1].equalsIgnoreCase("life")) {
            sender.sendMessage(ChatColor.RED + "Użycie: /lifesystem Give Life [nick]");
            return true;
        }

        Player giver = (Player) sender;
        Player target = giver;

        if (args.length >= 3) {
            if (!giver.isOp()) { giver.sendMessage(ChatColor.RED + "Brak uprawnień!"); return true; }
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) { giver.sendMessage(ChatColor.RED + "Gracz musi być online: " + args[2]); return true; }
        }

        target.getInventory().addItem(RecipeManager.createLifeItem(1));
        target.sendMessage(ChatColor.GREEN + "❤ Otrzymałeś item Życie! Kliknij PPM aby użyć.");
        giver.sendMessage(ChatColor.GREEN + "❤ Dano item Życie graczowi " + target.getName());
        return true;
    }

    private boolean handleLife(CommandSender sender, String[] args) {
        if (args.length < 4 || !args[1].equalsIgnoreCase("set")) {
            sender.sendMessage(ChatColor.RED + "Użycie: /lifesystem Life set [nick] [ilość]");
            return true;
        }
        if (!sender.isOp()) { sender.sendMessage(ChatColor.RED + "Brak uprawnień!"); return true; }

        OfflinePlayer t = Bukkit.getOfflinePlayer(args[2]);
        if (!t.hasPlayedBefore() && !t.isOnline()) { sender.sendMessage(ChatColor.RED + "Nie znaleziono gracza: " + args[2]); return true; }

        int amount;
        try { amount = Integer.parseInt(args[3]); }
        catch (NumberFormatException e) { sender.sendMessage(ChatColor.RED + "Nieprawidłowa ilość!"); return true; }

        lifeManager.setLives(t, amount);
        sender.sendMessage(ChatColor.GREEN + "Ustawiono " + amount + " żyć graczowi " + t.getName() + ".");
        return true;
    }

    /** /lifesystem withdraw [ilość] – zamienia życia na itemy. */
    private boolean handleWithdraw(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage(ChatColor.RED + "Tylko dla graczy!"); return true; }
        Player p = (Player) sender;

        if (args.length < 2) {
            p.sendMessage(ChatColor.RED + "Użycie: /lifesystem withdraw [ilość]");
            return true;
        }

        int amount;
        try { amount = Integer.parseInt(args[1]); }
        catch (NumberFormatException e) { p.sendMessage(ChatColor.RED + "Nieprawidłowa ilość!"); return true; }

        if (amount <= 0) { p.sendMessage(ChatColor.RED + "Ilość musi być większa od 0!"); return true; }

        int lives = lifeManager.getLives(p);
        if (lives - amount < 1) {
            p.sendMessage(ChatColor.RED + "Musisz zachować co najmniej 1 życie! Masz " + lives + " żyć.");
            return true;
        }

        lifeManager.setLives(p, lives - amount);
        p.getInventory().addItem(RecipeManager.createLifeItem(amount));
        p.sendMessage(ChatColor.GREEN + "❤ Wypłacono " + amount + " żyć jako itemy. Zostało Ci: " + ChatColor.YELLOW + (lives - amount) + ChatColor.GREEN + " żyć.");
        return true;
    }

    private boolean handlePlayerDeathOnly(CommandSender sender, String[] args) {
        if (!sender.isOp()) { sender.sendMessage(ChatColor.RED + "Brak uprawnień!"); return true; }
        if (args.length < 2) { sender.sendMessage(ChatColor.RED + "Użycie: /lifesystem PlayerDeathOnly on/off"); return true; }

        boolean value;
        if (args[1].equalsIgnoreCase("on")) value = true;
        else if (args[1].equalsIgnoreCase("off")) value = false;
        else { sender.sendMessage(ChatColor.RED + "Użyj on lub off!"); return true; }

        plugin.getConfig().set("player-death-only", value);
        plugin.saveConfig();
        sender.sendMessage(ChatColor.GREEN + "PlayerDeathOnly: " + (value ? "ON" : "OFF"));
        return true;
    }

    private void sendHelp(CommandSender s) {
        s.sendMessage(ChatColor.GOLD + "===== LifeSystem =====");
        s.sendMessage(ChatColor.YELLOW + "/lifesystem Check" + ChatColor.GRAY + " – swoje życia");
        s.sendMessage(ChatColor.YELLOW + "/lifesystem withdraw [ilość]" + ChatColor.GRAY + " – zamień życia na itemy");
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
            c.add("withdraw");
            if (sender.isOp()) { c.add("life"); c.add("playerdeathonly"); }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give")) c.add("life");
            if (args[0].equalsIgnoreCase("life")) c.add("set");
            if (args[0].equalsIgnoreCase("playerdeathonly")) { c.add("on"); c.add("off"); }
            if (args[0].equalsIgnoreCase("withdraw")) c.add("1");
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
