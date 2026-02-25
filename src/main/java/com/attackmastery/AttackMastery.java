package com.attackmastery;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AttackMastery extends JavaPlugin {
    private final Map<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();
    private final Set<UUID> dirtyPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<UUID, Map<UUID, Double>> mobDamageTracking = new ConcurrentHashMap<>();
    private EventListener eventListener;
    private QuestManager questManager;
    private File playersFile;
    private File questsFile;
    private YamlConfiguration playersConfig;
    private YamlConfiguration questsConfig;
    private int saveTaskId = -1;

    public static class LeaderboardEntry {
        private final String name;
        private final int level;
        private final int masteryLevel;

        public LeaderboardEntry(String name, int level, int masteryLevel) {
            this.name = name;
            this.level = level;
            this.masteryLevel = masteryLevel;
        }

        public String getName() {
            return name;
        }

        public int getLevel() {
            return level;
        }

        public int getMasteryLevel() {
            return masteryLevel;
        }
    }
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        getLogger().info("[DEBUG] Config max-level: " + getConfig().getInt("max-level", 200));

        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().severe("Failed to create plugin data folder!");
        }
        
        playersFile = new File(getDataFolder(), "players.yml");
        if (!playersFile.exists()) {
            try {
                playersFile.createNewFile();
            } catch (Exception e) {
                getLogger().severe("Failed to create players.yml!");
            }
        }
        playersConfig = YamlConfiguration.loadConfiguration(playersFile);
        
        questsFile = new File(getDataFolder(), "quests.yml");
        if (!questsFile.exists()) {
            try {
                questsFile.createNewFile();
            } catch (Exception e) {
                getLogger().severe("Failed to create quests.yml!");
            }
        }
        questsConfig = YamlConfiguration.loadConfiguration(questsFile);
        
        if (!LMUtils.init()) {
            getLogger().info("LevelledMobs not found - using mob health as level");
        } else {
            getLogger().info("LevelledMobs detected but not integrated - using mob health as level");
        }
        
        questManager = new QuestManager(this);
        eventListener = new EventListener(this);
        
        getServer().getPluginManager().registerEvents(eventListener, this);
        getServer().getPluginManager().registerEvents(new QuestListener(this), this);
        if (getCommand("attack") != null) {
            getCommand("attack").setExecutor(new CommandHandler(this));
        } else {
            getLogger().severe("Command 'attack' is missing from plugin.yml");
        }
        
        saveTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this::saveAllDirty, 100L, 100L);
        
        getLogger().info("AttackMastery enabled!");
    }
    
    @Override
    public void onDisable() {
        if (saveTaskId != -1) {
            Bukkit.getScheduler().cancelTask(saveTaskId);
        }

        if (questManager != null) {
            questManager.saveAllQuestData();
        }
        
        for (UUID uuid : playerDataMap.keySet()) {
            savePlayerData(uuid);
        }
        
        getLogger().info("AttackMastery disabled!");
    }
    
    public PlayerData getPlayerData(UUID uuid) {
        return playerDataMap.computeIfAbsent(uuid, this::loadPlayerData);
    }
    
    private PlayerData loadPlayerData(UUID uuid) {
        String path = uuid.toString();
        int level = playersConfig.getInt(path + ".level", 0);
        double xp = playersConfig.getDouble(path + ".xp", 0.0);
        
        PlayerData data = new PlayerData(level, xp);
        data.setMasteryPath(MasteryPath.fromString(playersConfig.getString(path + ".mastery.path", "NONE")));
        data.setMasteryLevel(playersConfig.getInt(path + ".mastery.level", 0));
        data.setMasteryXp(playersConfig.getDouble(path + ".mastery.xp", 0.0));
        data.setMasteryObjective(playersConfig.getString(path + ".mastery.objective", ""));
        data.setMasteryObjectiveProgress(playersConfig.getInt(path + ".mastery.objectiveProgress", 0));
        data.setMasteryObjectiveTarget(playersConfig.getInt(path + ".mastery.objectiveTarget", 0));
        data.setMasteryRespecs(playersConfig.getInt(path + ".mastery.respecs", 0));
        data.setCosmeticTitle(playersConfig.getString(path + ".mastery.title", ""));

        if (data.getMasteryPath() != MasteryPath.NONE && data.getMasteryObjectiveTarget() <= 0) {
            questManager.initializeMasteryObjective(data);
        }

        int base = getConfig().getInt("xp-base", 200);
        int increment = getConfig().getInt("xp-increment", 30);
        data.setXpNeeded(base + (level * increment));
        
        return data;
    }
    
    public void savePlayerData(UUID uuid) {
        PlayerData data = playerDataMap.get(uuid);
        if (data == null) return;

        String path = uuid.toString();
        playersConfig.set(path + ".level", data.getLevel());
        playersConfig.set(path + ".xp", data.getXp());
        playersConfig.set(path + ".mastery.path", data.getMasteryPath().name());
        playersConfig.set(path + ".mastery.level", data.getMasteryLevel());
        playersConfig.set(path + ".mastery.xp", data.getMasteryXp());
        playersConfig.set(path + ".mastery.objective", data.getMasteryObjective());
        playersConfig.set(path + ".mastery.objectiveProgress", data.getMasteryObjectiveProgress());
        playersConfig.set(path + ".mastery.objectiveTarget", data.getMasteryObjectiveTarget());
        playersConfig.set(path + ".mastery.respecs", data.getMasteryRespecs());
        playersConfig.set(path + ".mastery.title", data.getCosmeticTitle());
        
        try {
            playersConfig.save(playersFile);
        } catch (Exception e) {
            getLogger().severe("Failed to save player data for " + uuid);
        }
        
        dirtyPlayers.remove(uuid);
    }
    
    public void markDirty(UUID uuid) {
        dirtyPlayers.add(uuid);
    }

    public void unloadPlayerData(UUID uuid) {
        playerDataMap.remove(uuid);
        dirtyPlayers.remove(uuid);
    }
    
    private void saveAllDirty() {
        for (UUID uuid : new HashSet<>(dirtyPlayers)) {
            savePlayerData(uuid);
        }
    }
    
    public void trackDamage(UUID mobUuid, UUID playerUuid, double damage) {
        mobDamageTracking.computeIfAbsent(mobUuid, k -> new ConcurrentHashMap<>())
            .merge(playerUuid, damage, Double::sum);
    }
    
    public Map<UUID, Double> getDamageTracking(UUID mobUuid) {
        return mobDamageTracking.remove(mobUuid);
    }
    
    public QuestManager getQuestManager() {
        return questManager;
    }

    public EventListener getEventListener() {
        return eventListener;
    }
    
    public YamlConfiguration getQuestsConfig() {
        return questsConfig;
    }
    
    public void saveQuestsConfig() {
        try {
            questsConfig.save(questsFile);
        } catch (Exception e) {
            getLogger().severe("Failed to save quests.yml!");
        }
    }

    public List<LeaderboardEntry> getTopPlayers(int limit) {
        Map<UUID, LeaderboardEntry> entries = new ConcurrentHashMap<>();

        for (String rawKey : playersConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(rawKey);
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                String name = offlinePlayer.getName() != null ? offlinePlayer.getName() : rawKey.substring(0, 8);
                int level = playersConfig.getInt(rawKey + ".level", 0);
                int masteryLevel = playersConfig.getInt(rawKey + ".mastery.level", 0);
                entries.put(uuid, new LeaderboardEntry(name, level, masteryLevel));
            } catch (IllegalArgumentException ignored) {
            }
        }

        for (Map.Entry<UUID, PlayerData> entry : playerDataMap.entrySet()) {
            UUID uuid = entry.getKey();
            PlayerData data = entry.getValue();
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            String name = offlinePlayer.getName() != null ? offlinePlayer.getName() : uuid.toString().substring(0, 8);
            entries.put(uuid, new LeaderboardEntry(name, data.getLevel(), data.getMasteryLevel()));
        }

        List<LeaderboardEntry> sorted = new ArrayList<>(entries.values());
        sorted.sort(
            Comparator.comparingInt(LeaderboardEntry::getLevel)
                .thenComparingInt(LeaderboardEntry::getMasteryLevel)
                .reversed()
        );

        if (sorted.size() > limit) {
            return sorted.subList(0, limit);
        }
        return sorted;
    }
}


