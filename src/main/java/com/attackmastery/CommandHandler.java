package com.attackmastery;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;

public class CommandHandler implements TabExecutor {
    private final AttackMastery plugin;
    
    public CommandHandler(AttackMastery plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cConsole must specify a player!");
                return true;
            }
            showStats(sender, (Player) sender);
            sender.sendMessage("");
            sender.sendMessage("§7/attack quest §8- §7View quests");
            return true;
        }
        
        if (args[0].equalsIgnoreCase("quest")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cConsole cannot open quest menu!");
                return true;
            }
            plugin.getQuestManager().openQuestMenu((Player) sender);
            return true;
        }
        
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("attackmastery.reload")) {
                sender.sendMessage("§cNo permission!");
                return true;
            }
            plugin.reloadConfig();
            sender.sendMessage("§aConfig reloaded!");
            return true;
        }
        
        if (args[0].equalsIgnoreCase("reset")) {
            if (!sender.hasPermission("attackmastery.reset")) {
                sender.sendMessage("§cNo permission!");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /attack reset <player>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found!");
                return true;
            }
            PlayerData data = plugin.getPlayerData(target.getUniqueId());
            data.setLevel(0);
            data.setXp(0);
            data.setXpNeeded(plugin.getConfig().getInt("xp-base", 200));
            plugin.savePlayerData(target.getUniqueId());
            sender.sendMessage("§aReset " + target.getName() + "'s attack level!");
            return true;
        }
        
        if (args[0].equalsIgnoreCase("set")) {
            if (!sender.hasPermission("attackmastery.set")) {
                sender.sendMessage("§cNo permission!");
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /attack set <player> <level> [xp]");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found!");
                return true;
            }
            try {
                int level = Integer.parseInt(args[2]);
                int maxLevel = plugin.getConfig().getInt("max-level", 200);
                plugin.getLogger().info("[DEBUG] Config max-level value: " + maxLevel);
                plugin.getLogger().info("[DEBUG] Requested level: " + level);
                if (level < 0 || level > maxLevel) {
                    sender.sendMessage("§cLevel must be 0-" + maxLevel);
                    return true;
                }
                PlayerData data = plugin.getPlayerData(target.getUniqueId());
                data.setLevel(level);
                if (args.length > 3) {
                    data.setXp(Double.parseDouble(args[3]));
                } else {
                    data.setXp(0);
                }
                int base = plugin.getConfig().getInt("xp-base", 200);
                int increment = plugin.getConfig().getInt("xp-increment", 30);
                data.setXpNeeded(base + (level * increment));
                plugin.savePlayerData(target.getUniqueId());
                
                if (target.isOnline()) {
                    plugin.getEventListener().refreshPlayerStats(target);
                }
                
                sender.sendMessage("§aSet " + target.getName() + "'s level to " + level);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid number!");
            }
            return true;
        }
        
        if (!sender.hasPermission("attackmastery.info")) {
            sender.sendMessage("§cNo permission!");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found!");
            return true;
        }
        showStats(sender, target);
        return true;
    }
    
    private void showStats(CommandSender sender, Player target) {
        PlayerData data = plugin.getPlayerData(target.getUniqueId());
        int maxLevel = plugin.getConfig().getInt("max-level", 200);
        double damagePercent = data.getLevel() * plugin.getConfig().getDouble("damage-per-level", 0.05) * 100;
        
        sender.sendMessage("§6=== Attack Mastery: " + target.getName() + " ===");
        sender.sendMessage("§eLevel: §f" + data.getLevel() + "/" + maxLevel);
        sender.sendMessage("§eXP: §f" + String.format("%.1f", data.getXp()) + "/" + String.format("%.0f", data.getXpNeeded()));
        sender.sendMessage("§eDamage Bonus: §f+" + String.format("%.1f", damagePercent) + "%");
        sender.sendMessage("§eHealth Bonus: §f+" + Math.min(data.getLevel() / 10, 10) + " Hearts (Max: 10)");
        if (data.getLevel() > 100) {
            int levelsAbove = data.getLevel() - 100;
            double reduction = Math.min(levelsAbove * 0.5, 50.0);
            sender.sendMessage("§eDamage Reduction: §f" + String.format("%.1f", reduction) + "%");
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("quest");
            if (sender.hasPermission("attackmastery.reload")) completions.add("reload");
            if (sender.hasPermission("attackmastery.reset")) completions.add("reset");
            if (sender.hasPermission("attackmastery.set")) completions.add("set");
            if (sender.hasPermission("attackmastery.info")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    completions.add(p.getName());
                }
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("set"))) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
        }
        
        return completions;
    }
}
