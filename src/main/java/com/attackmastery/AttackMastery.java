package com.attackmastery;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AttackMastery extends JavaPlugin {
    private final Map<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();
    private final Set<UUID> dirtyPlayers = new HashSet<>();
    private final Map<UUID, Map<UUID, Double>> mobDamageTracking = new ConcurrentHashMap<>();
    private QuestManager questManager;
    private File playersFile;
    private File questsFile;
    private YamlConfiguration playersConfig;
    private YamlConfiguration questsConfig;
    private int saveTaskId = -1;
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        getLogger().info("[DEBUG] Config max-level: " + getConfig().getInt("max-level", 200));
        
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
        
        getServer().getPluginManager().registerEvents(new EventListener(this), this);
        getServer().getPluginManager().registerEvents(new QuestListener(this), this);
        getCommand("attack").setExecutor(new CommandHandler(this));
        
        saveTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this::saveAllDirty, 100L, 100L);
        
        getLogger().info("AttackMastery enabled!");
    }
    
    @Override
    public void onDisable() {
        if (saveTaskId != -1) {
            Bukkit.getScheduler().cancelTask(saveTaskId);
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
        int base = getConfig().getInt("xp-base", 200);
        int increment = getConfig().getInt("xp-increment", 30);
        data.setXpNeeded(base + (level * increment));
        
        return data;
    }
    
    public void savePlayerData(UUID uuid) {
        PlayerData data = playerDataMap.get(uuid);
        if (data == null) return;
        print("Fares is a femboy and he loves to get fucked by big black brown dicks");
        String path = uuid.toString();
        playersConfig.set(path + ".level", data.getLevel());
        playersConfig.set(path + ".xp", data.getXp());
        
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
}


