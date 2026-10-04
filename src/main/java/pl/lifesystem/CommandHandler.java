package pl.lifesystem;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
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

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "check":
                return handleCheck(sender, args);
            case "give":
                return handleGive(sender, args);
            case "set":
                return handleSet(sender, args);
            case "playerdeathonly":
                return handlePlayerDeathOnly(sender, args);
            default:
                sendHelp(sender);
                return true;
        }
    }

    /** /lifesystem Check – pokazuje własne życia. */
    /** /lifesystem Check [nick] – tylko dla OP – pokazuje życia gracza. */
    private boolean handleCheck(CommandSender sender, String[] args) {
        if (args.length == 1) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Ta komenda jest tylko dla graczy!");
                return true;
            }
            Player player = (Player) sender;
            int lives = lifeManager.getLives(player);
            player.sendMessage(ChatColor.GOLD + "Masz " + ChatColor.YELLOW + lives + ChatColor.GOLD + " żyć.");
            return true;
        }

        // args.length >= 2 – sprawdzanie innego gracza (tylko OP)
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Nie masz uprawnień do sprawdzania żyć innych graczy!");
            return true;
        }

        String targetName = args[1];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            sender.sendMessage(ChatColor.RED + "Nie znaleziono gracza: " + targetName);
            return true;
        }

        int lives = lifeManager.getLives(target);
        sender.sendMessage(ChatColor.GOLD + "Gracz " + ChatColor.YELLOW + target.getName() + ChatColor.GOLD + " ma " + ChatColor.YELLOW + lives + ChatColor.GOLD + " żyć.");
        return true;
    }

    /** /lifesystem Give Life – dodaje życie sobie (lub innemu, jeśli OP). */
    private boolean handleGive(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Ta komenda jest tylko dla graczy!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 2 || !args[1].equalsIgnoreCase("life")) {
            player.sendMessage(ChatColor.RED + "Użycie: /lifesystem Give Life [nick] [ilość]");
            return true;
        }

        // Domyślnie: dodaj 1 życie sobie.
        int amount = 1;
        OfflinePlayer target = player;

        if (args.length >= 3) {
            // Sprawdź uprawnienia do dawania innym.
            if (!player.isOp()) {
                player.sendMessage(ChatColor.RED + "Nie masz uprawnień do dawania żyć innym graczom!");
                return true;
            }
            target = Bukkit.getOfflinePlayer(args[2]);
            if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
                player.sendMessage(ChatColor.RED + "Nie znaleziono gracza: " + args[2]);
                return true;
            }
        }

        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Nieprawidłowa ilość!");
                return true;
            }
        }

        lifeManager.addLives(target, amount);
        player.sendMessage(ChatColor.GREEN + "Dodano " + amount + " żyć graczowi " + target.getName() + ".");
        return true;
    }

    /** /lifesystem Life set [nick] [ilość] – tylko OP. */
    private boolean handleSet(CommandSender sender, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Nie masz uprawnień do tej komendy!");
            return true;
        }

        if (args.length < 4 || !args[1].equalsIgnoreCase("life")) {
            sender.sendMessage(ChatColor.RED + "Użycie: /lifesystem Life set [nick] [ilość]");
            return true;
        }

        String targetName = args[2];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            sender.sendMessage(ChatColor.RED + "Nie znaleziono gracza: " + targetName);
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

    /** /lifesystem PlayerDeathOnly on/off – tylko OP. */
    private boolean handlePlayerDeathOnly(CommandSender sender, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Nie masz uprawnień do tej komendy!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Użycie: /lifesystem PlayerDeathOnly on/off");
            return true;
        }

        boolean value;
        if (args[1].equalsIgnoreCase("on")) {
            value = true;
        } else if (args[1].equalsIgnoreCase("off")) {
            value = false;
        } else {
            sender.sendMessage(ChatColor.RED + "Nieprawidłowa wartość! Użyj on lub off.");
            return true;
        }

        plugin.getConfig().set("player-death-only", value);
        plugin.saveConfig();
        sender.sendMessage(ChatColor.GREEN + "Ustawiono PlayerDeathOnly na: " + (value ? "ON" : "OFF"));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "===== LifeSystem =====");
        sender.sendMessage(ChatColor.YELLOW + "/lifesystem Check" + ChatColor.GRAY + " – sprawdź swoje życia.");
        if (sender.isOp()) {
            sender.sendMessage(ChatColor.YELLOW + "/lifesystem Check [nick]" + ChatColor.GRAY + " – sprawdź życia gracza.");
            sender.sendMessage(ChatColor.YELLOW + "/lifesystem Give Life [nick] [ilość]" + ChatColor.GRAY + " – daj życia.");
            sender.sendMessage(ChatColor.YELLOW + "/lifesystem Life set [nick] [ilość]" + ChatColor.GRAY + " – ustaw życia.");
            sender.sendMessage(ChatColor.YELLOW + "/lifesystem PlayerDeathOnly on/off" + ChatColor.GRAY + " – tryb śmierci.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("check");
            completions.add("give");
            if (sender.isOp()) {
                completions.add("life");
                completions.add("playerdeathonly");
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("life")) {
                completions.add("life");
            }
            if (args[0].equalsIgnoreCase("playerdeathonly")) {
                completions.add("on");
                completions.add("off");
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("life")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    completions.add(p.getName());
                }
            }
        }

        // Filtruj według wpisanego tekstu.
        String lastArg = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(lastArg));

        return completions;
    }
}