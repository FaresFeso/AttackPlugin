package com.attackmastery;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.Material;
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
            sender.sendMessage("§7/attack help §8- §7Open start guide");
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cOnly players can open the guide book.");
                return true;
            }
            openHelpBook(player);
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

        if (args[0].equalsIgnoreCase("path")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cConsole cannot use mastery paths!");
                return true;
            }
            return handlePathCommand((Player) sender, args);
        }
        
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("attackmastery.reload")) {
                sender.sendMessage("§cNo permission!");
                return true;
            }
            plugin.reloadConfig();
            for (Player online : Bukkit.getOnlinePlayers()) {
                plugin.getEventListener().refreshPlayerStats(online);
            }
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
            plugin.getEventListener().refreshPlayerStats(target);
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
        sender.sendMessage("§ePath: §f" + data.getMasteryPath().displayName() + " §7(Mastery Lv." + data.getMasteryLevel() + ")");
        if (!data.getCosmeticTitle().isBlank()) {
            sender.sendMessage("§eTitle: §f" + data.getCosmeticTitle());
        }
        if (data.getMasteryPath() != MasteryPath.NONE && data.getMasteryObjectiveTarget() > 0) {
            sender.sendMessage("§eObjective: §f" + data.getMasteryObjective() + " §7(" + data.getMasteryObjectiveProgress() + "/" + data.getMasteryObjectiveTarget() + ")");
        }
        if (data.getLevel() > 100) {
            int levelsAbove = data.getLevel() - 100;
            double reductionPerLevelPercent = plugin.getConfig().getDouble("damage-reduction-per-level-after-100", 0.005) * 100.0;
            double reduction = Math.min(levelsAbove * reductionPerLevelPercent, 50.0);
            sender.sendMessage("§eDamage Reduction: §f" + String.format("%.1f", reduction) + "%");
        }
    }

    private boolean handlePathCommand(Player player, String[] args) {
        PlayerData data = plugin.getPlayerData(player.getUniqueId());
        int unlockLevel = plugin.getConfig().getInt("mastery.unlock-level", 25);

        if (args.length == 1 || args[1].equalsIgnoreCase("info")) {
            player.sendMessage("§6=== Mastery Path ===");
            player.sendMessage("§ePath: §f" + data.getMasteryPath().displayName());
            player.sendMessage("§eMastery Level: §f" + data.getMasteryLevel());
            player.sendMessage("§eMastery XP: §f" + String.format("%.1f", data.getMasteryXp()));
            if (data.getMasteryPath() != MasteryPath.NONE && data.getMasteryObjectiveTarget() > 0) {
                player.sendMessage("§eObjective: §f" + data.getMasteryObjective() + " §7(" + data.getMasteryObjectiveProgress() + "/" + data.getMasteryObjectiveTarget() + ")");
            }
            player.sendMessage("§7Use: /attack path choose <sword|axe|bow|crit>");
            player.sendMessage("§7Use: /attack path respec");
            return true;
        }

        if (args[1].equalsIgnoreCase("choose")) {
            if (data.getLevel() < unlockLevel) {
                player.sendMessage("§cYou must be attack level " + unlockLevel + "+ to choose a mastery path.");
                return true;
            }
            if (data.getMasteryPath() != MasteryPath.NONE) {
                player.sendMessage("§cYou already have a path. Use /attack path respec to change it.");
                return true;
            }
            if (args.length < 3) {
                player.sendMessage("§cUsage: /attack path choose <sword|axe|bow|crit>");
                return true;
            }

            MasteryPath selected = MasteryPath.fromString(args[2]);
            if (selected == MasteryPath.NONE) {
                player.sendMessage("§cInvalid path. Choose sword, axe, bow, or crit.");
                return true;
            }

            data.setMasteryPath(selected);
            data.setMasteryLevel(0);
            data.setMasteryXp(0);
            data.setMasteryObjectiveProgress(0);
            data.setMasteryObjectiveTarget(0);
            data.setCosmeticTitle("");
            plugin.getQuestManager().initializeMasteryObjective(data);
            plugin.markDirty(player.getUniqueId());
            player.sendMessage("§aYou chose §f" + selected.displayName() + "§a!");
            return true;
        }

        if (args[1].equalsIgnoreCase("respec")) {
            if (data.getMasteryPath() == MasteryPath.NONE) {
                player.sendMessage("§cYou have no path to respec.");
                return true;
            }

            int levelCost = plugin.getConfig().getInt("mastery.respec-cost-levels", 5);
            if (data.getLevel() < levelCost) {
                player.sendMessage("§cYou need at least " + levelCost + " attack levels to respec.");
                return true;
            }

            data.setLevel(Math.max(0, data.getLevel() - levelCost));
            data.setXp(0);
            data.setXpNeeded(plugin.getConfig().getInt("xp-base", 200) + (data.getLevel() * plugin.getConfig().getInt("xp-increment", 30)));
            data.setMasteryPath(MasteryPath.NONE);
            data.setMasteryLevel(0);
            data.setMasteryXp(0);
            data.setMasteryObjective("");
            data.setMasteryObjectiveProgress(0);
            data.setMasteryObjectiveTarget(0);
            data.setCosmeticTitle("");
            data.setMasteryRespecs(data.getMasteryRespecs() + 1);
            plugin.markDirty(player.getUniqueId());
            plugin.getEventListener().refreshPlayerStats(player);
            player.sendMessage("§eMastery reset complete. §c-" + levelCost + " attack levels");
            return true;
        }

        player.sendMessage("§cUsage: /attack path <info|choose|respec>");
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("help");
            completions.add("quest");
            completions.add("path");
            if (sender.hasPermission("attackmastery.reload")) completions.add("reload");
            if (sender.hasPermission("attackmastery.reset")) completions.add("reset");
            if (sender.hasPermission("attackmastery.set")) completions.add("set");
            if (sender.hasPermission("attackmastery.info")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    completions.add(p.getName());
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("path")) {
            completions.add("info");
            completions.add("choose");
            completions.add("respec");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("path") && args[1].equalsIgnoreCase("choose")) {
            completions.add("sword");
            completions.add("axe");
            completions.add("bow");
            completions.add("crit");
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("set"))) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
        }
        
        return completions;
    }

    private void openHelpBook(Player player) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta == null) {
            return;
        }

        meta.setTitle("Attack Mastery Guide");
        meta.setAuthor("AttackMastery");
        meta.setGeneration(BookMeta.Generation.ORIGINAL);

        List<Component> pages = new ArrayList<>();
        pages.add(Component.text("§6§lAttack Mastery\n\n§0Welcome!\nLevel up by killing mobs, unlock mastery paths, complete quests, and become stronger."));
        pages.add(Component.text("§6§lGetting Started\n\n§0- /attack\nView your stats\n\n- /attack quest\nOpen quest menu\n\n- /attack help\nOpen this guide"));
        pages.add(Component.text("§6§lMastery Paths\n\n§0At level 25, choose a path:\n§2Sword\n§4Axe\n§9Bow\n§5Crit\n\nUse: /attack path choose <path>"));
        pages.add(Component.text("§6§lPath Progress\n\n§0Use /attack path info to see mastery level and objective progress.\n\nComplete objectives/contracts for mastery XP."));
        pages.add(Component.text("§6§lRespec\n\n§0Want another build?\nUse /attack path respec\n\nIt costs attack levels, so choose carefully."));
        pages.add(Component.text("§6§lLeaderboard\n\n§0Check the right sidebar for your live stats and top players.\n\nClimb the leaderboard!\n"));
        pages.add(Component.text("§6§lAdmin Commands\n\n§0/attack set <p> <lvl> [xp]\n/attack reset <p>\n/attack reload"));

        meta.addPages(pages.toArray(new Component[0]));
        book.setItemMeta(meta);
        player.openBook(book);
    }
}
