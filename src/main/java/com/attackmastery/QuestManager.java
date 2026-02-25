package com.attackmastery;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

public class QuestManager {
    private final AttackMastery plugin;
    private final Map<UUID, QuestData> questDataMap = new HashMap<>();
    private final ZoneId egyptZone = ZoneId.of("Africa/Cairo");
    
    public QuestManager(AttackMastery plugin) {
        this.plugin = plugin;
        startResetScheduler();
    }
    
    private void startResetScheduler() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            ZonedDateTime now = ZonedDateTime.now(egyptZone);
            
            for (UUID uuid : questDataMap.keySet()) {
                QuestData data = questDataMap.get(uuid);
                
                // Check daily reset (12:00 AM and 12:00 PM Egypt time)
                if (shouldResetDaily(data.getLastDailyReset(), now)) {
                    data.resetDaily();
                    data.setLastDailyReset(now.toInstant().toEpochMilli());
                    saveQuestData(uuid);
                }
                
                // Check weekly reset (every 7 days at 12:00 AM Egypt time)
                if (shouldResetWeekly(data.getLastWeeklyReset(), now)) {
                    data.resetWeekly();
                    data.setLastWeeklyReset(now.toInstant().toEpochMilli());
                    saveQuestData(uuid);
                }
            }
        }, 1200L, 1200L); // Check every minute
    }
    
    private boolean shouldResetDaily(long lastReset, ZonedDateTime now) {
        if (lastReset == 0) return true;
        
        ZonedDateTime lastResetTime = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(lastReset), egyptZone);
        
        // Check if we passed 12:00 AM or 12:00 PM
        if (now.toLocalDate().isAfter(lastResetTime.toLocalDate())) {
            return now.getHour() >= 0;
        }
        
        if (now.toLocalDate().equals(lastResetTime.toLocalDate())) {
            int lastHour = lastResetTime.getHour();
            int currentHour = now.getHour();
            
            // If last reset was before noon and now is after noon
            if (lastHour < 12 && currentHour >= 12) return true;
            // If last reset was before midnight and now is after midnight (next day)
            if (lastHour >= 12 && currentHour < 12) return true;
        }
        
        return false;
    }
    
    private boolean shouldResetWeekly(long lastReset, ZonedDateTime now) {
        if (lastReset == 0) return true;
        
        ZonedDateTime lastResetTime = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(lastReset), egyptZone);
        long daysSinceReset = java.time.Duration.between(lastResetTime, now).toDays();
        
        return daysSinceReset >= 7 && now.getHour() >= 0;
    }
    
    public QuestData getQuestData(UUID uuid) {
        if (!questDataMap.containsKey(uuid)) {
            questDataMap.put(uuid, loadQuestData(uuid));
        }
        return questDataMap.get(uuid);
    }
    
    private QuestData loadQuestData(UUID uuid) {
        String path = uuid.toString();
        QuestData data = new QuestData();
        
        if (plugin.getQuestsConfig().contains(path)) {
            data.setLastDailyReset(plugin.getQuestsConfig().getLong(path + ".lastDailyReset", 0));
            data.setLastWeeklyReset(plugin.getQuestsConfig().getLong(path + ".lastWeeklyReset", 0));
            data.setDailyProgress(plugin.getQuestsConfig().getInt(path + ".dailyProgress", 0));
            data.setDailyProgress2(plugin.getQuestsConfig().getInt(path + ".dailyProgress2", 0));
            data.setDailyProgress3(plugin.getQuestsConfig().getInt(path + ".dailyProgress3", 0));
            data.setDailyCompleted(plugin.getQuestsConfig().getBoolean(path + ".dailyCompleted", false));
            data.setWeeklyProgress1(plugin.getQuestsConfig().getInt(path + ".weeklyProgress1", 0));
            data.setWeeklyProgress2(plugin.getQuestsConfig().getInt(path + ".weeklyProgress2", 0));
            data.setWeeklyProgress3(plugin.getQuestsConfig().getInt(path + ".weeklyProgress3", 0));
            data.setWeeklyCompleted1(plugin.getQuestsConfig().getBoolean(path + ".weeklyCompleted1", false));
            data.setWeeklyCompleted2(plugin.getQuestsConfig().getBoolean(path + ".weeklyCompleted2", false));
            data.setWeeklyCompleted3(plugin.getQuestsConfig().getBoolean(path + ".weeklyCompleted3", false));
            data.setSpecialProgress(plugin.getQuestsConfig().getInt(path + ".specialProgress", 0));
            data.setSpecialCompleted(plugin.getQuestsConfig().getBoolean(path + ".specialCompleted", false));
            data.setPathQuestProgress(plugin.getQuestsConfig().getInt(path + ".pathQuestProgress", 0));
            data.setPathQuestCompleted(plugin.getQuestsConfig().getBoolean(path + ".pathQuestCompleted", false));
        }
        
        return data;
    }
    
    public void saveQuestData(UUID uuid) {
        QuestData data = questDataMap.get(uuid);
        if (data == null) return;
        
        String path = uuid.toString();
        plugin.getQuestsConfig().set(path + ".lastDailyReset", data.getLastDailyReset());
        plugin.getQuestsConfig().set(path + ".lastWeeklyReset", data.getLastWeeklyReset());
        plugin.getQuestsConfig().set(path + ".dailyProgress", data.getDailyProgress());
        plugin.getQuestsConfig().set(path + ".dailyProgress2", data.getDailyProgress2());
        plugin.getQuestsConfig().set(path + ".dailyProgress3", data.getDailyProgress3());
        plugin.getQuestsConfig().set(path + ".dailyCompleted", data.isDailyCompleted());
        plugin.getQuestsConfig().set(path + ".weeklyProgress1", data.getWeeklyProgress1());
        plugin.getQuestsConfig().set(path + ".weeklyProgress2", data.getWeeklyProgress2());
        plugin.getQuestsConfig().set(path + ".weeklyProgress3", data.getWeeklyProgress3());
        plugin.getQuestsConfig().set(path + ".weeklyCompleted1", data.isWeeklyCompleted1());
        plugin.getQuestsConfig().set(path + ".weeklyCompleted2", data.isWeeklyCompleted2());
        plugin.getQuestsConfig().set(path + ".weeklyCompleted3", data.isWeeklyCompleted3());
        plugin.getQuestsConfig().set(path + ".specialProgress", data.getSpecialProgress());
        plugin.getQuestsConfig().set(path + ".specialCompleted", data.isSpecialCompleted());
        plugin.getQuestsConfig().set(path + ".pathQuestProgress", data.getPathQuestProgress());
        plugin.getQuestsConfig().set(path + ".pathQuestCompleted", data.isPathQuestCompleted());
        
        plugin.saveQuestsConfig();
    }

    public void saveAllQuestData() {
        for (UUID uuid : new ArrayList<>(questDataMap.keySet())) {
            saveQuestData(uuid);
        }
    }
    
    public void trackMobKill(Player player, LivingEntity entity) {
        QuestData data = getQuestData(player.getUniqueId());
        PlayerData pData = plugin.getPlayerData(player.getUniqueId());
        int level = pData.getLevel();
        boolean questDataChanged = false;
        
        // Track daily quest
        if (!data.isDailyCompleted()) {
            if (level >= 150 && entity instanceof org.bukkit.entity.Warden) {
                data.setDailyProgress(data.getDailyProgress() + 1);
                questDataChanged = true;
                if (data.getDailyProgress() >= 20) {
                    data.setDailyCompleted(true);
                    questDataChanged = true;
                    rewardDaily(player, level);
                }
            } else if (level >= 100 && entity instanceof org.bukkit.entity.Warden) {
                data.setDailyProgress(data.getDailyProgress() + 1);
                questDataChanged = true;
                if (data.getDailyProgress() >= 15) {
                    data.setDailyCompleted(true);
                    questDataChanged = true;
                    rewardDaily(player, level);
                }
            } else if (level >= 80 && entity instanceof org.bukkit.entity.Warden) {
                data.setDailyProgress(data.getDailyProgress() + 1);
                questDataChanged = true;
                if (data.getDailyProgress() >= 10) {
                    data.setDailyCompleted(true);
                    questDataChanged = true;
                    rewardDaily(player, level);
                }
            } else if (level >= 50 && entity instanceof org.bukkit.entity.Warden) {
                data.setDailyProgress(data.getDailyProgress() + 1);
                questDataChanged = true;
                if (data.getDailyProgress() >= 5) {
                    data.setDailyCompleted(true);
                    questDataChanged = true;
                    rewardDaily(player, level);
                }
            } else if (level >= 30 && entity instanceof org.bukkit.entity.Warden) {
                data.setDailyProgress(data.getDailyProgress() + 1);
                questDataChanged = true;
                if (data.getDailyProgress() >= 3) {
                    data.setDailyCompleted(true);
                    questDataChanged = true;
                    rewardDaily(player, level);
                }
            } else if (level >= 15 && entity instanceof org.bukkit.entity.Warden) {
                data.setDailyProgress(data.getDailyProgress() + 1);
                questDataChanged = true;
                if (data.getDailyProgress() >= 2) {
                    data.setDailyCompleted(true);
                    questDataChanged = true;
                    rewardDaily(player, level);
                }
            } else if (level < 15) {
                if (entity instanceof org.bukkit.entity.Zombie) {
                    data.setDailyProgress(data.getDailyProgress() + 1);
                    questDataChanged = true;
                } else if (entity instanceof org.bukkit.entity.Spider) {
                    data.setDailyProgress2(data.getDailyProgress2() + 1);
                    questDataChanged = true;
                } else if (entity instanceof org.bukkit.entity.Skeleton) {
                    data.setDailyProgress3(data.getDailyProgress3() + 1);
                    questDataChanged = true;
                }
                if (data.getDailyProgress() >= 20 && data.getDailyProgress2() >= 15 && data.getDailyProgress3() >= 5) {
                    data.setDailyCompleted(true);
                    questDataChanged = true;
                    rewardDaily(player, level);
                }
            }
        }
        
        // Track weekly quest 1
        if (!data.isWeeklyCompleted1()) {
            boolean quest1Kill = (level < 15 && entity instanceof org.bukkit.entity.Zombie)
                || (level >= 15 && entity instanceof org.bukkit.entity.Warden);
            if (quest1Kill) {
                data.setWeeklyProgress1(data.getWeeklyProgress1() + 1);
                questDataChanged = true;
                int target = getWeeklyQuest1Target(level);
                if (data.getWeeklyProgress1() >= target) {
                    data.setWeeklyCompleted1(true);
                    questDataChanged = true;
                    rewardWeekly(player, level, 1);
                }
            }
        }

        // Track weekly quest 2
        if (!data.isWeeklyCompleted2()) {
            boolean quest2Kill = (level < 15 && entity instanceof org.bukkit.entity.Creeper)
                || (level >= 15 && entity instanceof org.bukkit.entity.Wither);
            if (quest2Kill) {
                data.setWeeklyProgress2(data.getWeeklyProgress2() + 1);
                questDataChanged = true;
                int target = getWeeklyQuest2Target(level);
                if (data.getWeeklyProgress2() >= target) {
                    data.setWeeklyCompleted2(true);
                    questDataChanged = true;
                    rewardWeekly(player, level, 2);
                }
            }
        }

        if (!data.isWeeklyCompleted3()) {
            int target = getWeeklyLevelTarget(level);
            int newProgress = Math.max(data.getWeeklyProgress3(), level);
            if (newProgress != data.getWeeklyProgress3()) {
                data.setWeeklyProgress3(newProgress);
                questDataChanged = true;
            }
            if (level >= target) {
                data.setWeeklyCompleted3(true);
                questDataChanged = true;
                rewardWeekly(player, level, 3);
            }
        }
        
        // Track special quest
        if (!data.isSpecialCompleted() && entity instanceof org.bukkit.entity.EnderDragon) {
            data.setSpecialProgress(data.getSpecialProgress() + 1);
            questDataChanged = true;
            if (data.getSpecialProgress() >= 3) {
                data.setSpecialCompleted(true);
                questDataChanged = true;
                rewardSpecial(player);
            }
        }

        if (questDataChanged) {
            saveQuestData(player.getUniqueId());
        }
    }
    
    private void rewardDaily(Player player, int level) {
        double reward;
        if (level >= 150) {
            reward = 50000;
            player.sendMessage("§a§lDaily Quest Complete! §e+50,000 XP");
        } else if (level >= 100) {
            reward = 35000;
            player.sendMessage("§a§lDaily Quest Complete! §e+35,000 XP");
        } else if (level >= 80) {
            int levelsNeeded = 5;
            double xpForLevels = 0;
            for (int i = 0; i < levelsNeeded; i++) {
                xpForLevels += 200 + ((level + i) * 30);
            }
            reward = Math.max(15000, xpForLevels);
            player.sendMessage("§a§lDaily Quest Complete! §e+" + (int)reward + " XP");
        } else if (level >= 50) {
            reward = 25000;
            player.sendMessage("§a§lDaily Quest Complete! §e+25,000 XP");
        } else if (level >= 30) {
            reward = 20000;
            player.sendMessage("§a§lDaily Quest Complete! §e+20,000 XP");
        } else if (level >= 15) {
            reward = 15000;
            player.sendMessage("§a§lDaily Quest Complete! §e+15,000 XP");
        } else {
            reward = 20000;
            player.sendMessage("§a§lDaily Quest Complete! §e+20,000 XP");
        }

        plugin.getEventListener().grantXp(player, reward, false);
    }

    private void rewardWeekly(Player player, int level, int questIndex) {
        double reward;

        if (questIndex == 1) {
            if (level >= 150) reward = 100000;
            else if (level >= 100) reward = 75000;
            else if (level >= 80) reward = 50000;
            else if (level >= 50) reward = 40000;
            else if (level >= 30) reward = 30000;
            else if (level >= 15) reward = 25000;
            else reward = 15000;
        } else if (questIndex == 2) {
            if (level >= 150) reward = 120000;
            else if (level >= 100) reward = 80000;
            else if (level >= 80) reward = 60000;
            else if (level >= 50) reward = 45000;
            else if (level >= 30) reward = 35000;
            else if (level >= 15) reward = 20000;
            else reward = 15000;
        } else {
            if (level >= 150) reward = 200000;
            else if (level >= 100) reward = 150000;
            else if (level >= 80) reward = 100000;
            else if (level >= 50) reward = 50000;
            else if (level >= 30) reward = 40000;
            else if (level >= 15) reward = 30000;
            else reward = 20000;
        }

        plugin.getEventListener().grantXp(player, reward, false);
        player.sendMessage("§b§lWeekly Quest Complete! §e+" + (int) reward + " XP");
    }
    
    private void rewardSpecial(Player player) {
        plugin.getEventListener().grantXp(player, 200000, false);
        player.sendMessage("§d§lSpecial Quest Complete! §e+200,000 XP");
    }

    private int getWeeklyQuest1Target(int level) {
        if (level >= 150) return 50;
        if (level >= 100) return 35;
        if (level >= 80) return 25;
        if (level >= 50) return 15;
        if (level >= 30) return 10;
        if (level >= 15) return 5;
        return 100;
    }

    private int getWeeklyQuest2Target(int level) {
        if (level >= 150) return 10;
        if (level >= 100) return 7;
        if (level >= 80) return 5;
        if (level >= 50) return 3;
        if (level >= 30) return 2;
        if (level >= 15) return 1;
        return 50;
    }

    private int getWeeklyLevelTarget(int level) {
        if (level >= 150) return 200;
        if (level >= 100) return 150;
        if (level >= 80) return 100;
        if (level >= 50) return 80;
        if (level >= 30) return 50;
        if (level >= 15) return 30;
        return 15;
    }
    
    public AttackMastery getPlugin() {
        return plugin;
    }
    
    public void openQuestMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§6§lAttack Mastery Quests");
        
        // Daily Quests Book
        ItemStack daily = new ItemStack(Material.BOOK);
        ItemMeta dailyMeta = daily.getItemMeta();
        dailyMeta.setDisplayName("§a§lDaily Quests");
        List<String> dailyLore = new ArrayList<>();
        dailyLore.add("§7Complete daily challenges");
        dailyLore.add("§7for XP rewards!");
        dailyLore.add("");
        dailyLore.add("§eClick to view");
        dailyMeta.setLore(dailyLore);
        daily.setItemMeta(dailyMeta);
        
        // Weekly Quests Book
        ItemStack weekly = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta weeklyMeta = weekly.getItemMeta();
        weeklyMeta.setDisplayName("§b§lWeekly Quests");
        List<String> weeklyLore = new ArrayList<>();
        weeklyLore.add("§7Harder challenges with");
        weeklyLore.add("§7bigger rewards!");
        weeklyLore.add("");
        weeklyLore.add("§eClick to view");
        weeklyMeta.setLore(weeklyLore);
        weekly.setItemMeta(weeklyMeta);
        
        // Special Quests Book
        ItemStack special = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta specialMeta = special.getItemMeta();
        specialMeta.setDisplayName("§d§lSpecial Quests");
        List<String> specialLore = new ArrayList<>();
        specialLore.add("§7Exclusive limited-time");
        specialLore.add("§7quests with unique rewards!");
        specialLore.add("");
        specialLore.add("§eClick to view");
        specialMeta.setLore(specialLore);
        special.setItemMeta(specialMeta);

        ItemStack pathContracts = new ItemStack(Material.NETHER_STAR);
        ItemMeta pathMeta = pathContracts.getItemMeta();
        pathMeta.setDisplayName("§6§lPath Contracts");
        List<String> pathLore = new ArrayList<>();
        pathLore.add("§7Path-specific challenges");
        pathLore.add("§7for mastery progression");
        pathLore.add("");
        pathLore.add("§eClick to view");
        pathMeta.setLore(pathLore);
        pathContracts.setItemMeta(pathMeta);
        
        inv.setItem(11, daily);
        inv.setItem(13, weekly);
        inv.setItem(15, special);
        inv.setItem(22, pathContracts);
        
        player.openInventory(inv);
    }
    
    public void openDailyQuests(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§a§lDaily Quests");
        
        PlayerData data = plugin.getPlayerData(player.getUniqueId());
        QuestData qData = getQuestData(player.getUniqueId());
        int level = data.getLevel();
        
        // Level-based daily quests
        if (level >= 150) {
            inv.setItem(13, createQuestItem("§aKill 20 Wardens", "§7" + qData.getDailyProgress() + "/20 Wardens killed", "§e+50k XP", qData.isDailyCompleted()));
        } else if (level >= 100) {
            inv.setItem(13, createQuestItem("§aKill 15 Wardens", "§7" + qData.getDailyProgress() + "/15 Wardens killed", "§e+35k XP", qData.isDailyCompleted()));
        } else if (level >= 80) {
            inv.setItem(13, createQuestItem("§aKill 10 Wardens", "§7" + qData.getDailyProgress() + "/10 Wardens killed", "§e+15k XP or 5 Levels", qData.isDailyCompleted()));
        } else if (level >= 50) {
            inv.setItem(13, createQuestItem("§aKill 5 Wardens", "§7" + qData.getDailyProgress() + "/5 Wardens killed", "§e+25k XP", qData.isDailyCompleted()));
        } else if (level >= 30) {
            inv.setItem(13, createQuestItem("§aKill 3 Wardens", "§7" + qData.getDailyProgress() + "/3 Wardens killed", "§e+20k XP", qData.isDailyCompleted()));
        } else if (level >= 15) {
            inv.setItem(13, createQuestItem("§aKill 2 Wardens", "§7" + qData.getDailyProgress() + "/2 Wardens killed", "§e+15k XP", qData.isDailyCompleted()));
        } else {
            inv.setItem(11, createQuestItem("§aKill 20 Zombies", "§7" + qData.getDailyProgress() + "/20 Zombies killed", "§e+20k XP", qData.isDailyCompleted()));
            inv.setItem(13, createQuestItem("§aKill 15 Spiders", "§7" + qData.getDailyProgress2() + "/15 Spiders killed", "§e+20k XP", qData.isDailyCompleted()));
            inv.setItem(15, createQuestItem("§aKill 5 Skeletons", "§7" + qData.getDailyProgress3() + "/5 Skeletons killed", "§e+20k XP", qData.isDailyCompleted()));
        }
        
        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName("§cBack");
        back.setItemMeta(backMeta);
        inv.setItem(49, back);
        
        player.openInventory(inv);
    }
    
    public void openWeeklyQuests(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§b§lWeekly Quests");
        
        PlayerData data = plugin.getPlayerData(player.getUniqueId());
        QuestData qData = getQuestData(player.getUniqueId());
        int level = data.getLevel();

        if (!qData.isWeeklyCompleted3() && level >= getWeeklyLevelTarget(level)) {
            qData.setWeeklyCompleted3(true);
            qData.setWeeklyProgress3(level);
            rewardWeekly(player, level, 3);
            saveQuestData(player.getUniqueId());
        }
        
        // Level-based weekly quests
        if (level >= 150) {
            inv.setItem(11, createQuestItem("§bKill 50 Wardens", "§7" + qData.getWeeklyProgress1() + "/50 Wardens killed", "§e+100k XP", qData.isWeeklyCompleted1()));
            inv.setItem(13, createQuestItem("§bKill 10 Withers", "§7" + qData.getWeeklyProgress2() + "/10 Withers killed", "§e+120k XP", qData.isWeeklyCompleted2()));
            inv.setItem(15, createQuestItem("§bReach Level 200", "§7Current: Level " + level, "§e+200k XP", qData.isWeeklyCompleted3()));
        } else if (level >= 100) {
            inv.setItem(11, createQuestItem("§bKill 35 Wardens", "§7" + qData.getWeeklyProgress1() + "/35 Wardens killed", "§e+75k XP", qData.isWeeklyCompleted1()));
            inv.setItem(13, createQuestItem("§bKill 7 Withers", "§7" + qData.getWeeklyProgress2() + "/7 Withers killed", "§e+80k XP", qData.isWeeklyCompleted2()));
            inv.setItem(15, createQuestItem("§bReach Level 150", "§7Current: Level " + level, "§e+150k XP", qData.isWeeklyCompleted3()));
        } else if (level >= 80) {
            inv.setItem(11, createQuestItem("§bKill 25 Wardens", "§7" + qData.getWeeklyProgress1() + "/25 Wardens killed", "§e+50k XP", qData.isWeeklyCompleted1()));
            inv.setItem(13, createQuestItem("§bKill 5 Withers", "§7" + qData.getWeeklyProgress2() + "/5 Withers killed", "§e+60k XP", qData.isWeeklyCompleted2()));
            inv.setItem(15, createQuestItem("§bReach Level 100", "§7Current: Level " + level, "§e+100k XP", qData.isWeeklyCompleted3()));
        } else if (level >= 50) {
            inv.setItem(11, createQuestItem("§bKill 15 Wardens", "§7" + qData.getWeeklyProgress1() + "/15 Wardens killed", "§e+40k XP", qData.isWeeklyCompleted1()));
            inv.setItem(13, createQuestItem("§bKill 3 Withers", "§7" + qData.getWeeklyProgress2() + "/3 Withers killed", "§e+45k XP", qData.isWeeklyCompleted2()));
            inv.setItem(15, createQuestItem("§bReach Level 80", "§7Current: Level " + level, "§e+50k XP", qData.isWeeklyCompleted3()));
        } else if (level >= 30) {
            inv.setItem(11, createQuestItem("§bKill 10 Wardens", "§7" + qData.getWeeklyProgress1() + "/10 Wardens killed", "§e+30k XP", qData.isWeeklyCompleted1()));
            inv.setItem(13, createQuestItem("§bKill 2 Withers", "§7" + qData.getWeeklyProgress2() + "/2 Withers killed", "§e+35k XP", qData.isWeeklyCompleted2()));
            inv.setItem(15, createQuestItem("§bReach Level 50", "§7Current: Level " + level, "§e+40k XP", qData.isWeeklyCompleted3()));
        } else if (level >= 15) {
            inv.setItem(11, createQuestItem("§bKill 5 Wardens", "§7" + qData.getWeeklyProgress1() + "/5 Wardens killed", "§e+25k XP", qData.isWeeklyCompleted1()));
            inv.setItem(13, createQuestItem("§bKill 1 Wither", "§7" + qData.getWeeklyProgress2() + "/1 Wither killed", "§e+20k XP", qData.isWeeklyCompleted2()));
            inv.setItem(15, createQuestItem("§bReach Level 30", "§7Current: Level " + level, "§e+30k XP", qData.isWeeklyCompleted3()));
        } else {
            inv.setItem(11, createQuestItem("§bKill 100 Zombies", "§7" + qData.getWeeklyProgress1() + "/100 Zombies killed", "§e+15k XP", qData.isWeeklyCompleted1()));
            inv.setItem(13, createQuestItem("§bKill 50 Creepers", "§7" + qData.getWeeklyProgress2() + "/50 Creepers killed", "§e+15k XP", qData.isWeeklyCompleted2()));
            inv.setItem(15, createQuestItem("§bReach Level 15", "§7Current: Level " + level, "§e+20k XP", qData.isWeeklyCompleted3()));
        }
        
        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName("§cBack");
        back.setItemMeta(backMeta);
        inv.setItem(49, back);
        
        player.openInventory(inv);
    }
    
    public void openSpecialQuests(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§d§lSpecial Quests");
        
        QuestData qData = getQuestData(player.getUniqueId());
        
        // Special quest
        inv.setItem(13, createQuestItem("§dKill 3 Ender Dragons", "§7" + qData.getSpecialProgress() + "/3 Dragons killed", "§e+200k XP", qData.isSpecialCompleted()));
        
        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName("§cBack");
        back.setItemMeta(backMeta);
        inv.setItem(49, back);
        
        player.openInventory(inv);
    }

    public void openPathQuests(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§6§lPath Contracts");
        PlayerData playerData = plugin.getPlayerData(player.getUniqueId());
        QuestData qData = getQuestData(player.getUniqueId());

        if (playerData.getMasteryPath() == MasteryPath.NONE) {
            inv.setItem(13, createQuestItem("§7No Path Selected", "§7Choose one with /attack path choose", "§eNo rewards", false));
        } else {
            int target = getPathQuestTarget(playerData.getMasteryPath());
            inv.setItem(13, createQuestItem(
                "§6" + getPathQuestName(playerData.getMasteryPath()),
                "§7" + qData.getPathQuestProgress() + "/" + target,
                "§e+15k XP & +250 Mastery XP",
                qData.isPathQuestCompleted()
            ));
        }

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName("§cBack");
        back.setItemMeta(backMeta);
        inv.setItem(22, back);

        player.openInventory(inv);
    }

    public void trackPathQuestKill(Player player, LivingEntity entity) {
        PlayerData playerData = plugin.getPlayerData(player.getUniqueId());
        if (playerData.getMasteryPath() == MasteryPath.NONE) {
            return;
        }

        QuestData qData = getQuestData(player.getUniqueId());
        if (qData.isPathQuestCompleted()) {
            return;
        }

        if (!matchesPathQuestTarget(playerData.getMasteryPath(), entity)) {
            return;
        }

        qData.setPathQuestProgress(qData.getPathQuestProgress() + 1);
        int target = getPathQuestTarget(playerData.getMasteryPath());
        if (qData.getPathQuestProgress() >= target) {
            qData.setPathQuestCompleted(true);
            plugin.getEventListener().grantXp(player, 15000, false);
            plugin.getEventListener().grantMasteryXp(player, 250, true);
            player.sendMessage("§6§lPath Contract Complete! §e+15,000 XP §7and §b+250 Mastery XP");
        }

        saveQuestData(player.getUniqueId());
    }

    public void initializeMasteryObjective(PlayerData data) {
        if (data.getMasteryPath() == MasteryPath.NONE) {
            data.setMasteryObjective("");
            data.setMasteryObjectiveProgress(0);
            data.setMasteryObjectiveTarget(0);
            return;
        }

        int target = 12 + (data.getMasteryLevel() * 2);
        data.setMasteryObjective(getPathObjectiveName(data.getMasteryPath()));
        data.setMasteryObjectiveTarget(target);
        data.setMasteryObjectiveProgress(Math.min(data.getMasteryObjectiveProgress(), target));
    }

    private String getPathObjectiveName(MasteryPath path) {
        return switch (path) {
            case SWORD -> "Sword Combo Trials";
            case AXE -> "Axe Bleed Hunts";
            case BOW -> "Archer Precision Contracts";
            case CRIT -> "Critical Finisher Runs";
            case NONE -> "";
        };
    }

    private String getPathQuestName(MasteryPath path) {
        return switch (path) {
            case SWORD -> "Sword Vanguard Contract";
            case AXE -> "Axe Trial Contract";
            case BOW -> "Archer Contract";
            case CRIT -> "Crit Execution Contract";
            case NONE -> "No Contract";
        };
    }

    private int getPathQuestTarget(MasteryPath path) {
        return switch (path) {
            case SWORD -> 40;
            case AXE -> 25;
            case BOW -> 30;
            case CRIT -> 20;
            case NONE -> 0;
        };
    }

    private boolean matchesPathQuestTarget(MasteryPath path, LivingEntity entity) {
        return switch (path) {
            case SWORD -> entity instanceof org.bukkit.entity.Zombie || entity instanceof org.bukkit.entity.Skeleton;
            case AXE -> entity instanceof org.bukkit.entity.Pillager || entity instanceof org.bukkit.entity.Vindicator || entity instanceof org.bukkit.entity.Evoker || entity instanceof org.bukkit.entity.Ravager;
            case BOW -> entity instanceof org.bukkit.entity.Skeleton || entity instanceof org.bukkit.entity.Phantom || entity instanceof org.bukkit.entity.Ghast;
            case CRIT -> entity instanceof org.bukkit.entity.Enderman || entity instanceof org.bukkit.entity.Blaze || entity instanceof org.bukkit.entity.Witch || entity instanceof org.bukkit.entity.Wither || entity instanceof org.bukkit.entity.Warden;
            case NONE -> false;
        };
    }
    
    private ItemStack createQuestItem(String name, String progress, String reward, boolean completed) {
        ItemStack item = new ItemStack(completed ? Material.LIME_DYE : Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(progress);
        lore.add("");
        lore.add("§7Reward: " + reward);
        lore.add("");
        if (completed) {
            lore.add("§a§l✔ COMPLETED");
        } else {
            lore.add("§7In Progress...");
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
